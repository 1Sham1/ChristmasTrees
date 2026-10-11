package com.example.christmastrees.util

import android.media.MediaPlayer
import androidx.compose.runtime.compositionLocalOf
import com.example.christmastrees.R

class AudioManager private constructor(private val context: android.content.Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var _isPlaying: Boolean = false
    private var _isPrepared: Boolean = false
    val isPlaying: Boolean
        get() = _isPlaying

    init {
        try {
            val resourceId = R.raw.carol_of_the_bells
            val mp = MediaPlayer.create(context, resourceId)
            if (mp != null) {
                mp.isLooping = true
                mp.setOnPreparedListener {
                    _isPrepared = true
                }
                mp.setOnCompletionListener {
                    _isPlaying = false
                    _isPrepared = false
                }
                mp.setOnErrorListener { mpErr, error, extra ->
                    android.util.Log.e("AudioManager", "MediaPlayer error: $error, extra: $extra")
                    _isPlaying = false
                    _isPrepared = false
                    true
                }
                mediaPlayer = mp
            } else {
                android.util.Log.e("AudioManager", "Failed to create MediaPlayer from resource")
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioManager", "Failed to initialize MediaPlayer", e)
        }
    }

    fun start() {
        val mp = mediaPlayer
        if (mp != null && !_isPlaying) {
            try {
                mp.seekTo(0)
                mp.start()
                _isPlaying = true
            } catch (e: Exception) {
                android.util.Log.e("AudioManager", "Failed to start playback", e)
            }
        }
    }

    fun stop() {
        val mp = mediaPlayer
        if (mp != null && _isPlaying) {
            try {
                mp.stop()
                _isPlaying = false
                _isPrepared = false
            } catch (e: Exception) {
                android.util.Log.e("AudioManager", "Failed to stop playback", e)
            }
        }
    }

    fun toggle() {
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
        _isPrepared = false
    }

    companion object {
        @Volatile
        private var instance: AudioManager? = null

        fun init(context: android.content.Context) {
            instance ?: synchronized(this) {
                instance ?: AudioManager(context.applicationContext).also { instance = it }
            }
        }

        fun get(): AudioManager = instance
            ?: throw IllegalStateException("AudioManager not initialized. Call init() first.")

        fun clear() {
            instance?.release()
            instance = null
        }
    }
}

val LocalAudioManager = compositionLocalOf<AudioManager?> { null }
