package io.music_assistant.client.api

import io.music_assistant.client.data.model.server.events.Event
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.webrtc.DataChannelWrapper
import io.music_assistant.client.webrtc.WebRTCHttpProxy
import io.music_assistant.client.webrtc.model.RemoteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ServiceClient {
    val sessionState: StateFlow<SessionState>

    suspend fun sendRequest(request: Request): Result<Answer>
    suspend fun playLiveAnnouncement(
        playerId: String,
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("Live announcements are not supported by this client."),
    )

    suspend fun playLiveAnnouncement(
        playerId: String,
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
        preAnnounce: Boolean,
    ): Result<Unit> = playLiveAnnouncement(
        playerId = playerId,
        pcm = pcm,
        sampleRate = sampleRate,
        channels = channels,
    )
    suspend fun login(username: String, password: String)
    suspend fun authorize(token: String, isAutoLogin: Boolean = false)
    fun logout()
    val isReadyForCommands: StateFlow<Boolean>
    fun resolveImageUrl(
        path: String,
        provider: String,
        isRemotelyAccessible: Boolean,
        proxyId: String?,
    ): String?
    fun rebaseServerImageUrl(rawUrl: String): String?
    val webRTCHttpProxy: WebRTCHttpProxy?
    fun forceWebRTCReconnect()
    val events: Flow<Event<out Any>>
    val webrtcSendspinChannel: DataChannelWrapper?

    fun onAppForeground()
    fun onAppBackground()
    val foregroundEvents: Flow<Unit>
    fun disconnectByUser()
    fun connect(connection: ConnectionInfo)
    fun connectWebRTC(remoteId: RemoteId)
    fun onExternalConsumerActive()

    /**
     * Asks for the command session back for a request that cannot go through
     * [sendRequest] yet (an offline-queued local-player command). Fire-and-forget:
     * the caller is not blocked while the session recovers.
     */
    fun requestCommandRecovery()

    fun onPlaybackActive()
    fun onExternalConsumerInactive()
    fun onPlaybackInactive()
    fun forceDisconnect(reason: Exception)
    fun noServer()

    /** True while an external consumer (Android Auto / CarPlay) is bound. Cross-platform car edge. */
    val externalConsumerActive: StateFlow<Boolean>
}
