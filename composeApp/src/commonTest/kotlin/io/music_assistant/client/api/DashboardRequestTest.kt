package io.music_assistant.client.api

import io.music_assistant.client.data.model.server.DashboardType
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the mobile dashboard requests to the server DashboardController API. */
class DashboardRequestTest {
    @Test
    fun devicesCanBeFilteredByDashboardType() {
        val request = Request.Dashboard.devices(DashboardType.NOW_PLAYING)

        assertEquals(APICommands.DASHBOARD_DASHBOARDS, request.command)
        assertEquals(JsonPrimitive("now_playing"), request.args?.get("dashboard"))
    }

    @Test
    fun allDevicesHasNoArgs() {
        val request = Request.Dashboard.devices()

        assertEquals(APICommands.DASHBOARD_DASHBOARDS, request.command)
        assertNull(request.args)
    }

    @Test
    fun showNormalCarriesTargetPlayer() {
        val request = Request.Dashboard.show(
            dashboardId = "chromecast_shield",
            type = DashboardType.NOW_PLAYING,
            playerId = "living-room",
        )

        assertEquals(APICommands.DASHBOARD_SHOW, request.command)
        assertEquals(JsonPrimitive("chromecast_shield"), request.args?.get("dashboard_id"))
        assertEquals(JsonPrimitive("now_playing"), request.args?.get("dashboard"))
        assertEquals(JsonPrimitive("living-room"), request.args?.get("player_id"))
    }

    @Test
    fun showPartyOmitsPlayer() {
        val request = Request.Dashboard.show(
            dashboardId = "chromecast_shield",
            type = DashboardType.PARTY,
        )

        assertEquals(setOf("dashboard_id", "dashboard"), request.args?.keys)
    }

    @Test
    fun hideCarriesOnlyDashboardId() {
        val request = Request.Dashboard.hide("chromecast_shield")

        assertEquals(APICommands.DASHBOARD_HIDE, request.command)
        assertEquals(setOf("dashboard_id"), request.args?.keys)
    }

    @Test
    fun normalUrlCarriesPlayer() {
        val request = Request.Dashboard.getUrl(
            type = DashboardType.NOW_PLAYING,
            playerId = "living-room",
        )

        assertEquals(APICommands.DASHBOARD_GET_URL, request.command)
        assertEquals(JsonPrimitive("now_playing"), request.args?.get("dashboard"))
        assertEquals(JsonPrimitive("living-room"), request.args?.get("player_id"))
    }
}
