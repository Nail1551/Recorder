package com.example.recorder8

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import java.io.File

class RecordingAdapter(private val recordings: MutableList<Recording>) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    private var currentPlayingPosition: Int = -1
    private var mediaPlayer: MediaPlayer? = null

    inner class RecordingViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.item_recording, parent, false)) {
        val recordingName: TextView = itemView.findViewById(R.id.recordingName)
        val playButton: Button = itemView.findViewById(R.id.btnPlayPause)
        val deleteButton: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return RecordingViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        // Используем position только для получения данных записи на момент биндинга
        val recording = recordings[position]
        holder.recordingName.text = recording.name

        val isPlaying = (position == currentPlayingPosition)
        holder.playButton.text = if (isPlaying) "⏸" else "▶"

        holder.playButton.setOnClickListener {
            // Здесь не используем position напрямую, а обращаемся к holder.adapterPosition
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            val currentIsPlaying = (adapterPos == currentPlayingPosition)
            if (currentIsPlaying) {
                // Пауза
                mediaPlayer?.pause()
                currentPlayingPosition = -1
                notifyItemChanged(adapterPos)
            } else {
                // Останавливаем предыдущий плеер
                stopPlaying()
                // Запускаем новый файл
                val filePath = recordings[adapterPos].filePath
                startPlaying(filePath) {
                    currentPlayingPosition = -1
                    notifyItemChanged(adapterPos)
                }
                currentPlayingPosition = adapterPos
                notifyDataSetChanged()
            }
        }

        holder.deleteButton.setOnClickListener {
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            // Останавливаем воспроизведение, если удаляем текущую воспроизводимую запись
            if (adapterPos == currentPlayingPosition) {
                stopPlaying()
                currentPlayingPosition = -1
            }

            deleteRecording(adapterPos)
        }
    }

    override fun getItemCount(): Int = recordings.size

    fun updateRecordings(newRecordings: List<Recording>) {
        recordings.clear()
        recordings.addAll(newRecordings)
        notifyDataSetChanged()
    }

    private fun startPlaying(filePath: String, onCompletion: () -> Unit) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            setOnCompletionListener {
                onCompletion()
            }
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    private fun deleteRecording(position: Int) {
        val recording = recordings[position]
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
        recordings.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, recordings.size)
    }
}