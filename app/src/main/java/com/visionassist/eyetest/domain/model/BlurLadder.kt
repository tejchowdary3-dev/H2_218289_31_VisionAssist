package com.visionassist.eyetest.domain.model

/**
 * One row in the illustrative blur ladder shown on the Blur Reference screen.
 *
 * Note: [diopterFeelLow]/[diopterFeelHigh] are *subjective illustrations only* and must not be
 * presented as a measured refractive error. A phone screen cannot produce optically faithful
 * defocus for a given observer.
 */
data class BlurLadderTier(
    val id: String,
    val blurDp: Float,
    val title: String,
    val diopterFeelLow: Float,
    val diopterFeelHigh: Float
)

data class BlurLadder(
    val sampleLine: String,
    val tiers: List<BlurLadderTier>
)

object BlurLadderDefaults {
    fun create(): BlurLadder = BlurLadder(
        sampleLine = "C D H K N O P R S T V Z",
        tiers = listOf(
            BlurLadderTier("tier_0", 0f,   "Row 1 — reference (minimal blur)", 0f, 0f),
            BlurLadderTier("tier_1", 1f,   "Row 2 — very mild (illustrative feel)", 0.25f, 0.5f),
            BlurLadderTier("tier_2", 2f,   "Row 3 — mild (illustrative feel)", 0.75f, 1f),
            BlurLadderTier("tier_3", 3.5f, "Row 4 — moderate blur illustration", 1f, 1.75f),
            BlurLadderTier("tier_4", 5.5f, "Row 5 — stronger blur illustration", 1.75f, 2.75f),
            BlurLadderTier("tier_5", 8f,   "Row 6 — strong blur illustration", 2.75f, 4.5f)
        )
    )
}
