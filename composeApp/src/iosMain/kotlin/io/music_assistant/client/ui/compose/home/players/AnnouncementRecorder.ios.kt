package io.music_assistant.client.ui.compose.home.players

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAnnouncementRecorder(): AnnouncementRecorder =
    remember { UnsupportedAnnouncementRecorder() }

private class UnsupportedAnnouncementRecorder : AnnouncementRecorder {
    override val isRecording: Boolean = false
    override val error: AnnouncementRecorderError = AnnouncementRecorderError.UNSUPPORTED

    override fun start(): Boolean = false

    override suspend fun finish(): AnnouncementRecording? = null

    override fun cancel() = Unit
}
