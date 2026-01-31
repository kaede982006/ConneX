package com.connex.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val ownerId: Int?
)
