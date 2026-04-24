package com.visionassist.eyetest.domain.model

/**
 * Subjective refraction screening output. **Not** a prescription, not a refraction, not
 * equivalent to autorefractor / retinoscopy / phoropter. Used to drive on-screen copy only.
 */
data class RefractionEstimate(
    val severity: RefractionSeverity,
    /** Approximate spherical-equivalent range in diopters. null means no usable estimate. */
    val diopterFeelLow: Float?,
    val diopterFeelHigh: Float?,
    /** Dominant focal issue inferred from inputs — for copy/UI only. */
    val dominantFocal: FocalDifficultyPattern,
    /** Rough confidence 0..1 based on how many signals agreed. */
    val confidence01: Float,
    /** Human-readable explanation of which inputs drove this estimate. */
    val rationale: List<String>
)

enum class RefractionSeverity {
    NONE,
    MILD,
    MODERATE,
    STRONG,
    INDETERMINATE
}
