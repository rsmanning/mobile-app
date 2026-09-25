package io.music_assistant.client.api

import io.music_assistant.client.data.model.server.DashboardType
import io.music_assistant.client.data.model.server.events.DashboardSessionsUpdatedEvent
import io.music_assistant.client.data.model.server.events.DashboardsUpdatedEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DashboardEventTest {
    private fun decode(raw: String) =
        Event(Json.parseToJsonElement(raw) as JsonObject).event()

    @Test
    fun decodesDashboardDevicesSnapshot() {
        val decoded = decode(
            """{
                "event": "dashboards_updated",
                "data": [{
                    "dashboard_id": "chromecast_shield",
                    "name": "SHIELD",
                    "supported_types": ["party", "now_playing", "music_quiz"],
                    "provider_domain_hint": "chromecast"
                }]
            }""",
        )

        val event = assertIs<DashboardsUpdatedEvent>(decoded)
        assertEquals("SHIELD", event.data.single().name)
        assertEquals(
            listOf(DashboardType.PARTY, DashboardType.NOW_PLAYING, DashboardType.MUSIC_QUIZ),
            event.data.single().supportedTypes,
        )
    }

    @Test
    fun decodesDashboardSessionsSnapshot() {
        val decoded = decode(
            """{
                "event": "dashboard_sessions_updated",
                "data": [{
                    "dashboard_id": "chromecast_shield",
                    "name": "SHIELD",
                    "dashboard": "now_playing",
                    "player_id": "living-room"
                }]
            }""",
        )

        val event = assertIs<DashboardSessionsUpdatedEvent>(decoded)
        assertEquals(DashboardType.NOW_PLAYING, event.data.single().dashboard)
        assertEquals("living-room", event.data.single().playerId)
    }
}
