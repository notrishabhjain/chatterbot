package com.digitaltwin.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitaltwin.assistant.ai.ExtractionCandidate
import com.digitaltwin.assistant.ai.ExtractionContext
import com.digitaltwin.assistant.ai.RuleBasedExtractor
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import com.digitaltwin.assistant.data.model.Source
import com.digitaltwin.assistant.data.repository.CaptureRepository
import com.digitaltwin.assistant.data.repository.ContactClassifier
import com.digitaltwin.assistant.data.repository.WorkItemRepository
import com.digitaltwin.assistant.data.repository.toWorkItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val workItemRepo: WorkItemRepository,
    private val captureRepo: CaptureRepository,
    private val extractor: RuleBasedExtractor,
    private val classifier: ContactClassifier,
) : ViewModel() {

    val queue = workItemRepo.queue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _extracting = MutableStateFlow(false)
    val extracting = _extracting.asStateFlow()

    fun approve(item: WorkItem) = viewModelScope.launch { workItemRepo.approve(item) }
    fun discard(item: WorkItem) = viewModelScope.launch { workItemRepo.discard(item) }

    /** Manual capture: extract candidates from pasted/shared text and queue them. */
    fun extractAndQueue(text: String) = viewModelScope.launch {
        _extracting.value = true
        try {
            val ctx = ExtractionContext(source = Source.MANUAL)
            val candidates = extractor.extract(text, ctx)
            if (candidates.isEmpty()) {
                // Nothing matched — queue a MY_TASK with the raw text so user can review it.
                val fallback = ExtractionCandidate(
                    title = text.take(140),
                    type = ItemType.MY_TASK,
                    priority = Priority.MEDIUM,
                    confidence = 0.3,
                )
                val cat = classifier.categoryFor(null)
                workItemRepo.insert(fallback.toWorkItem(Source.MANUAL, null, cat))
            } else {
                candidates.forEach { candidate ->
                    val cat = classifier.categoryFor(candidate.contact)
                    workItemRepo.insert(candidate.toWorkItem(Source.MANUAL, candidate.contact, cat))
                }
            }
        } finally {
            _extracting.value = false
        }
    }
}
