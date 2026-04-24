package com.visionassist.eyetest.ui.screens.questionnaire

import androidx.lifecycle.ViewModel
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import com.visionassist.eyetest.domain.engine.QuestionnaireEngine
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import com.visionassist.eyetest.domain.model.QuestionnaireAnswers
import com.visionassist.eyetest.domain.model.SymptomLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QuestionnaireUiState(
    val focalPattern: FocalDifficultyPattern? = null,
    val distanceBlur: SymptomLevel? = null,
    val nearDifficulty: SymptomLevel? = null,
    val eyeStrain: SymptomLevel? = null,
    val nightVision: SymptomLevel? = null,
    val askNightVision: Boolean = false
)

class QuestionnaireViewModel(
    private val repository: VisionAssistRepository
) : ViewModel() {

    private val engine = QuestionnaireEngine()
    private val _ui = MutableStateFlow(QuestionnaireUiState())
    val uiState: StateFlow<QuestionnaireUiState> = _ui.asStateFlow()

    fun setFocalPattern(pattern: FocalDifficultyPattern) {
        _ui.update { it.copy(focalPattern = pattern) }
    }

    fun setDistanceBlur(level: SymptomLevel) {
        _ui.update { s ->
            val askNight = engine.shouldAskNightVision(level, s.eyeStrain ?: SymptomLevel.NONE)
            s.copy(distanceBlur = level, askNightVision = askNight)
        }
    }

    fun setNearDifficulty(level: SymptomLevel) {
        _ui.update { it.copy(nearDifficulty = level) }
    }

    fun setEyeStrain(level: SymptomLevel) {
        _ui.update { s ->
            val askNight = s.distanceBlur?.let { engine.shouldAskNightVision(it, level) } ?: false
            s.copy(eyeStrain = level, askNightVision = askNight)
        }
    }

    fun setNight(level: SymptomLevel) {
        _ui.update { it.copy(nightVision = level) }
    }

    fun skipNightIfNotAsked() {
        _ui.update { s ->
            if (!s.askNightVision && s.focalPattern != null && s.distanceBlur != null &&
                s.nearDifficulty != null && s.eyeStrain != null
            ) {
                s.copy(nightVision = SymptomLevel.NONE)
            } else {
                s
            }
        }
    }

    fun commit() {
        val s = _ui.value
        val answers = QuestionnaireAnswers(
            focalPattern = s.focalPattern ?: FocalDifficultyPattern.NEITHER,
            distanceBlur = s.distanceBlur ?: SymptomLevel.NONE,
            nearDifficulty = s.nearDifficulty ?: SymptomLevel.NONE,
            eyeStrain = s.eyeStrain ?: SymptomLevel.NONE,
            nightVision = if (s.askNightVision) s.nightVision else null
        )
        repository.setQuestionnaire(answers)
    }

    fun isComplete(): Boolean {
        val s = _ui.value
        if (s.focalPattern == null || s.distanceBlur == null || s.nearDifficulty == null || s.eyeStrain == null) {
            return false
        }
        return if (s.askNightVision) s.nightVision != null else true
    }
}
