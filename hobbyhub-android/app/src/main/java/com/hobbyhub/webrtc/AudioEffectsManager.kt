package com.hobbyhub.webrtc

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.hobbyhub.R

object AudioEffectsManager {
    private const val TAG = "AudioEffectsManager"

    fun playVoiceJoinLeaveSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.voice_in_out)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()
            Log.d(TAG, "Playing voice join/leave sound effect")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound effect", e)
        }
    }
}
