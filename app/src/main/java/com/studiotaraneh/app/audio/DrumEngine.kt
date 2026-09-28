package com.studiotaraneh.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Small local drum synthesizer. It creates short PCM voices on-device,
 * so Drum Pattern playback does not depend on network audio or AI.
 */
class DrumEngine(private val sampleRate: Int = 44_100) {
    private val kick = createTrack(makeKick())
    private val snare = createTrack(makeSnare())
    private val hiHat = createTrack(makeHiHat())

    fun triggerKick() = trigger(kick)
    fun triggerSnare() = trigger(snare)
    fun triggerHiHat() = trigger(hiHat)

    private fun trigger(track: AudioTrack) {
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.setPlaybackHeadPosition(0)
            track.play()
        }
    }

    fun release() {
        runCatching { kick.release() }
        runCatching { snare.release() }
        runCatching { hiHat.release() }
    }

    private fun createTrack(samples: ShortArray): AudioTrack {
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(format)
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .apply {
                write(samples, 0, samples.size)
                setVolume(0.9f)
            }
    }

    private fun makeKick(): ShortArray {
        val length = (sampleRate * 0.20).toInt()
        return ShortArray(length) { i ->
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * PI * (
                55.0 + 105.0 * exp(-t * 25.0)
            ) * t
            val envelope = exp(-t * 22.0)
            (sin(phase) * envelope * 0.92 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun makeSnare(): ShortArray {
        val length = (sampleRate * 0.16).toInt()
        var previous = 0.0
        return ShortArray(length) { i ->
            val t = i.toDouble() / sampleRate
            val noise = Random.nextDouble(-1.0, 1.0)
            val highPassed = noise - previous * 0.96
            previous = noise
            val tone = sin(2.0 * PI * 185.0 * t) * 0.22
            val envelope = exp(-t * 24.0)
            ((highPassed * 0.72 + tone) * envelope * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun makeHiHat(): ShortArray {
        val length = (sampleRate * 0.055).toInt()
        var previous = 0.0
        return ShortArray(length) { i ->
            val t = i.toDouble() / sampleRate
            val noise = Random.nextDouble(-1.0, 1.0)
            val highPassed = noise - previous * 0.92
            previous = noise
            val envelope = exp(-t * 90.0)
            (highPassed * envelope * 0.38 * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
