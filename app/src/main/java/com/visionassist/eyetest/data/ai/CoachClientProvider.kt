package com.visionassist.eyetest.data.ai

import com.visionassist.eyetest.BuildConfig
import com.visionassist.eyetest.domain.model.AiCoachConfig
import com.visionassist.eyetest.domain.model.AiCoachMode

/**
 * Picks the right coach client based on dynamic app config:
 *   - AUTO: use proxy if configured; else OpenAI-direct (debug only if allowed); else disabled.
 *   - PROXY: always proxy, even if URL is blank (errors surface to user).
 *   - OPENAI_DIRECT: direct to OpenAI using build-config key.
 *   - OFF: null, UI hides the button.
 */
object CoachClientProvider {

    fun select(config: AiCoachConfig, isDebugBuild: Boolean = BuildConfig.DEBUG): CoachClient? {
        return when (config.mode) {
            AiCoachMode.OFF -> null
            AiCoachMode.PROXY -> ProxyCoachClient(config.proxyBaseUrl, config.proxyTipPath)
            AiCoachMode.OPENAI_DIRECT -> if (isDebugBuild) {
                OpenAiDirectCoachClient(BuildConfig.OPENAI_API_KEY)
            } else null
            AiCoachMode.AUTO -> {
                if (config.proxyBaseUrl.isNotBlank()) {
                    ProxyCoachClient(config.proxyBaseUrl, config.proxyTipPath)
                } else if (isDebugBuild &&
                    config.allowOpenAiDirectInDebug &&
                    BuildConfig.OPENAI_API_KEY.isNotBlank()
                ) {
                    OpenAiDirectCoachClient(BuildConfig.OPENAI_API_KEY)
                } else null
            }
        }
    }
}
