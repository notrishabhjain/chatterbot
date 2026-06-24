package com.digitaltwin.assistant

import com.digitaltwin.assistant.ai.ExtractionContext
import com.digitaltwin.assistant.ai.RuleBasedExtractor
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import com.digitaltwin.assistant.data.model.Source
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedExtractorTest {

    private val extractor = RuleBasedExtractor()
    private val ctx = ExtractionContext(source = Source.WHATSAPP, contact = "Priya")

    @Test
    fun `English action request is extracted as MY_TASK`() = runTest {
        val results = extractor.extract("Please send the proposal by tomorrow", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(ItemType.MY_TASK, results.first().type)
    }

    @Test
    fun `Hindi action request is extracted`() = runTest {
        val results = extractor.extract("Report bhej do aaj", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(ItemType.MY_TASK, results.first().type)
    }

    @Test
    fun `Delegation marker produces DELEGATED item`() = runTest {
        val results = extractor.extract("Ask Rahul to prepare the deck", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(ItemType.DELEGATED, results.first().type)
    }

    @Test
    fun `Follow-up marker produces FOLLOW_UP item`() = runTest {
        val results = extractor.extract("Waiting for client to revert on budget", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(ItemType.FOLLOW_UP, results.first().type)
    }

    @Test
    fun `Status completion produces STATUS_UPDATE`() = runTest {
        val results = extractor.extract("ho gaya", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(ItemType.STATUS_UPDATE, results.first().type)
    }

    @Test
    fun `Urgent keyword sets HIGH priority`() = runTest {
        val results = extractor.extract("urgent: please review the contract asap", ctx)
        assertTrue(results.isNotEmpty())
        assertEquals(Priority.HIGH, results.first().priority)
    }

    @Test
    fun `Unactionable text returns empty`() = runTest {
        val results = extractor.extract("ok thanks", ctx)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `Tomorrow due date is set`() = runTest {
        val results = extractor.extract("please send the file tomorrow", ctx)
        assertTrue(results.isNotEmpty())
        assertFalse(results.first().dueAt == null)
    }
}
