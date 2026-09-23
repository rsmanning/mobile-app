package io.music_assistant.client.api

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AnnouncementRequestTest {
    @Test
    fun `typed announcement sends message and explicit chime preference`() {
        val request = Request.Player.playAnnouncement(
            playerId = "player-1",
            message = "Testing testing 1 2 3",
            preAnnounce = false,
        )

        assertEquals(APICommands.PLAYERS_CMD_PLAY_ANNOUNCEMENT, request.command)
        assertEquals("player-1", (request.args?.get("player_id") as JsonPrimitive).content)
        assertEquals(
            "Testing testing 1 2 3",
            (request.args?.get("message") as JsonPrimitive).content,
        )
        assertEquals("false", (request.args?.get("pre_announce") as JsonPrimitive).content)
    }
}
