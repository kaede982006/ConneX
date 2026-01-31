package com.connex.app.di

import com.connex.app.BuildConfig
import com.connex.app.data.remote.api.AuthApi
import com.connex.app.data.remote.api.AuthInterceptor
import com.connex.app.data.remote.api.RoomApi
import com.connex.app.data.remote.api.UploadApi
import com.connex.app.data.remote.api.UserApi
import com.connex.app.data.remote.ws.ChatSocket
import com.connex.app.data.remote.ws.OkHttpChatSocket
import com.connex.app.data.local.prefs.SecurePrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(prefs: SecurePrefs): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideRoomApi(retrofit: Retrofit): RoomApi = retrofit.create(RoomApi::class.java)

    @Provides
    @Singleton
    fun provideUploadApi(retrofit: Retrofit): UploadApi = retrofit.create(UploadApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideChatSocket(
        client: OkHttpClient,
        moshi: Moshi,
        prefs: SecurePrefs
    ): ChatSocket {
        // OkHttp requires http/https scheme even for WebSockets (it handles upgrade).
        // Passing ws:// causes toHttpUrlOrNull() to return null.
        val wsUrl = BuildConfig.API_BASE_URL
        return OkHttpChatSocket(client, moshi, prefs, wsUrl)
    }
}
