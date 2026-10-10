package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportedVideoDao {
    @Query("SELECT * FROM exported_videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<ExportedVideoEntity>>

    @Query("SELECT * FROM exported_videos WHERE id = :id")
    suspend fun getVideoById(id: Long): ExportedVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: ExportedVideoEntity): Long

    @Update
    suspend fun updateVideo(video: ExportedVideoEntity)

    @Query("UPDATE exported_videos SET title = :newTitle WHERE id = :id")
    suspend fun renameVideo(id: Long, newTitle: String)

    @Delete
    suspend fun deleteVideo(video: ExportedVideoEntity)

    @Query("DELETE FROM exported_videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)
}
