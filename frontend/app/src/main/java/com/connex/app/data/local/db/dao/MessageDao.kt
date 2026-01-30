package com.connex.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.connex.app.data.local.db.entity.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(msg: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(msgs: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE roomId = :roomId AND channelId = :channelId ORDER BY createdAtMs ASC LIMIT :limit")
    suspend fun list(roomId: String, channelId: String, limit: Int = 300): List<MessageEntity>

    @Query("DELETE FROM messages WHERE roomId = :roomId AND channelId = :channelId")
    suspend fun clear(roomId: String, channelId: String)
}
