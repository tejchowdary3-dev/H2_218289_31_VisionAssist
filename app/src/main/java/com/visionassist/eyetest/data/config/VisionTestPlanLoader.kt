package com.visionassist.eyetest.data.config

import android.content.Context
import com.visionassist.eyetest.domain.model.SnellenLevelConfig
import com.visionassist.eyetest.domain.model.VisionTestConfig
import com.visionassist.eyetest.domain.model.VisionTestConfigDefaults
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads test plan JSON from optional dev server (cleartext allowed in debug via network security config),
 * then bundled assets, then in-code defaults.
 */
class VisionTestPlanLoader(
    private val appContext: Context,
    private val httpClient: OkHttpClient = DefaultHttpClient
) {

    suspend fun loadRemoteOrNull(): VisionTestConfig? = withContext(Dispatchers.IO) {
        val urls = listOf(TEST_PLAN_URL_ANDROID_EMULATOR, TEST_PLAN_URL_LOCALHOST)
        for (endpoint in urls) {
            val json = runCatching { httpGet(endpoint) }.getOrNull() ?: continue
            runCatching { return@withContext parseVisionTestConfig(json) }
        }
        null
    }

    suspend fun loadFromAssetsOrNull(): VisionTestConfig? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open(TEST_PLAN_ASSET).use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    parseVisionTestConfig(reader.readText())
                }
            }
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("test-plan HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val TEST_PLAN_ASSET = "vision_test_plan.json"
        private const val TEST_PLAN_URL_ANDROID_EMULATOR = "http://10.0.2.2:3000/test-plan"
        private const val TEST_PLAN_URL_LOCALHOST = "http://localhost:3000/test-plan"

        private val DefaultHttpClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        fun parseVisionTestConfig(json: String): VisionTestConfig {
            val defaults = VisionTestConfigDefaults.create()
            val root = JSONObject(json)

            val levels = root.optJSONArray("snellen_levels")
                ?.toSnellenLevels()
                ?.takeIf { it.isNotEmpty() }
                ?: defaults.snellenLevels

            val nearScores = root.optJSONArray("near_option_scores")
                ?.toFloatList()
                ?.takeIf { it.isNotEmpty() }
                ?: defaults.nearOptionScores

            val instructions = root.optJSONObject("instructions")

            val lineLength = root.optInt("line_length", defaults.lineLength).coerceIn(3, 8)
            val threshold = root.optInt("correct_threshold", defaults.correctThreshold).coerceIn(1, lineLength)
            return VisionTestConfig(
                letters = root.optString("letters", defaults.letters).ifBlank { defaults.letters },
                lineLength = lineLength,
                correctThreshold = threshold,
                maxRetriesPerLevel = root.optInt("max_retries_per_level", defaults.maxRetriesPerLevel).coerceAtLeast(0),
                snellenLevels = levels,
                nearOptionScores = nearScores,
                contrastRounds = root.optInt("contrast_rounds", defaults.contrastRounds).coerceAtLeast(1),
                rightEyeInstruction = instructions?.optString("right_eye", defaults.rightEyeInstruction)
                    ?: defaults.rightEyeInstruction,
                leftEyeInstruction = instructions?.optString("left_eye", defaults.leftEyeInstruction)
                    ?: defaults.leftEyeInstruction,
                initialHelper = instructions?.optString("initial_helper", defaults.initialHelper)
                    ?: defaults.initialHelper,
                retryHelper = instructions?.optString("retry_helper", defaults.retryHelper)
                    ?: defaults.retryHelper,
                switchEyeHelper = instructions?.optString("switch_eye_helper", defaults.switchEyeHelper)
                    ?: defaults.switchEyeHelper,
                nearPhaseInstruction = instructions?.optString("near_phase", defaults.nearPhaseInstruction)
                    ?: defaults.nearPhaseInstruction,
                astigPhaseInstruction = instructions?.optString("astig_phase", defaults.astigPhaseInstruction)
                    ?: defaults.astigPhaseInstruction,
                astigHelper = instructions?.optString("astig_helper", defaults.astigHelper)
                    ?: defaults.astigHelper,
                contrastInstruction = instructions?.optString("contrast_phase", defaults.contrastInstruction)
                    ?: defaults.contrastInstruction
            )
        }

        private fun JSONArray.toSnellenLevels(): List<SnellenLevelConfig> {
            val output = ArrayList<SnellenLevelConfig>(length())
            for (i in 0 until length()) {
                val obj = optJSONObject(i) ?: continue
                val label = obj.optString("label").ifBlank { continue }
                val fontSp = obj.optDouble("font_sp", -1.0).toFloat()
                if (fontSp <= 0f) continue
                output.add(SnellenLevelConfig(snellenLabel = label, fontSizeSp = fontSp))
            }
            return output
        }

        private fun JSONArray.toFloatList(): List<Float> {
            val output = ArrayList<Float>(length())
            for (i in 0 until length()) {
                val value = optDouble(i, Double.NaN)
                if (value.isNaN()) continue
                output.add(value.toFloat().coerceIn(0f, 1f))
            }
            return output
        }
    }
}
