package com.skbsakib.audiosuperpower.library

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes any Android-supported audio file (MP3, AAC, M4A, FLAC, OGG, OPUS, WAV)
 * to a temporary 16-bit PCM WAV file in app cache.
 *
 * The native AudioPlayer then loads that WAV via WavReader. One-shot decode
 * on load; playback is instantaneous after.
 *
 * Phase 4 replaces this with a streaming ring buffer fed by MediaCodec
 * directly into the native engine — real-time, zero cache WAV.
 */
object AudioDecoder {

    private const val TAG = "SKB-AudioDecoder"
    private const val TIMEOUT_US = 10_000L

    data class Result(val wavPath: String, val sampleRate: Int, val channels: Int)

    /** Returns a WAV path (original if already WAV, else a decoded cache copy) or null. */
    fun decodeToCache(context: Context, sourcePath: String): Result? {
        val source = File(sourcePath)
        if (!source.exists()) { Log.e(TAG, "not found: $sourcePath"); return null }

        if (sourcePath.endsWith(".wav", ignoreCase = true)) {
            return Result(sourcePath, 0, 0)
        }

        // Extract audio track format
        val extractor = MediaExtractor()
        try { extractor.setDataSource(sourcePath) }
        catch (t: Throwable) { Log.e(TAG, "extractor: ${t.message}"); return null }

        val idx = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: run { extractor.release(); return null }

        extractor.selectTrack(idx)
        val fmt = extractor.getTrackFormat(idx)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: run { extractor.release(); return null }

        val codec = try { MediaCodec.createDecoderByType(mime) }
        catch (t: Throwable) {
            Log.e(TAG, "createDecoder: ${t.message}"); extractor.release(); return null
        }

        codec.configure(fmt, null, null, 0)
        codec.start()

        val srcRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val srcCh   = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val cacheDir = File(context.cacheDir, "decoded").apply { mkdirs() }
        val outFile  = File(cacheDir, "skb_${System.currentTimeMillis()}.wav")
        val out      = RandomAccessFile(outFile, "rw")

        // Reserve WAV header (44 bytes); patched at the end
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
            runCatching { outFile.delete() }
            runCatching { codec.stop(); codec.release() }
            extractor.release()
            return null
        }

        // Patch WAV header
        out.seek(0)
        writeWavHeader(out, srcRate, srcCh, 16, pcmBytes)
        out.close()

        runCatching { codec.stop(); codec.release() }
        extractor.release()

        Log.i(TAG, "decoded → ${outFile.name} ($pcmBytes bytes, $srcRate Hz, $srcCh ch)")
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

    /** Best-effort cleanup of decoded cache WAVs older than 24h. */
    fun cleanOldCache(context: Context) {
        val dir = File(context.cacheDir, "decoded")
        if (!dir.exists()) return
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }
}
