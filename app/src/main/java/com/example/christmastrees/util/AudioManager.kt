package com.example.christmastrees.util

import android.media.MediaPlayer
import androidx.compose.runtime.compositionLocalOf
import com.example.christmastrees.R

class AudioManager private constructor(private val context: android.content.Context) {
    val appContext: android.content.Context
        get() = context

    private var mediaPlayer: MediaPlayer? = null
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean
        get() = _isPlaying

    init {
        SettingsManager.init(context)
        try {
            val resourceId = R.raw.carol_of_the_bells
            val mp = MediaPlayer.create(context, resourceId)
            if (mp != null) {
                mp.isLooping = true
                mp.setOnCompletionListener {
                    _isPlaying = false
                }
                mp.setOnErrorListener { mpErr, error, extra ->
                    android.util.Log.e("AudioManager", "MediaPlayer error: $error, extra: $extra")
                    _isPlaying = false
                    true
                }
                mediaPlayer = mp
                android.util.Log.d("AudioManager", "MediaPlayer created successfully, resource=$resourceId")

                // Restore music state from settings
                if (SettingsManager.isMusicEnabled(context)) {
                    _isPlaying = true
                    mp.seekTo(0)
                    mp.start()
                    android.util.Log.d("AudioManager", "Restored music state: playing on init")
                }
            } else {
                android.util.Log.e("AudioManager", "Failed to create MediaPlayer from resource $resourceId")
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "Failed to initialize MediaPlayer", e)
        }
    }

    fun start() {
        val mp = mediaPlayer
        if (mp != null) {
            if (!_isPlaying) {
                try {
                    mp.seekTo(0)
                    mp.start()
                    _isPlaying = true
                    SettingsManager.saveMusicEnabled(appContext, true)
                    android.util.Log.d("AudioManager", "Started playing")
                } catch (e: Exception) {
                    android.util.Log.e("AudioManager", "Failed to start playback", e)
                }
            } else {
                android.util.Log.d("AudioManager", "Already playing, skipping start")
            }
        } else {
            android.util.Log.w("AudioManager", "Cannot start: mediaPlayer is null")
        }
    }

    fun stop() {
        val mp = mediaPlayer
        if (mp != null && _isPlaying) {
            try {
                mp.pause()
                _isPlaying = false
                SettingsManager.saveMusicEnabled(appContext, false)
                android.util.Log.d("AudioManager", "Paused playing")
            } catch (e: Exception) {
                android.util.Log.e("AudioManager", "Failed to pause playback", e)
            }
        }
    }

    fun toggle() {
        android.util.Log.d("AudioManager", "toggle called, current state: $_isPlaying")
        if (_isPlaying) {
            stop()
        } else {
            start()
        }
    }

    fun release() {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                if (_isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                android.util.Log.e("AudioManager", "Failed to release MediaPlayer", e)
            }
        }
        mediaPlayer = null
        _isPlaying = false
        android.util.Log.d("AudioManager", "Released")
    }

    companion object {
        @Volatile
        private var instance: AudioManager? = null

        fun init(context: android.content.Context) {
            instance ?: synchronized(this) {
                instance ?: AudioManager(context.applicationContext).also { instance = it }
            }
        }

        fun get(): AudioManager? = instance

        fun requireGet(): AudioManager = instance
            ?: throw IllegalStateException("AudioManager not initialized. Call init() first.")

        fun clear() {
            instance?.release()
            instance = null
        }
    }
}

val LocalAudioManager = compositionLocalOf<AudioManager?> { null }
