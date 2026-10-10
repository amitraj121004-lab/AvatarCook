package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exported_videos")
data class ExportedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val characterName: String,
    val videoPath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long,
    val resolution: String,
    val aspectRatio: String,
    val fps: Int,
    val fileSizeBytes: Long,
    val modeUsed: String,
    val createdAt: Long = System.currentTimeMillis()
)
