package com.visionassist.eyetest.data.ai

/** Minimal contract for the optional coaching/TTS helper. */
interface CoachClient {

    /** True when the client has a usable network target (url or key). */
    suspend fun isAvailable(): Boolean

    /**
     * Returns a single short coaching sentence (≤ 25 words) describing posture / lighting /
     * fellow-eye occlusion tips. Never returns a diagnosis.
     */
    suspend fun fetchCoachingTip(
        testedEyeLabel: String,
        levelLabel: String,
        onScreenInstruction: String
    ): Result<String>
}
