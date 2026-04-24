package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.EnvironmentFlags
import com.visionassist.eyetest.domain.model.ReliabilityLevel

/**
 * Heuristic reliability for screening — not clinical validation.
 */
class ReliabilityEngine {

    fun evaluate(
        avgResponseTimeMs: Long,
        lineConsistency01: Float,
        totalRetries: Int,
        environment: EnvironmentFlags
    ): Pair<ReliabilityLevel, String> {
        var score = 100

        when {
            avgResponseTimeMs < TOO_FAST_MS -> score -= 25
            avgResponseTimeMs > TOO_SLOW_MS -> score -= 10
        }

        score -= ((1f - lineConsistency01) * 40f).toInt().coerceIn(0, 40)
        score -= (totalRetries * 12).coerceAtMost(36)

        if (!environment.lightingGood) score -= 15
        if (!environment.distanceGood) score -= 15

        val level = when {
            score >= 70 -> ReliabilityLevel.HIGH
            score >= 45 -> ReliabilityLevel.MEDIUM
            else -> ReliabilityLevel.LOW
        }

        val notes = buildString {
            if (avgResponseTimeMs < TOO_FAST_MS) append("Responses were very fast; take your time. ")
            if (lineConsistency01 < 0.55f) append("Answers varied between similar lines. ")
            if (totalRetries > 1) append("Several retries were needed. ")
            if (!environment.lightingGood) append("Lighting may have affected contrast. ")
            if (!environment.distanceGood) append("Viewing distance may have been inconsistent. ")
            if (isEmpty()) append("Signals look reasonable for a home screening.")
        }

        return level to notes.trim()
    }

    companion object {
        private const val TOO_FAST_MS = 450L
        private const val TOO_SLOW_MS = 25_000L
    }
}
