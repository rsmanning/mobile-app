package io.music_assistant.client.data.model.server.events

import io.music_assistant.client.data.model.server.DashboardDevice
import io.music_assistant.client.data.model.server.DashboardSession
import io.music_assistant.client.data.model.server.EventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardsUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String? = null,
    @SerialName("data") override val data: List<DashboardDevice>,
) : Event<List<DashboardDevice>>

@Serializable
data class DashboardSessionsUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String? = null,
    @SerialName("data") override val data: List<DashboardSession>,
) : Event<List<DashboardSession>>
