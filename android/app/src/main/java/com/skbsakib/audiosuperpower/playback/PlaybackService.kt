package com.skbsakib.audiosuperpower.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.skbsakib.audiosuperpower.MainActivity
import com.skbsakib.audiosuperpower.R
import com.skbsakib.audiosuperpower.player.NativePlayer
import com.skbsakib.audiosuperpower.player.PlayerState

/**
 * Foreground service that keeps audio playback alive when the app is backgrounded.
 *
 * Android 14+ requires foregroundServiceType=mediaPlayback and the corresponding
 * FOREGROUND_SERVICE_MEDIA_PLAYBACK permission (both declared in the Manifest).
 */
class PlaybackService : Service() {

    companion object {
        const val ACTION_PLAY  = "com.skbsakib.audiosuperpower.PLAY"
        const val ACTION_PAUSE = "com.skbsakib.audiosuperpower.PAUSE"
        const val ACTION_STOP  = "com.skbsakib.audiosuperpower.STOP"

        private const val CHANNEL_ID = "skb_playback"
        private const val NOTIF_ID   = 1001

        fun start(context: Context) {
            val i = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY  -> NativePlayer.play()
            ACTION_PAUSE -> NativePlayer.pause()
            ACTION_STOP  -> { NativePlayer.stop(); stopSelf(); return START_NOT_STICKY }
        }

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 29)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else 0
        )
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "SKB Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }
            mgr.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val snap = NativePlayer.snapshot.value

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseAction = if (snap.state == PlayerState.PLAYING) {
            action(ACTION_PAUSE, "Pause",
                android.R.drawable.ic_media_pause)
        } else {
            action(ACTION_PLAY, "Play",
                android.R.drawable.ic_media_play)
        }
        val stopAction = action(ACTION_STOP, "Stop",
            android.R.drawable.ic_menu_close_clear_cancel)

        val title = snap.title.ifBlank { "SKB Audio Superpower" }
        val text = when {
            snap.artist.isNotBlank() && snap.sourceInfo.isNotBlank() ->
                "${snap.artist} · ${snap.sourceInfo}"
            snap.artist.isNotBlank() -> snap.artist
            else -> "2080 AUDIO LAB"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(snap.state == PlayerState.PLAYING)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .build()
    }

    private fun action(action: String, label: String, iconRes: Int): NotificationCompat.Action {
        val i = Intent(this, PlaybackService::class.java).setAction(action)
        val pi = PendingIntent.getService(
            this, action.hashCode(), i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action(iconRes, label, pi)
    }

    /** Called externally (from MainActivity) to refresh the notification on state change. */
    fun refresh() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_ID, buildNotification())
        }
    }
}
