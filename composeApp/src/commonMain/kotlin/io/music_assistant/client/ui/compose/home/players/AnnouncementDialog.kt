package io.music_assistant.client.ui.compose.home.players

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import musicassistantclient.composeapp.generated.resources.announcement_message_label
import musicassistantclient.composeapp.generated.resources.announcement_message_placeholder
import musicassistantclient.composeapp.generated.resources.announcement_mode_speak
import musicassistantclient.composeapp.generated.resources.announcement_mode_type
import musicassistantclient.composeapp.generated.resources.announcement_permission_denied
import musicassistantclient.composeapp.generated.resources.announcement_play_chime_first
import musicassistantclient.composeapp.generated.resources.announcement_recorded
import musicassistantclient.composeapp.generated.resources.announcement_recording
import musicassistantclient.composeapp.generated.resources.announcement_recording_failed
import musicassistantclient.composeapp.generated.resources.announcement_recording_unsupported
import musicassistantclient.composeapp.generated.resources.announcement_send_failed
import musicassistantclient.composeapp.generated.resources.announcement_sending
import musicassistantclient.composeapp.generated.resources.common_accept
import musicassistantclient.composeapp.generated.resources.common_cancel
import musicassistantclient.composeapp.generated.resources.play_announcement
import org.jetbrains.compose.resources.stringResource

private const val DURATION_ROUNDING_MILLIS = 50L
private const val MILLIS_PER_TENTH_SECOND = 100L

private enum class AnnouncementMode {
    TYPE,
    SPEAK,
}

@Composable
internal fun AnnouncementDialog(
    playerName: String,
    initialPreAnnounce: Boolean,
    onPreAnnounceChanged: (Boolean) -> Unit,
    onTextAccept: suspend (String, Boolean) -> Result<Unit>,
    onRecordingAccept: suspend (AnnouncementRecording, Boolean) -> Result<Unit>,
    onDismissRequest: () -> Unit,
) {
    val recorder = rememberAnnouncementRecorder()
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AnnouncementMode.TYPE) }
    var message by remember { mutableStateOf("") }
    var completedRecording by remember { mutableStateOf<AnnouncementRecording?>(null) }
    var preAnnounce by remember(initialPreAnnounce) { mutableStateOf(initialPreAnnounce) }
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    val recorderError = recorder.error
    val speakAvailable = recorderError != AnnouncementRecorderError.UNSUPPORTED
    val completed = completedRecording
    val genericSendError = stringResource(Res.string.announcement_send_failed)

    val speakStatusText = when {
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

    val canConfirm = when (mode) {
        AnnouncementMode.TYPE -> message.isNotBlank() && !isSending
        AnnouncementMode.SPEAK ->
            completed != null && !recorder.isRecording && !isSending
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSending) dismiss()
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.play_announcement),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (speakAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = mode == AnnouncementMode.TYPE,
                            enabled = !isSending && !recorder.isRecording,
                            onClick = {
                                recorder.cancel()
                                completedRecording = null
                                sendError = null
                                mode = AnnouncementMode.TYPE
                            },
                            label = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(Res.string.announcement_mode_type),
                                    textAlign = TextAlign.Center,
                                )
                            },
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = mode == AnnouncementMode.SPEAK,
                            enabled = !isSending && !recorder.isRecording,
                            onClick = {
                                sendError = null
                                mode = AnnouncementMode.SPEAK
                            },
                            label = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(Res.string.announcement_mode_speak),
                                    textAlign = TextAlign.Center,
                                )
                            },
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                }

                when (mode) {
                    AnnouncementMode.TYPE -> {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = message,
                            enabled = !isSending,
                            onValueChange = {
                                message = it
                                sendError = null
                            },
                            label = { Text(stringResource(Res.string.announcement_message_label)) },
                            placeholder = {
                                Text(stringResource(Res.string.announcement_message_placeholder))
                            },
                            minLines = 3,
                            maxLines = 6,
                        )
                        if (isSending || sendError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = if (isSending) {
                                    stringResource(Res.string.announcement_sending)
                                } else {
                                    sendError.orEmpty()
                                },
                                color = if (sendError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    AnnouncementMode.SPEAK -> {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = speakStatusText,
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
                                    contentDescription = speakStatusText,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(Res.string.announcement_play_chime_first),
                    )
                    Switch(
                        checked = preAnnounce,
                        enabled = !isSending && !recorder.isRecording,
                        onCheckedChange = { enabled ->
                            preAnnounce = enabled
                            onPreAnnounceChanged(enabled)
                        },
                    )
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
                enabled = canConfirm,
                onClick = {
                    isSending = true
                    sendError = null
                    scope.launch {
                        val result = when (mode) {
                            AnnouncementMode.TYPE ->
                                onTextAccept(message.trim(), preAnnounce)

                            AnnouncementMode.SPEAK -> {
                                val recording = completedRecording
                                if (recording == null) {
                                    Result.failure(
                                        IllegalStateException("No completed announcement recording."),
                                    )
                                } else {
                                    onRecordingAccept(recording, preAnnounce)
                                }
                            }
                        }
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
    val tenths = (durationMillis + DURATION_ROUNDING_MILLIS) / MILLIS_PER_TENTH_SECOND
    return "${tenths / 10}.${tenths % 10}"
}
