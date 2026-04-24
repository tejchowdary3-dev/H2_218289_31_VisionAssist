package com.visionassist.eyetest.domain.model

/**
 * Runtime app configuration, loadable from JSON so keys/URLs aren't baked into bytecode.
 */
data class AppRuntimeConfig(
    val aiCoach: AiCoachConfig,
    val refractionEstimator: RefractionEstimatorConfig
)

enum class AiCoachMode {
    /** Choose automatically: proxy if URL set, else OpenAI-direct only in debug, else OFF. */
    AUTO,
    /** Call [AiCoachConfig.proxyBaseUrl] + [AiCoachConfig.proxyTipPath]. */
    PROXY,
    /** Send directly to OpenAI using [com.visionassist.eyetest.BuildConfig.OPENAI_API_KEY] (debug only). */
    OPENAI_DIRECT,
    /** Disable the AI coach button; user still has device TTS. */
    OFF
}

data class AiCoachConfig(
    val mode: AiCoachMode,
    val proxyBaseUrl: String,
    val proxyTipPath: String,
    val allowOpenAiDirectInDebug: Boolean
)

/**
 * Weights + thresholds used by [com.visionassist.eyetest.domain.engine.RefractionEstimator].
 *
 * [severityBucketBreakpointsD] is a list of ascending diopter breakpoints; the estimator maps
 * its final diopter-feel midpoint into one of 4 severity buckets using these breakpoints.
 */
data class RefractionEstimatorConfig(
    val blurTierWeight: Float,
    val distanceVaWeight: Float,
    val nearWeight: Float,
    val symptomWeight: Float,
    val focalPatternWeight: Float,
    /** Snellen level index >= this means distance VA looks "good enough". */
    val vaThresholdClear: Int,
    /** Snellen level index <= this means distance VA looks meaningfully reduced. */
    val vaThresholdReduced: Int,
    val severityBucketBreakpointsD: List<Float>
)

object AppRuntimeConfigDefaults {
    fun create(): AppRuntimeConfig = AppRuntimeConfig(
        aiCoach = AiCoachConfig(
            mode = AiCoachMode.AUTO,
            proxyBaseUrl = "",
            proxyTipPath = "/coach/tip",
            allowOpenAiDirectInDebug = true
        ),
        refractionEstimator = RefractionEstimatorConfig(
            blurTierWeight = 1.0f,
            distanceVaWeight = 1.0f,
            nearWeight = 0.5f,
            symptomWeight = 0.5f,
            focalPatternWeight = 0.5f,
            vaThresholdClear = 5,
            vaThresholdReduced = 3,
            severityBucketBreakpointsD = listOf(0.5f, 1.25f, 2.5f)
        )
    )
}
