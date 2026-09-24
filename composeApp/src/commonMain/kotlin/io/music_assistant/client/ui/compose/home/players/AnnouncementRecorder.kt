package io.music_assistant.client.ui.compose.home.players

import androidx.compose.runtime.Composable

internal const val ANNOUNCEMENT_SAMPLE_RATE_HZ = 48_000
internal const val ANNOUNCEMENT_CHANNELS = 1
internal const val ANNOUNCEMENT_BYTES_PER_SAMPLE = 2
internal const val ANNOUNCEMENT_MAX_DURATION_MS = 300_000L

internal data class AnnouncementRecording(
    val pcm: ByteArray,
    val sampleRate: Int = ANNOUNCEMENT_SAMPLE_RATE_HZ,
    val channels: Int = ANNOUNCEMENT_CHANNELS,
) {
    val durationMillis: Long
        get() {
            val bytesPerSecond = sampleRate.toLong() * channels * ANNOUNCEMENT_BYTES_PER_SAMPLE
            return if (bytesPerSecond > 0) pcm.size.toLong() * 1_000L / bytesPerSecond else 0L
        }
}

internal enum class AnnouncementRecorderError {
    PERMISSION_DENIED,
    INITIALIZATION_FAILED,
    RECORDING_FAILED,
    UNSUPPORTED,
}

internal interface AnnouncementRecorder {
    val isRecording: Boolean
    val error: AnnouncementRecorderError?

    /**
     * Starts recording immediately when microphone permission is already granted.
     *
     * When permission is missing, this requests it and returns false. The user can hold
     * the record control again after granting permission.
     */
    fun start(): Boolean

    /** Stops capture, waits for the reader to finish, and returns one completed PCM clip. */
    suspend fun finish(): AnnouncementRecording?

    /** Stops and discards the current clip. */
    fun cancel()
}

@Composable
internal expect fun rememberAnnouncementRecorder(): AnnouncementRecorder
