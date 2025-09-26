package com.bytecoder.lurora.backend.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.bytecoder.lurora.backend.player.LuroraMediaEngine

/**
 * BroadcastReceiver to handle media control actions from notification
 */
@AndroidEntryPoint
class MediaActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var mediaEngine: LuroraMediaEngine
    
    companion object {
        const val ACTION_PLAY_PAUSE = "com.bytecoder.lurora.ACTION_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.bytecoder.lurora.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.bytecoder.lurora.ACTION_NEXT"
        const val ACTION_STOP = "com.bytecoder.lurora.ACTION_STOP"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (mediaEngine.isPlaying()) {
                    mediaEngine.pause()
                } else {
                    mediaEngine.play()
                }
            }
            
            ACTION_PREVIOUS -> {
                mediaEngine.seekToPrevious()
            }
            
            ACTION_NEXT -> {
                mediaEngine.seekToNext()
            }
            
            ACTION_STOP -> {
                mediaEngine.stop()
                context?.let { ctx ->
                    LuroraMediaService.stopService(ctx)
                }
            }
        }
    }
}