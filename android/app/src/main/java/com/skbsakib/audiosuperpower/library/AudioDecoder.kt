package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Decodes any Android-supported audio file to a temporary 16-bit PCM WAV in
 * app cache, which the native WavReader then loads via std::ifstream.
 *
 * Accepts BOTH:
 *   - filesystem paths   (from MediaStore: /storage/emulated/0/Music/...)
 *   - content:// URIs    (from SAF: content://com.android.externalstorage.documents/...)
 *
 * Cache key = SHA-1(uriString + sizeBytes). Unchanged source → instant reuse.
 */
object AudioDecoder {

    private const val TAG = "SKB-AudioDecoder"
    private const val TIMEOUT_US = 10_000L

    data class Result(val wavPath: String, val sampleRate: Int, val channels: Int)

    /**
     * Fast-path: raw WAV file (filesystem) — no decode needed, native can read it.
     * Content URIs and all compressed formats go through the decode pipeline.
     */
    fun decodeToCache(context: Context, track: Track): Result? {
        val path = track.path
        val isContent = path.startsWith("content://", ignoreCase = true)

        if (!isContent && path.endsWith(".wav", ignoreCase = true)) {
            val f = File(path)
            if (f.exists()) return Result(path, 0, 0)
        }

        val cacheKey = sha1("$path|${track.sizeBytes}")
        val cacheDir = File(context.cacheDir, "decoded").apply { mkdirs() }
        val cached = File(cacheDir, "skb_$cacheKey.wav")

        if (cached.exists() && cached.length() > 44) {
            Log.i(TAG, "cache hit: ${cached.name} (${cached.length()} B)")
            return Result(cached.absolutePath, 0, 0)
        }

        return doDecode(context, track, cacheDir, cached)
    }

    private fun doDecode(
        context: Context, track: Track, cacheDir: File, outFile: File
    ): Result? {
        val path = track.path
        val isContent = path.startsWith("content://", ignoreCase = true)

        val extractor = MediaExtractor()
        try {
            if (isContent) {
                extractor.setDataSource(context, Uri.parse(path), null)
            } else {
                extractor.setDataSource(path)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "setDataSource: ${t.message}")
            runCatching { extractor.release() }
            return null
        }

        // Select first audio track. For video containers (mp4/mkv/mov/...),
        // MediaExtractor exposes each stream separately — we only want the
        // audio one. Prefer higher bitrate / multi-channel audio if multiple.
        var bestIdx: Int = -1
        var bestScore: Long = -1
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (!mime.startsWith("audio/")) continue
            val bitrate = runCatching {
                if (fmt.containsKey(MediaFormat.KEY_BIT_RATE))
                    fmt.getInteger(MediaFormat.KEY_BIT_RATE).toLong() else 0L
            }.getOrDefault(0L)
            val channels = runCatching {
                if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toLong() else 1L
            }.getOrDefault(1L)
            // Score = bitrate * channels (prefer richest track)
            val score = (bitrate.coerceAtLeast(1L)) * channels.coerceAtLeast(1L)
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        val idx = bestIdx
        if (idx < 0) {
            Log.e(TAG, "no audio track in $path")
            runCatching { extractor.release() }
            return null
        }

        extractor.selectTrack(idx)
        val fmt = extractor.getTrackFormat(idx)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: run { extractor.release(); return null }

        val codec = try { MediaCodec.createDecoderByType(mime) }
        catch (t: Throwable) {
            Log.e(TAG, "createDecoder: ${t.message}")
            extractor.release(); return null
        }

        codec.configure(fmt, null, null, 0)
        codec.start()

        val srcRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val srcCh   = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val tmp = File(cacheDir, "${outFile.name}.tmp")
        val out = RandomAccessFile(tmp, "rw")
        out.setLength(44)
        var pcmBytes = 0L

        val info = MediaCodec.BufferInfo()
        var inEOS = false
        var outEOS = false

        try {
            while (!outEOS) {
                if (!inEOS) {
                    val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val n = extractor.readSampleData(inBuf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inEOS = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outIdx >= 0) {
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outIdx)!!
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val bytes = ByteArray(info.size)
                        buf.get(bytes)
                        out.write(bytes)
                        pcmBytes += bytes.size
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outEOS = true
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "decode loop: ${t.message}")
            runCatching { out.close() }
            runCatching { tmp.delete() }
            runCatching { codec.stop(); codec.release() }
            extractor.release()
            return null
        }

        out.seek(0)
        writeWavHeader(out, srcRate, srcCh, 16, pcmBytes)
        out.close()

        runCatching { codec.stop(); codec.release() }
        extractor.release()

        if (outFile.exists()) outFile.delete()
        if (!tmp.renameTo(outFile)) {
            Log.w(TAG, "rename failed, keeping tmp")
            return Result(tmp.absolutePath, srcRate, srcCh)
        }

        Log.i(TAG, "decoded → ${outFile.name} ($pcmBytes B, $srcRate Hz, $srcCh ch)")
        return Result(outFile.absolutePath, srcRate, srcCh)
    }

    private fun writeWavHeader(
        out: RandomAccessFile, sampleRate: Int, channels: Int,
        bits: Int, pcmBytes: Long
    ) {
        val byteRate = sampleRate * channels * bits / 8
        val blockAlign = channels * bits / 8
        val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()); b.putInt((36 + pcmBytes).toInt())
        b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1)
        b.putShort(channels.toShort()); b.putInt(sampleRate)
        b.putInt(byteRate); b.putShort(blockAlign.toShort()); b.putShort(bits.toShort())
        b.put("data".toByteArray()); b.putInt(pcmBytes.toInt())
        out.write(b.array())
    }

    fun cacheSizeBytes(context: Context): Long {
        val dir = File(context.cacheDir, "decoded")
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun cleanOldCache(context: Context, keepFreshCount: Int = 50) {
        val dir = File(context.cacheDir, "decoded")
        if (!dir.exists()) return
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keepFreshCount).forEach { it.delete() }
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
            .take(16)
    }
}
