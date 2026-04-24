package com.visionassist.eyetest.ui.screens.blurref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.model.BlurLadder
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BlurReferenceUiState(
    val ladder: BlurLadder,
    val selectedTierIndex: Int?,
    val loadingConfig: Boolean
)

class BlurReferenceViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    val focalPattern: FocalDifficultyPattern?
        get() = repository.questionnaireAnswers?.focalPattern

    private val _state = MutableStateFlow(
        BlurReferenceUiState(
            ladder = repository.currentBlurLadder(),
            selectedTierIndex = repository.blurIllustrationLevelIndex,
            loadingConfig = true
        )
    )
    val state: StateFlow<BlurReferenceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val latest = repository.fetchBlurLadder()
            _state.value = _state.value.copy(ladder = latest, loadingConfig = false)
        }
    }

    fun selectTier(index: Int) {
        _state.value = _state.value.copy(selectedTierIndex = index)
    }

    fun persistSelection() {
        repository.setBlurIllustrationLevelIndex(_state.value.selectedTierIndex)
    }
}
