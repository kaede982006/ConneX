package com.connex.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.connex.app.data.local.db.entity.ChannelEntity

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE roomId = :roomId")
    suspend fun listByRoom(roomId: Int): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE roomId = :roomId")
    suspend fun clearRoom(roomId: Int)
}
