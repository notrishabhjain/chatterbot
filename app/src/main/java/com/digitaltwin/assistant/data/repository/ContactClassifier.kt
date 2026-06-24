package com.digitaltwin.assistant.data.repository

import com.digitaltwin.assistant.data.local.dao.ContactTagDao
import com.digitaltwin.assistant.data.model.Category
import javax.inject.Inject
import javax.inject.Singleton

/** Maps a captured contact name/number to a [Category] using the user's contact tags. */
@Singleton
class ContactClassifier @Inject constructor(
    private val contactTagDao: ContactTagDao,
) {
    suspend fun categoryFor(contact: String?): Category {
        val key = contact?.trim()?.lowercase() ?: return Category.UNCATEGORIZED
        return contactTagDao.findByKey(key)?.category ?: Category.UNCATEGORIZED
    }
}
