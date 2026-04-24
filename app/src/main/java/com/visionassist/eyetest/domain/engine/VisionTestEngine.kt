package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionTestConfig
import kotlin.random.Random

/**
 * Snellen-style screening: random letters per line, adaptive step size.
 * Not a clinical device; results are indicative only.
 */
class VisionTestEngine(
    private val config: VisionTestConfig,
    private val random: Random = Random.Default
) {

    val snellenLevels: List<SnellenLevel>
        get() = config.snellenLevels.mapIndexed { index, level ->
            SnellenLevel(index, level.snellenLabel, level.fontSizeSp)
        }

    fun randomLine(length: Int = config.lineLength): String {
        val letters = config.letters.ifBlank { DEFAULT_LETTERS }
        return buildString(length) {
            repeat(length) {
                append(letters.random(random))
            }
        }
    }

    fun linePassed(correctCount: Int, total: Int = config.lineLength): Boolean {
        return correctCount >= config.correctThreshold
    }

    fun fontSizeSpForLevel(levelIndex: Int): Float {
        val idx = levelIndex.coerceIn(0, snellenLevels.lastIndex)
        return snellenLevels[idx].fontSizeSp
    }

    fun labelForLevel(levelIndex: Int): String {
        val idx = levelIndex.coerceIn(0, snellenLevels.lastIndex)
        return snellenLevels[idx].snellenLabel
    }

    fun instructionForEye(eye: TestedEye): String {
        return when (eye) {
            TestedEye.RIGHT -> config.rightEyeInstruction
            TestedEye.LEFT -> config.leftEyeInstruction
        }
    }

    data class SnellenLevel(
        val index: Int,
        val snellenLabel: String,
        val fontSizeSp: Float
    )

    companion object {
        const val DEFAULT_LETTERS = "CDEHKNOPRSTUVZ"
    }
}
