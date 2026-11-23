package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Spatializer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import com.example.bluetooth.FeatureSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

val EqBandLabels = listOf(
    "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
    "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"
)

val EqBandCenterHz = listOf(
    31f, 62f, 125f, 250f, 500f,
    1000f, 2000f, 4000f, 8000f, 16000f
)

data class BuiltInEqPreset(
    val name: String,
    val bandsDb: List<Float>,
    val bassBoost: Float,
    val vocalClarity: Float,
    val trebleBoost: Float,
    val loudness: Float,
    val soundStage: Float
)

val BuiltInEqPresets = listOf(
    BuiltInEqPreset("Balanced", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0.1f, 0.1f, 0.0f, 0.0f, 0.4f),
    BuiltInEqPreset("Deep Bass", listOf(7.5f, 6.5f, 5.0f, 2.0f, -0.5f, 0.0f, 1.0f, 2.5f, 3.0f, 2.5f), 0.78f, 0.15f, 0.2f, 0.25f, 0.45f),
    BuiltInEqPreset("Punch Bass", listOf(5.0f, 6.5f, 6.0f, 3.0f, 0.0f, 0.5f, 1.5f, 2.5f, 2.0f, 1.5f), 0.65f, 0.2f, 0.25f, 0.2f, 0.4f),
    BuiltInEqPreset("Vocal", listOf(-2.0f, -1.5f, -0.5f, 1.5f, 3.5f, 4.5f, 5.0f, 3.5f, 2.0f, 1.0f), 0.05f, 0.8f, 0.35f, 0.15f, 0.35f),
    BuiltInEqPreset("Pop", listOf(2.5f, 3.5f, 2.5f, 0.5f, -1.0f, 1.5f, 3.0f, 4.0f, 3.5f, 2.5f), 0.35f, 0.45f, 0.4f, 0.2f, 0.5f),
    BuiltInEqPreset("Rock", listOf(4.5f, 3.5f, 1.5f, -1.5f, -2.0f, 1.0f, 3.5f, 4.5f, 5.0f, 4.0f), 0.4f, 0.35f, 0.5f, 0.25f, 0.55f),
    BuiltInEqPreset("EDM", listOf(6.5f, 5.5f, 3.5f, 0.0f, -1.5f, 0.5f, 2.5f, 4.5f, 5.5f, 5.0f), 0.7f, 0.2f, 0.55f, 0.3f, 0.65f),
    BuiltInEqPreset("Jazz", listOf(2.0f, 2.5f, 1.5f, 1.0f, 2.0f, 2.5f, 1.5f, 2.5f, 3.0f, 2.5f), 0.2f, 0.4f, 0.3f, 0.1f, 0.6f),
    BuiltInEqPreset("Classical", listOf(1.5f, 1.0f, 0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 4.5f), 0.1f, 0.3f, 0.45f, 0.05f, 0.75f),
    BuiltInEqPreset("Cinema", listOf(6.0f, 5.0f, 3.0f, 1.0f, 1.5f, 3.0f, 3.5f, 2.5f, 3.0f, 2.0f), 0.6f, 0.55f, 0.3f, 0.35f, 0.85f),
    BuiltInEqPreset("Gaming", listOf(3.0f, 2.0f, -1.0f, -2.0f, 1.0f, 3.0f, 5.5f, 6.0f, 4.5f, 3.0f), 0.3f, 0.5f, 0.65f, 0.25f, 0.8f)
)

data class AudioEngineStatus(
    val globalFxAttached: Boolean = false,
    val auditionPlaying: Boolean = false,
    val hardwareEqualizerBands: Int = 0,
    val equalizerActive: Boolean = false,
    val bassBoostActive: Boolean = false,
    val virtualizerActive: Boolean = false,
    val loudnessActive: Boolean = false,
    val processingModeLabel: String = "Android AudioFX Software DSP",
    val spatializerAvailable: Boolean = false,
    val spatializerEnabled: Boolean = false,
    val headTrackerAvailable: Boolean = false,
    val spatializerLevelLabel: String = "Checking Android Spatializer..."
)

class KaranAudioEngine(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow(AudioEngineStatus())
    val status: StateFlow<AudioEngineStatus> = _status.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var auditionJob: Job? = null
    private var currentSessionId: Int = 0
    private var lastBandsDb: List<Float> = List(10) { 0f }
    private var lastBass: Float = 0.15f
    private var lastVocal: Float = 0f
    private var lastTreble: Float = 0f
    private var lastLoudness: Float = 0f
    private var lastBalance: Float = 0f
    private var lastStage: Float = 0.45f
    private var lastVirtualSurround: Boolean = false

    init {
        attachAudioEffects(sessionId = 0)
        refreshSpatializerStatus()
    }

    fun checkHardwareSpatializer(): FeatureSupport {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return FeatureSupport.Unsupported("Hardware Spatializer requires Android 13+")
        }
        val sp = audioManager?.spatializer
        return if (sp != null && sp.isAvailable) {
            val stateStr = if (sp.isEnabled) "Active on Android Audio Pipeline" else "Available in System Settings"
            FeatureSupport.Supported(stateStr)
        } else {
            FeatureSupport.Unsupported("Not supported by current device or audio route")
        }
    }

    fun checkHeadTracking(): FeatureSupport {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return FeatureSupport.Unsupported("Requires Android 13+ Spatializer")
        }
        val sp = audioManager?.spatializer
        return if (sp != null && sp.isAvailable && sp.isHeadTrackerAvailable) {
            FeatureSupport.Supported("Head Tracker sensor detected by Android Spatializer")
        } else {
            FeatureSupport.Unsupported("Not supported by this device")
        }
    }

    fun refreshSpatializerStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sp = audioManager?.spatializer
            val avail = sp?.isAvailable == true
            val enabled = sp?.isEnabled == true
            val headTracker = avail && (sp?.isHeadTrackerAvailable == true)
            val levelLabel = when (sp?.immersiveAudioLevel) {
                Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL -> "Android Multichannel Spatializer"
                Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_OTHER -> "Android Spatializer (Vendor Mode)"
                else -> if (avail) "Android Spatializer Ready" else "System Spatializer Unavailable (Software Virtualizer Active)"
            }
            _status.value = _status.value.copy(
                spatializerAvailable = avail,
                spatializerEnabled = enabled,
                headTrackerAvailable = headTracker,
                spatializerLevelLabel = levelLabel
            )
        } else {
            _status.value = _status.value.copy(
                spatializerAvailable = false,
                spatializerEnabled = false,
                headTrackerAvailable = false,
                spatializerLevelLabel = "Android 13+ required for system Spatializer; Software Virtualizer active"
            )
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        releaseEffects()
        currentSessionId = sessionId
        var eqOk = false
        var eqBands = 0
        var bassOk = false
        var virtOk = false
        var loudOk = false

        runCatching {
            val eq = Equalizer(0, sessionId)
            eq.enabled = true
            equalizer = eq
            eqBands = eq.numberOfBands.toInt()
            eqOk = true
        }
        runCatching {
            val bb = BassBoost(0, sessionId)
            bb.enabled = true
            bassBoost = bb
            bassOk = bb.strengthSupported
        }
        runCatching {
            @Suppress("DEPRECATION")
            val virt = Virtualizer(0, sessionId)
            virt.enabled = lastVirtualSurround
            virtualizer = virt
            virtOk = virt.strengthSupported
        }
        runCatching {
            val le = LoudnessEnhancer(sessionId)
            le.enabled = lastLoudness > 0.01f
            loudnessEnhancer = le
            loudOk = true
        }

        _status.value = _status.value.copy(
            globalFxAttached = sessionId == 0 && (eqOk || bassOk || virtOk),
            hardwareEqualizerBands = eqBands,
            equalizerActive = eqOk,
            bassBoostActive = bassOk,
            virtualizerActive = virtOk,
            loudnessActive = loudOk,
            processingModeLabel = if (sessionId == 0 && eqOk) {
                "Android Global AudioFX DSP ($eqBands HW bands)"
            } else if (eqOk) {
                "Android Dedicated Session AudioFX DSP ($eqBands bands)"
            } else {
                "Android AudioFX (Device restricts global session 0; use Audition Player to test live DSP)"
            }
        )
        applyCurrentParametersToFx()
    }

    fun applyProcessing(
        bandsDb: List<Float>,
        bassBoostAmount: Float,
        vocalClarity: Float,
        trebleBoost: Float,
        loudnessGain: Float,
        balance: Float,
        soundStage: Float,
        virtualSurround: Boolean
    ) {
        lastBandsDb = if (bandsDb.size == 10) bandsDb else List(10) { bandsDb.getOrElse(it) { 0f } }
        lastBass = bassBoostAmount.coerceIn(0f, 1f)
        lastVocal = vocalClarity.coerceIn(0f, 1f)
        lastTreble = trebleBoost.coerceIn(0f, 1f)
        lastLoudness = loudnessGain.coerceIn(0f, 1f)
        lastBalance = balance.coerceIn(-1f, 1f)
        lastStage = soundStage.coerceIn(0f, 1f)
        lastVirtualSurround = virtualSurround
        applyCurrentParametersToFx()
    }

    private fun applyCurrentParametersToFx() {
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange // [minMb, maxMb]
                val minMb = range[0].toFloat()
                val maxMb = range[1].toFloat()
                val numBands = eq.numberOfBands.toInt()
                for (bandIdx in 0 until numBands) {
                    val centerHz = eq.getCenterFreq(bandIdx.toShort()) / 1000f
                    val shapedDb = sampleShaped10BandCurveDb(centerHz)
                    val targetMb = (shapedDb * 100f).coerceIn(minMb, maxMb).roundToInt().toShort()
                    eq.setBandLevel(bandIdx.toShort(), targetMb)
                }
            }
        }
        bassBoost?.let { bb ->
            runCatching {
                if (bb.strengthSupported) {
                    val strength = (lastBass * 1000f).roundToInt().coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            }
        }
        virtualizer?.let { virt ->
            runCatching {
                virt.enabled = lastVirtualSurround || lastStage > 0.55f
                if (virt.strengthSupported) {
                    val strength = if (lastVirtualSurround) {
                        (lastStage * 1000f).roundToInt().coerceIn(200, 1000).toShort()
                    } else {
                        ((lastStage - 0.5f).coerceAtLeast(0f) * 1400f).roundToInt().coerceIn(0, 1000).toShort()
                    }
                    virt.setStrength(strength)
                }
            }
        }
        loudnessEnhancer?.let { le ->
            runCatching {
                le.enabled = lastLoudness > 0.02f
                // Up to +1200 mB (12 dB) clean loudness boost
                le.setTargetGain((lastLoudness * 1200f).roundToInt())
            }
        }
    }

    private fun sampleShaped10BandCurveDb(freqHz: Float): Float {
        // Interpolate the 10-band curve + add vocal clarity (1k-4k) + treble boost (6k-16k)
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        EqBandCenterHz.forEachIndexed { idx, center ->
            val dist = abs(center - freqHz)
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = idx
            }
        }
        var db = lastBandsDb.getOrElse(bestIdx) { 0f }
        if (freqHz in 900f..4500f) {
            db += lastVocal * 4.5f
        }
        if (freqHz >= 5500f) {
            db += lastTreble * 5.0f
        }
        return db.coerceIn(-12f, 12f)
    }

    /**
     * Plays a real-time stereo acoustic audition chord through AudioTrack bound to AudioFX
     * so users can immediately hear 10-band EQ, Bass Boost, Vocal, Balance, and Spatial Soundstage.
     */
    fun toggleAuditionPlayer() {
        if (auditionJob?.isActive == true) {
            stopAuditionPlayer()
            return
        }
        _status.value = _status.value.copy(auditionPlaying = true)
        auditionJob = scope.launch {
            val sampleRate = 44100
            val bufferFrames = 2048
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferFrames * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            attachAudioEffects(track.audioSessionId)
            track.play()

            val pcm = ShortArray(bufferFrames * 2)
            var sampleIndex = 0L
            // Warm studio chord frequencies (sub-bass 65Hz, 130Hz, 261Hz, 392Hz, 523Hz, shimmer 1046Hz)
            val freqs = doubleArrayOf(65.41, 130.81, 261.63, 329.63, 392.0, 523.25)
            try {
                while (isActive) {
                    val leftGain = (1f - lastBalance.coerceAtLeast(0f)).coerceIn(0.15f, 1f)
                    val rightGain = (1f + lastBalance.coerceAtMost(0f)).coerceIn(0.15f, 1f)
                    val stageSpread = lastStage * 0.35

                    for (i in 0 until bufferFrames) {
                        val t = sampleIndex.toDouble() / sampleRate
                        val pulse = 0.65 + 0.35 * sin(2.0 * PI * 1.25 * t)
                        var lSum = 0.0
                        var rSum = 0.0
                        for (k in freqs.indices) {
                            val phase = 2.0 * PI * freqs[k] * t
                            val amp = if (k == 0) 0.32 * pulse else 0.14
                            val panOffset = if (k % 2 == 0) -stageSpread else stageSpread
                            lSum += sin(phase) * amp * (1.0 - panOffset)
                            rSum += sin(phase + stageSpread * 0.4) * amp * (1.0 + panOffset)
                        }
                        val lSample = (lSum * leftGain * 22000.0).roundToInt().coerceIn(-32767, 32767).toShort()
                        val rSample = (rSum * rightGain * 22000.0).roundToInt().coerceIn(-32767, 32767).toShort()
                        pcm[i * 2] = lSample
                        pcm[i * 2 + 1] = rSample
                        sampleIndex++
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
                attachAudioEffects(0)
                _status.value = _status.value.copy(auditionPlaying = false)
            }
        }
    }

    fun stopAuditionPlayer() {
        auditionJob?.cancel()
        auditionJob = null
        _status.value = _status.value.copy(auditionPlaying = false)
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
