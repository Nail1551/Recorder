package com.example.recorder8

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var waveformView: WaveformView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterRecordings: RecordingAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value == true }
        if (!granted) {
            Toast.makeText(this, "Необходимо разрешение для записи аудио!", Toast.LENGTH_SHORT).show()
        }
    }

    private var updateWaveJob: Job? = null

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissionsIfNeeded()

        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        waveformView = findViewById(R.id.waveformView)

        recyclerView = findViewById(R.id.recordingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterRecordings = RecordingAdapter(mutableListOf())
        recyclerView.adapter = adapterRecordings

        loadRecordings()

        btnRecord.setOnClickListener {
            startRecording()
        }

        btnStop.setOnClickListener {
            stopRecording()
        }
    }

    private fun loadRecordings() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val recordings = dir.listFiles { file -> file.extension == "mp3" || file.extension == "m4a" }
            ?.map { Recording(it.name, it.absolutePath) } ?: emptyList()

        adapterRecordings.updateRecordings(recordings)
    }

    private fun startRecording() {
        if (isRecording) {
            Toast.makeText(this, "Уже идёт запись", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        if (!dir.exists()) dir.mkdirs()

        val filename = "recording_${System.currentTimeMillis()}.m4a"
        val filePath = File(dir, filename).absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(filePath)
            try {
                prepare()
                start()
                isRecording = true
                Toast.makeText(this@MainActivity, "Запись началась", Toast.LENGTH_SHORT).show()
                startUpdatingWaveform()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка при записи", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            Toast.makeText(this, "Запись не идёт", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            stopUpdatingWaveform()
            Toast.makeText(this, "Запись остановлена", Toast.LENGTH_SHORT).show()
            loadRecordings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startUpdatingWaveform() {
        updateWaveJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isRecording) {
                val amplitude = recorder?.maxAmplitude ?: 0
                waveformView.addAmplitude(amplitude.toFloat())
                delay(100L)
            }
        }
    }

    private fun stopUpdatingWaveform() {
        updateWaveJob?.cancel()
        updateWaveJob = null
    }
}
