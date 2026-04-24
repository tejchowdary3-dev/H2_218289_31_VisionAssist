package com.visionassist.eyetest.domain.model

enum class TestedEye {
    RIGHT,
    LEFT
}

enum class ReliabilityLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class UserProfile(
    val age: Int,
    val screenTimeHoursPerDay: Int?
)

data class EnvironmentFlags(
    val lightingGood: Boolean,
    val distanceGood: Boolean
)

/**
 * Which working distance is harder for the patient (self-report). Guides blur-demo wording only.
 */
enum class FocalDifficultyPattern {
    /** Road signs, TV, faces across the room */
    DISTANT_HARDER,
    /** Phone, book, fine print */
    NEAR_HARDER,
    BOTH,
    /** No clear difference reported */
    NEITHER
}

data class QuestionnaireAnswers(
    val focalPattern: FocalDifficultyPattern,
    val distanceBlur: SymptomLevel,
    val nearDifficulty: SymptomLevel,
    val eyeStrain: SymptomLevel,
    val nightVision: SymptomLevel?
)

enum class SymptomLevel {
    NONE,
    MILD,
    MODERATE,
    SEVERE
}

data class AcuityResult(
    val eye: TestedEye,
    val snellenApproxLabel: String,
    val levelIndexAchieved: Int,
    val retriesUsed: Int
)

data class VisionSessionResult(
    val acuityRight: AcuityResult?,
    val acuityLeft: AcuityResult?,
    val nearVisionScore01: Float,
    val astigmatismFlag: Boolean,
    val contrastSensitivity01: Float,
    val reliability: ReliabilityLevel,
    val reliabilityNotes: String,
    val responseTimeAvgMs: Long,
    val lineConsistencyScore01: Float,
    /** Optional row index from illustrative blur ladder (not a diopter). */
    val blurIllustrationTierIndex: Int? = null
)

data class TestResultRecord(
    val id: String,
    val timestampMillis: Long,
    val session: VisionSessionResult
)

data class Clinic(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val availabilityNote: String
)

data class SnellenLevelConfig(
    val snellenLabel: String,
    val fontSizeSp: Float
)

data class VisionTestConfig(
    val letters: String,
    val lineLength: Int,
    val correctThreshold: Int,
    val maxRetriesPerLevel: Int,
    val snellenLevels: List<SnellenLevelConfig>,
    val nearOptionScores: List<Float>,
    val contrastRounds: Int,
    val rightEyeInstruction: String,
    val leftEyeInstruction: String,
    val initialHelper: String,
    val retryHelper: String,
    val switchEyeHelper: String,
    val nearPhaseInstruction: String,
    val astigPhaseInstruction: String,
    val astigHelper: String,
    val contrastInstruction: String
)

object VisionTestConfigDefaults {
    fun create(): VisionTestConfig = VisionTestConfig(
        letters = "CDEHKNOPRSTUVZ",
        lineLength = 5,
        correctThreshold = 4,
        maxRetriesPerLevel = 1,
        snellenLevels = listOf(
            SnellenLevelConfig("20/200", 34f),
            SnellenLevelConfig("20/100", 30f),
            SnellenLevelConfig("20/70", 26f),
            SnellenLevelConfig("20/50", 22f),
            SnellenLevelConfig("20/40", 19f),
            SnellenLevelConfig("20/30", 16f),
            SnellenLevelConfig("20/25", 14f),
            SnellenLevelConfig("20/20", 12f),
            SnellenLevelConfig("20/15", 10f)
        ),
        nearOptionScores = listOf(0.35f, 0.62f, 0.88f),
        contrastRounds = 3,
        rightEyeInstruction = "OD (right eye): occlude OS completely. Read the smallest row you can, left to right, without guessing.",
        leftEyeInstruction = "OS (left eye): occlude OD completely. Same task — smallest clear row, left to right.",
        initialHelper = "Record at least 4 of 5 letters correctly on this row to move to a smaller (harder) row — as in threshold acuity testing.",
        retryHelper = "Same row size — new letters. Take your time; call each letter clearly.",
        switchEyeHelper = "Cover the fellow eye fully before starting the next eye.",
        nearPhaseInstruction = "Near vision, both eyes at your usual reading distance: choose the smallest print that remains comfortable.",
        astigPhaseInstruction = "Radial pattern: with both eyes, do all spokes look equally sharp, or are some darker or smeared?",
        astigHelper = "Subjective screening only — not a diagnosis of astigmatism or any other condition.",
        contrastInstruction = "Which side shows the letter with better contrast and clarity?"
    )
}
