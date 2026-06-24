package com.digitaltwin.assistant.di

import com.digitaltwin.assistant.ai.GroqApi
import com.digitaltwin.assistant.ai.GroqTranscriber
import com.digitaltwin.assistant.ai.Transcriber
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
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
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindTranscriber(impl: GroqTranscriber): Transcriber

    companion object {
        @Provides
        @Singleton
        fun provideMoshi(): Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        @Provides
        @Singleton
        fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()

        @Provides
        @Singleton
        fun provideGroqApi(client: OkHttpClient, moshi: Moshi): GroqApi =
            Retrofit.Builder()
                .baseUrl(GroqApi.BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GroqApi::class.java)
    }
}
