package com.example.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import com.example.data.model.MediaPlaybackState
import com.example.overlay.LumaMediaNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaPlaybackMonitor(private val context: Context) {

    private val _mediaState = MutableStateFlow(MediaPlaybackState())
    val mediaState: StateFlow<MediaPlaybackState> = _mediaState.asStateFlow()

    private var activeController: MediaController? = null
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController)
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindBestController(controllers)
        }

    fun startObserving() {
        val listenerComponent = ComponentName(context, LumaMediaNotificationListener::class.java)
        runCatching {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent
            )
            val current = mediaSessionManager?.getActiveSessions(listenerComponent)
            bindBestController(current)
        }
    }

    fun stopObserving() {
        runCatching {
            activeController?.unregisterCallback(controllerCallback)
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        }
        activeController = null
    }

    fun refreshSessions() {
        val listenerComponent = ComponentName(context, LumaMediaNotificationListener::class.java)
        runCatching {
            val current = mediaSessionManager?.getActiveSessions(listenerComponent)
            bindBestController(current)
        }
    }

    private fun bindBestController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val best = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        activeController = best
        best?.registerCallback(controllerCallback)
        updateFromController(best)
    }

    private fun updateFromController(controller: MediaController?) {
        if (controller == null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val musicActive = audioManager?.isMusicActive == true
            _mediaState.value = MediaPlaybackState(
                hasActiveSession = musicActive,
                title = if (musicActive) "Audio Active" else "Not Playing",
                artist = if (musicActive) "Enable Media Access for metadata" else "Tap play or launch music",
                isPlaying = musicActive
            )
            return
        }

        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Playing Media"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: controller.packageName
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        val appLabel = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(controller.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault("")

        _mediaState.value = MediaPlaybackState(
            hasActiveSession = true,
            title = title,
            artist = artist,
            appName = appLabel,
            packageName = controller.packageName,
            isPlaying = isPlaying,
            albumArt = art
        )
    }

    fun togglePlayPause() {
        val controller = activeController
        if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } else {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    fun skipNext() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToNext()
        } else {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun skipPrevious() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
        } else {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun openSourceAppOrOutput() {
        val pkg = activeController?.packageName
        if (pkg != null) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(launch) }
                return
            }
        }
        val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(soundIntent) }
    }

    private fun sendMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }
}
