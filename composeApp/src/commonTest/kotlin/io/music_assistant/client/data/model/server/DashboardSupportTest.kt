package io.music_assistant.client.data.model.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardSupportTest {
    @Test
    fun dashboardApiStartsAtSchema39() {
        assertFalse(supportsDashboards(38))
        assertTrue(supportsDashboards(39))
    }

    @Test
    fun musicQuizRoutingStartsAtSchema43() {
        assertFalse(supportsMusicQuizDashboard(42))
        assertTrue(supportsMusicQuizDashboard(43))
    }

    @Test
    fun normalSessionMatchesPlayerAsWellAsType() {
        val session = DashboardSession(
            dashboardId = "tv",
            name = "TV",
            dashboard = DashboardType.NOW_PLAYING,
            playerId = "living-room",
        )

        assertTrue(session.matches(DashboardType.NOW_PLAYING, "living-room"))
        assertFalse(session.matches(DashboardType.NOW_PLAYING, "kitchen"))
        assertFalse(session.matches(DashboardType.PARTY, null))
    }
}
