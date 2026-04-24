package com.visionassist.eyetest.data.config

import android.content.Context
import com.visionassist.eyetest.domain.model.AiCoachConfig
import com.visionassist.eyetest.domain.model.AiCoachMode
import com.visionassist.eyetest.domain.model.AppRuntimeConfig
import com.visionassist.eyetest.domain.model.AppRuntimeConfigDefaults
import com.visionassist.eyetest.domain.model.RefractionEstimatorConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Remote → assets → defaults for app-level runtime config (AI coach endpoint selection,
 * refraction estimator weights).
 */
class AppConfigLoader(
    private val appContext: Context,
    private val httpClient: OkHttpClient = ConfigHttp.DefaultClient
) {

    suspend fun loadRemoteOrNull(): AppRuntimeConfig? = withContext(Dispatchers.IO) {
        val urls = listOf(REMOTE_URL_EMULATOR, REMOTE_URL_LOCALHOST)
        for (url in urls) {
            val raw = runCatching { httpGet(url) }.getOrNull() ?: continue
            val parsed = runCatching { parseAppRuntimeConfig(raw) }.getOrNull() ?: continue
            return@withContext parsed
        }
        null
    }

    suspend fun loadFromAssetsOrNull(): AppRuntimeConfig? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open(ASSET_NAME).use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    parseAppRuntimeConfig(reader.readText())
                }
            }
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("app-config HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val ASSET_NAME = "app_config.json"
        private const val REMOTE_URL_EMULATOR = "http://10.0.2.2:3000/app-config"
        private const val REMOTE_URL_LOCALHOST = "http://localhost:3000/app-config"

        fun parseAppRuntimeConfig(json: String): AppRuntimeConfig {
            val defaults = AppRuntimeConfigDefaults.create()
            val root = JSONObject(json)
            val aiObj = root.optJSONObject("ai_coach")
            val refObj = root.optJSONObject("refraction_estimator")

            val aiCoach = if (aiObj != null) {
                AiCoachConfig(
                    mode = runCatching {
                        AiCoachMode.valueOf(aiObj.optString("mode").uppercase())
                    }.getOrDefault(defaults.aiCoach.mode),
                    proxyBaseUrl = aiObj.optString("proxy_base_url", defaults.aiCoach.proxyBaseUrl).trim(),
                    proxyTipPath = aiObj.optString("proxy_tip_path", defaults.aiCoach.proxyTipPath)
                        .ifBlank { defaults.aiCoach.proxyTipPath },
                    allowOpenAiDirectInDebug = aiObj.optBoolean(
                        "allow_openai_direct_in_debug",
                        defaults.aiCoach.allowOpenAiDirectInDebug
                    )
                )
            } else defaults.aiCoach

            val refEst = if (refObj != null) {
                RefractionEstimatorConfig(
                    blurTierWeight = refObj.optDouble("blur_tier_weight", defaults.refractionEstimator.blurTierWeight.toDouble()).toFloat(),
                    distanceVaWeight = refObj.optDouble("distance_va_weight", defaults.refractionEstimator.distanceVaWeight.toDouble()).toFloat(),
                    nearWeight = refObj.optDouble("near_weight", defaults.refractionEstimator.nearWeight.toDouble()).toFloat(),
                    symptomWeight = refObj.optDouble("symptom_weight", defaults.refractionEstimator.symptomWeight.toDouble()).toFloat(),
                    focalPatternWeight = refObj.optDouble("focal_pattern_weight", defaults.refractionEstimator.focalPatternWeight.toDouble()).toFloat(),
                    vaThresholdClear = refObj.optInt("va_threshold_clear", defaults.refractionEstimator.vaThresholdClear).coerceAtLeast(0),
                    vaThresholdReduced = refObj.optInt("va_threshold_reduced", defaults.refractionEstimator.vaThresholdReduced).coerceAtLeast(0),
                    severityBucketBreakpointsD = refObj.optJSONArray("severity_bucket_breakpoints_d")
                        ?.let { arr ->
                            (0 until arr.length())
                                .map { arr.optDouble(it, Double.NaN) }
                                .filter { !it.isNaN() && it > 0 }
                                .map { it.toFloat() }
                                .sorted()
                        }
                        ?.takeIf { it.isNotEmpty() }
                        ?: defaults.refractionEstimator.severityBucketBreakpointsD
                )
            } else defaults.refractionEstimator

            return AppRuntimeConfig(aiCoach = aiCoach, refractionEstimator = refEst)
        }
    }
}
