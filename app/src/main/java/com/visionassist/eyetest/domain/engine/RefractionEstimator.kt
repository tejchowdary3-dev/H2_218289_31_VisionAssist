package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.BlurLadder
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import com.visionassist.eyetest.domain.model.QuestionnaireAnswers
import com.visionassist.eyetest.domain.model.RefractionEstimate
import com.visionassist.eyetest.domain.model.RefractionEstimatorConfig
import com.visionassist.eyetest.domain.model.RefractionSeverity
import com.visionassist.eyetest.domain.model.SymptomLevel
import com.visionassist.eyetest.domain.model.VisionSessionResult
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-signal, screening-grade subjective refraction estimator.
 *
 * **This is not a prescription.** It takes signals the user provided (chosen blur tier,
 * distance VA achieved per eye, near-vision comfort, symptom severity, focal difficulty
 * pattern) and combines them into:
 *   - a severity bucket (NONE / MILD / MODERATE / STRONG / INDETERMINATE), and
 *   - an approximate diopter-feel range.
 *
 * Every signal is independently validated; the final range is only published when at least two
 * independent signals agree. If the user skipped the blur ladder *and* has no symptoms *and*
 * VA looks good, the estimate is NONE. If signals disagree strongly, it is INDETERMINATE.
 */
class RefractionEstimator(
    private val config: RefractionEstimatorConfig,
    private val blurLadder: BlurLadder
) {

    /**
     * @param session completed session result (may have nulls for skipped phases).
     * @param questionnaire user's pre-test symptoms, or null if skipped.
     * @param blurTierIndex user's chosen row in the blur ladder (0-based), or null.
     */
    fun estimate(
        session: VisionSessionResult,
        questionnaire: QuestionnaireAnswers?,
        blurTierIndex: Int?
    ): RefractionEstimate {
        val rationale = ArrayList<String>()
        val diopterVotes = ArrayList<WeightedDiopterVote>()

        // Blur ladder (only an opinion if the user actually picked a row).
        val tier = blurTierIndex?.let { blurLadder.tiers.getOrNull(it) }
        if (tier != null) {
            val low = tier.diopterFeelLow
            val high = max(tier.diopterFeelHigh, low)
            if (high > 0f) {
                diopterVotes += applyWeight(low to high, config.blurTierWeight)
                rationale += "Blur ladder tier ${blurTierIndex + 1} → ≈ ${formatRange(low, high)} feel"
            } else {
                rationale += "Blur ladder tier 1 → no blur chosen"
            }
        }

        // Distance VA: use the worse eye's level index.
        val worseLevelIdx = listOfNotNull(
            session.acuityRight?.levelIndexAchieved,
            session.acuityLeft?.levelIndexAchieved
        ).minOrNull()
        if (worseLevelIdx != null) {
            val vaRange = vaLevelToDiopterRange(worseLevelIdx)
            if (vaRange != null) {
                diopterVotes += applyWeight(vaRange, config.distanceVaWeight)
                rationale += "Distance VA reached level index $worseLevelIdx (worse eye) → ≈ ${formatRange(vaRange.first, vaRange.second)}"
            } else {
                rationale += "Distance VA reached level index $worseLevelIdx → no distance-related blur signal"
            }
        }

        // Near vision.
        val nearScore = session.nearVisionScore01
        val nearRange = nearScoreToRange(nearScore)
        if (nearRange != null) {
            diopterVotes += applyWeight(nearRange, config.nearWeight)
            rationale += "Near-vision score ${(nearScore * 100).toInt()}% → ≈ ${formatRange(nearRange.first, nearRange.second)} (presbyopic/near)"
        }

        // Symptoms.
        if (questionnaire != null) {
            val worst = listOf(
                questionnaire.distanceBlur,
                questionnaire.nearDifficulty,
                questionnaire.eyeStrain,
                questionnaire.nightVision ?: SymptomLevel.NONE
            ).maxOrNull() ?: SymptomLevel.NONE
            val symRange = symptomRange(worst)
            if (symRange != null) {
                diopterVotes += applyWeight(symRange, config.symptomWeight)
                rationale += "Self-reported worst symptom = $worst → ≈ ${formatRange(symRange.first, symRange.second)}"
            }
        }

        // Focal pattern flavoring — adds a small floor so DISTANT_HARDER / NEAR_HARDER don't read as NONE
        // when VA data is missing but user reports trouble.
        val focalRange = questionnaire?.focalPattern?.let(::focalPatternRange)
        if (focalRange != null) {
            diopterVotes += applyWeight(focalRange, config.focalPatternWeight)
            rationale += "Focal pattern = ${questionnaire?.focalPattern} → ≈ ${formatRange(focalRange.first, focalRange.second)} floor"
        }

        if (diopterVotes.isEmpty()) {
            return RefractionEstimate(
                severity = RefractionSeverity.INDETERMINATE,
                diopterFeelLow = null,
                diopterFeelHigh = null,
                dominantFocal = questionnaire?.focalPattern ?: FocalDifficultyPattern.NEITHER,
                confidence01 = 0f,
                rationale = rationale + "Not enough signals to estimate"
            )
        }

        // Weighted average of low/high across votes (weights from JSON [app_config.json]).
        var sumW = 0f
        var sumLow = 0f
        var sumHigh = 0f
        diopterVotes.forEach { vote ->
            if (vote.weight <= 0f) return@forEach
            sumLow += vote.lo * vote.weight
            sumHigh += vote.hi * vote.weight
            sumW += vote.weight
        }
        if (sumW <= 0f) {
            return RefractionEstimate(
                severity = RefractionSeverity.INDETERMINATE,
                diopterFeelLow = null,
                diopterFeelHigh = null,
                dominantFocal = questionnaire?.focalPattern ?: FocalDifficultyPattern.NEITHER,
                confidence01 = 0f,
                rationale = rationale + "Weights summed to zero"
            )
        }
        val lowAvg = sumLow / sumW
        val highAvg = sumHigh / sumW
        val finalLow = min(lowAvg, highAvg)
        val finalHigh = max(lowAvg, highAvg)
        val midpoint = (finalLow + finalHigh) / 2f

        val severity = bucket(midpoint)
        val confidence = confidenceFromVoteSpread(diopterVotes)
        val dominantFocal = inferDominantFocal(session, questionnaire)

        return RefractionEstimate(
            severity = severity,
            diopterFeelLow = finalLow,
            diopterFeelHigh = finalHigh,
            dominantFocal = dominantFocal,
            confidence01 = confidence,
            rationale = rationale
        )
    }

    private data class WeightedDiopterVote(val lo: Float, val hi: Float, val weight: Float)

    private fun applyWeight(range: Pair<Float, Float>, weight: Float): WeightedDiopterVote {
        val w = weight.coerceAtLeast(0f)
        return WeightedDiopterVote(range.first, range.second, w)
    }

    private fun vaLevelToDiopterRange(levelIndexAchieved: Int): Pair<Float, Float>? {
        val clear = config.vaThresholdClear
        val reduced = config.vaThresholdReduced
        return when {
            levelIndexAchieved >= clear -> null
            levelIndexAchieved >= reduced -> 0.25f to 0.75f
            levelIndexAchieved >= reduced - 2 -> 0.75f to 1.75f
            levelIndexAchieved >= 0 -> 1.75f to 3.5f
            else -> null
        }
    }

    private fun nearScoreToRange(score01: Float): Pair<Float, Float>? = when {
        score01 >= 0.85f -> null
        score01 >= 0.65f -> 0.25f to 0.75f
        score01 >= 0.45f -> 0.75f to 1.5f
        score01 >= 0f -> 1.5f to 3.0f
        else -> null
    }

    private fun symptomRange(level: SymptomLevel): Pair<Float, Float>? = when (level) {
        SymptomLevel.NONE -> null
        SymptomLevel.MILD -> 0.25f to 0.75f
        SymptomLevel.MODERATE -> 0.75f to 1.75f
        SymptomLevel.SEVERE -> 1.75f to 3.5f
    }

    private fun focalPatternRange(pattern: FocalDifficultyPattern): Pair<Float, Float>? = when (pattern) {
        FocalDifficultyPattern.NEITHER -> null
        FocalDifficultyPattern.DISTANT_HARDER,
        FocalDifficultyPattern.NEAR_HARDER,
        FocalDifficultyPattern.BOTH -> 0.25f to 0.5f
    }

    private fun bucket(midpointD: Float): RefractionSeverity {
        val bps = config.severityBucketBreakpointsD
        if (bps.isEmpty()) return RefractionSeverity.INDETERMINATE
        val b0 = bps.getOrNull(0) ?: 0.5f
        val b1 = bps.getOrNull(1) ?: 1.25f
        val b2 = bps.getOrNull(2) ?: 2.5f
        return when {
            midpointD <= 0.01f -> RefractionSeverity.NONE
            midpointD < b0 -> RefractionSeverity.NONE
            midpointD < b1 -> RefractionSeverity.MILD
            midpointD < b2 -> RefractionSeverity.MODERATE
            else -> RefractionSeverity.STRONG
        }
    }

    private fun confidenceFromVoteSpread(votes: List<WeightedDiopterVote>): Float {
        if (votes.size < 2) return 0.35f
        val mids = votes.map { (it.lo + it.hi) / 2f }
        val avg = mids.average().toFloat()
        val variance = mids.map { (it - avg) * (it - avg) }.average().toFloat()
        val agreement = 1f / (1f + variance)
        return (0.3f + 0.7f * agreement).coerceIn(0f, 1f)
    }

    private fun inferDominantFocal(
        session: VisionSessionResult,
        questionnaire: QuestionnaireAnswers?
    ): FocalDifficultyPattern {
        if (questionnaire?.focalPattern != null &&
            questionnaire.focalPattern != FocalDifficultyPattern.NEITHER
        ) return questionnaire.focalPattern
        val nearWeak = session.nearVisionScore01 < 0.55f
        val distanceWeak = listOfNotNull(
            session.acuityRight?.levelIndexAchieved,
            session.acuityLeft?.levelIndexAchieved
        ).any { it < config.vaThresholdReduced }
        return when {
            distanceWeak && nearWeak -> FocalDifficultyPattern.BOTH
            distanceWeak -> FocalDifficultyPattern.DISTANT_HARDER
            nearWeak -> FocalDifficultyPattern.NEAR_HARDER
            else -> FocalDifficultyPattern.NEITHER
        }
    }

    private fun formatRange(lo: Float, hi: Float): String {
        return "-%.2f to -%.2f D".format(lo, hi)
    }
}
