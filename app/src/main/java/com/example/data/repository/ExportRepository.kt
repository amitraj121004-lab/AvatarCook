package com.example.data.repository

import com.example.data.local.ExportedVideoDao
import com.example.data.local.ExportedVideoEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class ExportRepository(private val dao: ExportedVideoDao) {

    val allVideos: Flow<List<ExportedVideoEntity>> = dao.getAllVideos()

    suspend fun insertVideo(video: ExportedVideoEntity): Long {
        return dao.insertVideo(video)
    }

    suspend fun renameVideo(id: Long, newTitle: String) {
        dao.renameVideo(id, newTitle)
    }

    suspend fun deleteVideo(video: ExportedVideoEntity) {
        // Also delete physical file on disk if exists
        try {
            val file = File(video.videoPath)
            if (file.exists()) file.delete()
            video.thumbnailPath?.let { thumb ->
                val thumbFile = File(thumb)
                if (thumbFile.exists()) thumbFile.delete()
            }
        } catch (_: Exception) {}
        dao.deleteVideo(video)
    }
}
