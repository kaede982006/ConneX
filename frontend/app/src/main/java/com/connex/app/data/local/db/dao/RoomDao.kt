package com.connex.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.connex.app.data.local.db.entity.RoomEntity

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms")
    suspend fun listAll(): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rooms: List<RoomEntity>)
}
