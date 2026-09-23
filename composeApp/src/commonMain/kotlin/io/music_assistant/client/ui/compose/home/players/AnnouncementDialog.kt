package io.music_assistant.client.ui.compose.home.players

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.announcement_hold_to_record
import musicassistantclient.composeapp.generated.resources.announcement_permission_denied
import musicassistantclient.composeapp.generated.resources.announcement_recorded
import musicassistantclient.composeapp.generated.resources.announcement_recording
import musicassistantclient.composeapp.generated.resources.announcement_recording_failed
import musicassistantclient.composeapp.generated.resources.announcement_recording_unsupported
import musicassistantclient.composeapp.generated.resources.announcement_send_failed
import musicassistantclient.composeapp.generated.resources.announcement_sending
import musicassistantclient.composeapp.generated.resources.common_accept
import musicassistantclient.composeapp.generated.resources.common_cancel
import musicassistantclient.composeapp.generated.resources.record_announcement
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AnnouncementDialog(
    playerName: String,
    onAccept: suspend (AnnouncementRecording) -> Result<Unit>,
    onDismissRequest: () -> Unit,
) {
    val recorder = rememberAnnouncementRecorder()
    val scope = rememberCoroutineScope()
    var completedRecording by remember { mutableStateOf<AnnouncementRecording?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    val recorderError = recorder.error
    val completed = completedRecording
    val genericSendError = stringResource(Res.string.announcement_send_failed)

    val statusText = when {
        isSending -> stringResource(Res.string.announcement_sending)

        sendError != null -> sendError.orEmpty()

        recorder.isRecording ->
            stringResource(Res.string.announcement_recording)

        recorderError != null ->
            stringResource(
                when (recorderError) {
                    AnnouncementRecorderError.PERMISSION_DENIED ->
                        Res.string.announcement_permission_denied

                    AnnouncementRecorderError.INITIALIZATION_FAILED,
                    AnnouncementRecorderError.RECORDING_FAILED,
                    -> Res.string.announcement_recording_failed

                    AnnouncementRecorderError.UNSUPPORTED ->
                        Res.string.announcement_recording_unsupported
                },
            )

        completed != null ->
            stringResource(
                Res.string.announcement_recorded,
                formatDurationSeconds(completed.durationMillis),
            )

        else ->
            stringResource(Res.string.announcement_hold_to_record, playerName)
    }

    val dismiss = {
        recorder.cancel()
        completedRecording = null
        onDismissRequest()
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSending) dismiss()
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.record_announcement),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = statusText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .pointerInput(recorder, isSending) {
                            if (!isSending) {
                                detectTapGestures(
                                    onPress = {
                                        sendError = null
                                        completedRecording = null
                                        if (recorder.start()) {
                                            if (tryAwaitRelease()) {
                                                completedRecording = recorder.finish()
                                            } else {
                                                recorder.cancel()
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    shape = CircleShape,
                    color = if (recorder.isRecording) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (recorder.isRecording) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(48.dp),
                            imageVector = Icons.Outlined.MicNone,
                            contentDescription = statusText,
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSending,
                onClick = dismiss,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = completed != null && !recorder.isRecording && !isSending,
                onClick = {
                    val recording = completedRecording ?: return@TextButton
                    isSending = true
                    sendError = null
                    scope.launch {
                        val result = onAccept(recording)
                        isSending = false
                        result.fold(
                            onSuccess = { dismiss() },
                            onFailure = { error ->
                                sendError = error.message?.takeIf { it.isNotBlank() }
                                    ?: genericSendError
                            },
                        )
                    }
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.common_accept))
                }
            }
        },
    )
}

private fun formatDurationSeconds(durationMillis: Long): String {
    val tenths = (durationMillis + 50L) / 100L
    return "${tenths / 10}.${tenths % 10}"
}
