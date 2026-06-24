package com.digitaltwin.assistant.di

import android.content.Context
import androidx.room.Room
import com.digitaltwin.assistant.data.local.TwinDatabase
import com.digitaltwin.assistant.data.local.dao.CaptureDao
import com.digitaltwin.assistant.data.local.dao.ContactTagDao
import com.digitaltwin.assistant.data.local.dao.WorkItemDao
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
    fun provideDatabase(@ApplicationContext ctx: Context): TwinDatabase =
        Room.databaseBuilder(ctx, TwinDatabase::class.java, TwinDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideWorkItemDao(db: TwinDatabase): WorkItemDao = db.workItemDao()
    @Provides fun provideCaptureDao(db: TwinDatabase): CaptureDao = db.captureDao()
    @Provides fun provideContactTagDao(db: TwinDatabase): ContactTagDao = db.contactTagDao()
}
