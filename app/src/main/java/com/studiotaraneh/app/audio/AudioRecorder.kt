package com.studiotaraneh.app.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var paused = false
    fun start(): File {
        check(recorder == null) { "Recording already active" }
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "REC_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare(); start()
        }
        currentFile = file
        paused = false
        return file
    }
    fun pause() { recorder?.let { runCatching { it.pause(); paused = true } } }
    fun resume() { recorder?.let { runCatching { it.resume(); paused = false } } }
    fun stop(): File? { val r = recorder ?: return null; return try { r.stop(); r.release(); recorder = null; paused = false; currentFile } finally { recorder = null; paused = false } }
    fun cancel() { recorder?.release(); recorder = null; currentFile?.delete(); currentFile = null; paused = false }
    fun currentPath(): String? = currentFile?.absolutePath
    fun isRecording() = recorder != null
    fun isPaused(): Boolean = paused
}
