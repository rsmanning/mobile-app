package io.music_assistant.client.data.model.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** First server API schema containing the dashboard controller and its public commands. */
const val DASHBOARD_MIN_SCHEMA = 39

/** First server API schema able to route the Music Quiz dashboard. */
const val MUSIC_QUIZ_DASHBOARD_MIN_SCHEMA = 43

const val PARTY_PROVIDER_DOMAIN = "party"
const val MUSIC_QUIZ_PROVIDER_DOMAIN = "music_quiz"
const val DASHBOARD_REQUIRED_SCOPE = "users.invite"

fun supportsDashboards(schemaVersion: Int?): Boolean =
    schemaVersion != null && schemaVersion >= DASHBOARD_MIN_SCHEMA

fun supportsMusicQuizDashboard(schemaVersion: Int?): Boolean =
    schemaVersion != null && schemaVersion >= MUSIC_QUIZ_DASHBOARD_MIN_SCHEMA

@Serializable
enum class DashboardType(val serverValue: String) {
    @SerialName("party")
    PARTY("party"),

    @SerialName("now_playing")
    NOW_PLAYING("now_playing"),

    @SerialName("music_quiz")
    MUSIC_QUIZ("music_quiz"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
}

@Serializable
data class DashboardDevice(
    @SerialName("dashboard_id") val dashboardId: String,
    @SerialName("name") val name: String,
    @SerialName("supported_types") val supportedTypes: List<DashboardType> = emptyList(),
    @SerialName("provider_domain_hint") val providerDomainHint: String? = null,
)

@Serializable
data class DashboardSession(
    @SerialName("dashboard_id") val dashboardId: String,
    @SerialName("name") val name: String,
    @SerialName("dashboard") val dashboard: DashboardType,
    @SerialName("player_id") val playerId: String? = null,
)

/**
 * A session is the selected display context when its dashboard type matches and, for
 * Now Playing, it is pinned to the same player.
 */
fun DashboardSession.matches(type: DashboardType, playerId: String?): Boolean =
    dashboard == type && (type != DashboardType.NOW_PLAYING || this.playerId == playerId)
