package com.visionassist.eyetest.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingUiState(
    val ageText: String = "",
    val screenTimeText: String = "",
    val error: String? = null
)

class OnboardingViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _ui.asStateFlow()

    fun onAgeChange(value: String) {
        _ui.update { it.copy(ageText = value.filter { ch -> ch.isDigit() }.take(3), error = null) }
    }

    fun onScreenTimeChange(value: String) {
        _ui.update { it.copy(screenTimeText = value.filter { ch -> ch.isDigit() }.take(2), error = null) }
    }

    fun saveAndContinue(): Boolean {
        val age = _ui.value.ageText.toIntOrNull()
        if (age == null || age !in 5..120) {
            _ui.update { it.copy(error = "Please enter a valid age (5–120).") }
            return false
        }
        val st = _ui.value.screenTimeText.toIntOrNull()
        repository.setUserProfile(
            UserProfile(
                age = age,
                screenTimeHoursPerDay = st?.coerceIn(0, 24)
            )
        )
        return true
    }
}
