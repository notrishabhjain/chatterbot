package com.digitaltwin.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digitaltwin.assistant.data.local.entity.ContactTag
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: ContactTag)

    @Query("SELECT * FROM contact_tags ORDER BY displayName ASC")
    fun observeAll(): Flow<List<ContactTag>>

    @Query("SELECT * FROM contact_tags WHERE matchKey = :key LIMIT 1")
    suspend fun findByKey(key: String): ContactTag?

    @Query("DELETE FROM contact_tags WHERE id = :id")
    suspend fun delete(id: Long)
}
