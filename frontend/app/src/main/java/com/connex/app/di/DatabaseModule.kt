package com.connex.app.di

import android.content.Context
import androidx.room.Room
import com.connex.app.data.local.db.ConneXDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ConneXDatabase {
        return Room.databaseBuilder(
            context,
            ConneXDatabase::class.java,
            "connex.db"
        ).build()
    }

    @Provides
    fun provideUserDao(db: ConneXDatabase) = db.userDao()

    @Provides
    fun provideRoomDao(db: ConneXDatabase) = db.roomDao()

    @Provides
    fun provideChannelDao(db: ConneXDatabase) = db.channelDao()

    @Provides
    fun provideMessageDao(db: ConneXDatabase) = db.messageDao()
}
