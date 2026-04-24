package com.visionassist.eyetest.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.model.TestResultRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    repository: VisionAssistRepository
) : ViewModel() {

    val records: StateFlow<List<TestResultRecord>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}
