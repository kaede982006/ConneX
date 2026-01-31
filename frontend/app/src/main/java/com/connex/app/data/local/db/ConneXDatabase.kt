package com.connex.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.connex.app.data.local.db.dao.UserDao
import com.connex.app.data.local.db.dao.RoomDao
import com.connex.app.data.local.db.dao.ChannelDao
import com.connex.app.data.local.db.entity.UserEntity
import com.connex.app.data.local.db.entity.RoomEntity
import com.connex.app.data.local.db.entity.ChannelEntity

@Database(entities = [UserEntity::class, RoomEntity::class, ChannelEntity::class, com.connex.app.data.local.db.entity.MessageEntity::class], version = 2)
abstract class ConneXDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun roomDao(): RoomDao
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): com.connex.app.data.local.db.dao.MessageDao
}
