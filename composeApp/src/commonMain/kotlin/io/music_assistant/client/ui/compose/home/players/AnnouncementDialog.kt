package io.music_assistant.client.ui.compose.home.players

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import musicassistantclient.composeapp.generated.resources.Res
import musicassistantclient.composeapp.generated.resources.common_done
import musicassistantclient.composeapp.generated.resources.play_announcement
import musicassistantclient.composeapp.generated.resources.play_announcement_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * Placeholder for the player announcement flow.
 *
 * Recording and upload controls will be added here next.
 */
@Composable
fun AnnouncementDialog(
    playerName: String,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.MicNone,
                contentDescription = null,
            )
        },
        title = {
            Text(stringResource(Res.string.play_announcement))
        },
        text = {
            Text(
                stringResource(
                    Res.string.play_announcement_placeholder,
                    playerName,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.common_done))
            }
        },
    )
}
