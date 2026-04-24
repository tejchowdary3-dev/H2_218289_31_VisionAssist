package com.visionassist.eyetest.ui.screens.calibration

import androidx.lifecycle.ViewModel
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalibrationUiState(
    /** Slider 0f..1f maps to scale around 1f (match credit-card width on screen). */
    val slider01: Float = 0.5f,
    val referenceWidthMm: Float = 85.6f
)

class CalibrationViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _ui.asStateFlow()

    init {
        repository.setCalibrationScale(lerp(0.75f, 1.25f, _ui.value.slider01))
    }

    fun onSliderChange(value: Float) {
        _ui.update { it.copy(slider01 = value) }
        val scale = lerp(0.75f, 1.25f, value)
        repository.setCalibrationScale(scale)
    }

    fun resetToDefault() {
        _ui.update { it.copy(slider01 = 0.5f) }
        repository.setCalibrationScale(1f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
