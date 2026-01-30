package com.connex.app.di

import android.content.Context
import com.connex.app.crypto.aead.AesGcm
import com.connex.app.crypto.keys.IdentityKeyManager
import com.connex.app.crypto.keys.KeyStoreProvider
import com.connex.app.crypto.keys.RoomKeyManager
import com.connex.app.crypto.protocol.MessageCodec
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides @Singleton fun provideKeyStoreProvider(): KeyStoreProvider = KeyStoreProvider()



    @Provides @Singleton fun provideAesGcm(): AesGcm = AesGcm()

    @Provides @Singleton fun provideMessageCodec(moshi: Moshi): MessageCodec = MessageCodec(moshi)
}
