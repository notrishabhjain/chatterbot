package com.digitaltwin.assistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.digitaltwin.assistant.data.local.dao.CaptureDao
import com.digitaltwin.assistant.data.local.dao.ContactTagDao
import com.digitaltwin.assistant.data.local.dao.WorkItemDao
import com.digitaltwin.assistant.data.local.entity.CallRecord
import com.digitaltwin.assistant.data.local.entity.CapturedNotification
import com.digitaltwin.assistant.data.local.entity.ContactTag
import com.digitaltwin.assistant.data.local.entity.WorkItem

@Database(
    entities = [
        WorkItem::class,
        CapturedNotification::class,
        CallRecord::class,
        ContactTag::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TwinDatabase : RoomDatabase() {
    abstract fun workItemDao(): WorkItemDao
    abstract fun captureDao(): CaptureDao
    abstract fun contactTagDao(): ContactTagDao

    companion object {
        const val NAME = "twin.db"
    }
}
