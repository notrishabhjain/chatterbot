package com.digitaltwin.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.digitaltwin.assistant.data.local.entity.WorkItem
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.repository.WorkItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val repo: WorkItemRepository,
) : ViewModel() {

    val myTasks = repo.activeByType(ItemType.MY_TASK)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val delegated = repo.activeByType(ItemType.DELEGATED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val followUps = repo.activeByType(ItemType.FOLLOW_UP)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resolved = repo.resolved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun resolve(item: WorkItem) = viewModelScope.launch { repo.resolve(item.id) }
}
