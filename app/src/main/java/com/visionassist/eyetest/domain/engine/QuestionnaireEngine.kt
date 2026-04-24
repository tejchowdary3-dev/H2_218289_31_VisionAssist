package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.SymptomLevel

/**
 * Chooses follow-up questions from symptom answers (triage-style, not diagnostic).
 */
class QuestionnaireEngine {

    fun shouldAskNightVision(distanceBlur: SymptomLevel, eyeStrain: SymptomLevel): Boolean {
        return distanceBlur.ordinal >= SymptomLevel.MODERATE.ordinal ||
            eyeStrain.ordinal >= SymptomLevel.MODERATE.ordinal
    }
}
