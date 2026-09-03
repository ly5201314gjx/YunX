package com.yunx.cloud.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResolveHistoryDao {

    @Query("SELECT * FROM resolve_history ORDER BY createTime DESC LIMIT 200")
    fun observeAll(): Flow<List<ResolveHistoryEntity>>

    @Insert
    suspend fun insert(history: ResolveHistoryEntity): Long

    @Query("UPDATE resolve_history SET directUrl = :url WHERE id = :id")
    suspend fun updateDirectUrl(id: Long, url: String)

    @Query("DELETE FROM resolve_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM resolve_history")
    suspend fun clearAll()
}
