package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ExportedVideoEntity
import com.example.data.repository.ExportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExportRepository(db.exportedVideoDao())

    val videos: StateFlow<List<ExportedVideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun renameVideo(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameVideo(id, newTitle)
        }
    }

    fun deleteVideo(video: ExportedVideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }
}
