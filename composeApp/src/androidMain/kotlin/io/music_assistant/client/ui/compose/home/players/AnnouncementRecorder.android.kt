package io.music_assistant.client.ui.compose.home.players

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private const val READ_BUFFER_MILLIS = 100L

@Composable
internal actual fun rememberAnnouncementRecorder(): AnnouncementRecorder {
    val context = LocalContext.current
    val recorder = remember(context) {
        AndroidAnnouncementRecorder(context.applicationContext)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        recorder.onPermissionResult(granted)
    }

    SideEffect {
        recorder.permissionRequester = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(recorder) {
        onDispose { recorder.dispose() }
    }

    return recorder
}

private class AndroidAnnouncementRecorder(
    private val context: Context,
) : AnnouncementRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var recordingState by mutableStateOf(false)
    private var errorState by mutableStateOf<AnnouncementRecorderError?>(null)
    private var session: RecordingSession? = null

    var permissionRequester: (() -> Unit)? = null

    override val isRecording: Boolean
        get() = recordingState

    override val error: AnnouncementRecorderError?
        get() = errorState

    override fun start(): Boolean {
        errorState = null

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequester?.invoke()
            return false
        }

        if (session != null) {
            return true
        }

        return startWithPermission()
    }

    fun onPermissionResult(granted: Boolean) {
        errorState = if (granted) null else AnnouncementRecorderError.PERMISSION_DENIED
    }

    @SuppressLint("MissingPermission")
    private fun startWithPermission(): Boolean {
        val minBufferSize = AudioRecord.getMinBufferSize(
            ANNOUNCEMENT_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            errorState = AnnouncementRecorderError.INITIALIZATION_FAILED
            return false
        }

        val targetBufferSize = (
            ANNOUNCEMENT_SAMPLE_RATE_HZ *
                ANNOUNCEMENT_BYTES_PER_SAMPLE *
                READ_BUFFER_MILLIS /
                1_000L
            ).toInt()
        val bufferSize = maxOf(minBufferSize, targetBufferSize)

        val audioRecord = runCatching {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(ANNOUNCEMENT_SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .build()
        }.getOrElse {
            errorState = AnnouncementRecorderError.INITIALIZATION_FAILED
            return false
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            errorState = AnnouncementRecorderError.INITIALIZATION_FAILED
            return false
        }

        val newSession = RecordingSession(
            audioRecord = audioRecord,
            bufferSize = bufferSize,
        )

        val started = runCatching {
            audioRecord.startRecording()
            audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING
        }.getOrDefault(false)

        if (!started) {
            audioRecord.release()
            errorState = AnnouncementRecorderError.INITIALIZATION_FAILED
            return false
        }

        session = newSession
        recordingState = true
        newSession.job = scope.launch {
            readPcm(newSession)
        }
        return true
    }

    private fun readPcm(recordingSession: RecordingSession) {
        val buffer = ByteArray(recordingSession.bufferSize)
        val maxBytes = (
            ANNOUNCEMENT_SAMPLE_RATE_HZ.toLong() *
                ANNOUNCEMENT_CHANNELS *
                ANNOUNCEMENT_BYTES_PER_SAMPLE *
                ANNOUNCEMENT_MAX_DURATION_MS /
                1_000L
            ).toInt()
        var remaining = maxBytes

        while (recordingSession.running.get() && remaining > 0) {
            val requested = minOf(buffer.size, remaining)
            val count = recordingSession.audioRecord.read(buffer, 0, requested)
            when {
                count > 0 -> {
                    recordingSession.output.write(buffer, 0, count)
                    remaining -= count
                }

                count == 0 -> Unit
                else -> {
                    recordingSession.readFailed = true
                    recordingSession.running.set(false)
                }
            }
        }
    }

    override suspend fun finish(): AnnouncementRecording? {
        val current = session ?: return null
        session = null
        current.running.set(false)
        runCatching { current.audioRecord.stop() }
        listOfNotNull(current.job).joinAll()
        current.audioRecord.release()
        recordingState = false

        if (current.readFailed || current.output.size() == 0) {
            errorState = AnnouncementRecorderError.RECORDING_FAILED
            return null
        }

        errorState = null
        return AnnouncementRecording(
            pcm = current.output.toByteArray(),
        )
    }

    override fun cancel() {
        val current = session ?: run {
            recordingState = false
            return
        }
        session = null
        current.running.set(false)
        runCatching { current.audioRecord.stop() }
        current.job?.cancel()
        current.audioRecord.release()
        recordingState = false
    }

    fun dispose() {
        cancel()
        scope.cancel()
    }

    private class RecordingSession(
        val audioRecord: AudioRecord,
        val bufferSize: Int,
        val output: ByteArrayOutputStream = ByteArrayOutputStream(),
        val running: AtomicBoolean = AtomicBoolean(true),
        var job: Job? = null,
        var readFailed: Boolean = false,
    )
}
