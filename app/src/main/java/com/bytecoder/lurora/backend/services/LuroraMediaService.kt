package com.bytecoder.lurora.backend.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.bytecoder.lurora.R
import com.bytecoder.lurora.backend.player.LuroraMediaEngine
import com.bytecoder.lurora.MainActivity

/**
 * MediaSessionService for background music playback
 * Provides media controls in notification and lock screen
 */
@AndroidEntryPoint
class LuroraMediaService : MediaSessionService() {
    
    @Inject
    lateinit var mediaEngine: LuroraMediaEngine
    
    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "lurora_media_playback"
        private const val CHANNEL_NAME = "Music Playback"
        
        fun startService(context: Context) {
            val intent = Intent(context, LuroraMediaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LuroraMediaService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Create MediaSession
        mediaSession = MediaSession.Builder(this, mediaEngine.exoPlayer!!)
            .setCallback(MediaSessionCallback())
            .build()
        
        // Set up player listener for notification updates
        mediaEngine.exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
                updateLockScreenMetadata()
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotification()
                updateLockScreenMetadata()
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    stopSelf()
                } else {
                    updateLockScreenMetadata()
                }
            }
        })
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val currentMediaItem = mediaEngine.exoPlayer?.currentMediaItem
        val isPlaying = mediaEngine.exoPlayer?.isPlaying ?: false
        
        // Create intent to open main activity
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create media control intents
        val playPauseIntent = createMediaActionIntent(MediaActionReceiver.ACTION_PLAY_PAUSE)
        val previousIntent = createMediaActionIntent(MediaActionReceiver.ACTION_PREVIOUS)
        val nextIntent = createMediaActionIntent(MediaActionReceiver.ACTION_NEXT)
        val stopIntent = createMediaActionIntent(MediaActionReceiver.ACTION_STOP)
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown Track")
            .setContentText(currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist")
            .setSubText(currentMediaItem?.mediaMetadata?.albumTitle?.toString())
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                previousIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                nextIntent
            )
        
        return builder.build()
    }
    
    private fun createMediaActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaActionReceiver::class.java).apply {
            setAction(action)
        }
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun updateNotification() {
        if (mediaEngine.exoPlayer?.currentMediaItem != null) {
            val notification = createNotification()
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }
    
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            return drawable.bitmap
        }
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    /**
     * Update lock screen metadata for media controls
     */
    private fun updateLockScreenMetadata() {
        val currentMediaItem = mediaEngine.exoPlayer?.currentMediaItem
        currentMediaItem?.let { item ->
            // Update MediaSession metadata for lock screen controls
            mediaSession?.let { session ->
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(item.mediaMetadata.title ?: "Unknown Title")
                    .setArtist(item.mediaMetadata.artist ?: "Unknown Artist")
                    .setAlbumTitle(item.mediaMetadata.albumTitle ?: "Unknown Album")
                    .setArtworkUri(item.mediaMetadata.artworkUri)
                    .build()
                
                // Create updated media item with metadata
                val updatedItem = MediaItem.Builder()
                    .setUri(item.localConfiguration?.uri ?: android.net.Uri.EMPTY)
                    .setMediaMetadata(metadata)
                    .build()
                
                // This would update the MediaSession's current item in a real implementation
                // session.setCurrentMediaItem(updatedItem)
            }
        }
    }
    
    /**
     * MediaSession callback to handle media controls
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        // Basic callback implementation - Media3 handles the basic controls automatically
    }
}