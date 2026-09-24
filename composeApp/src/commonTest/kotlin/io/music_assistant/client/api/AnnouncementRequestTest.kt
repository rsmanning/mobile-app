package io.music_assistant.client.api

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnnouncementRequestTest {
    @Test
    fun `typed announcement sends message chime preference and volume override`() {
        val request = Request.Player.playAnnouncement(
            playerId = "player-1",
            message = "Testing testing 1 2 3",
            preAnnounce = false,
            volumeLevel = 25,
        )

        assertEquals(APICommands.PLAYERS_CMD_PLAY_ANNOUNCEMENT, request.command)
        assertEquals("player-1", (request.args?.get("player_id") as JsonPrimitive).content)
        assertEquals(
            "Testing testing 1 2 3",
            (request.args?.get("message") as JsonPrimitive).content,
        )
        assertEquals("false", (request.args?.get("pre_announce") as JsonPrimitive).content)
        assertEquals("25", (request.args?.get("volume_level") as JsonPrimitive).content)
    }

    @Test
    fun `typed announcement omits volume override when default is selected`() {
        val request = Request.Player.playAnnouncement(
            playerId = "player-1",
            message = "Use the configured announcement volume",
            preAnnounce = true,
        )

        assertNull(request.args?.get("volume_level"))
    }
}
