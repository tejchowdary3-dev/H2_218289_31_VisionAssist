package com.visionassist.eyetest.data.config

import android.content.Context
import com.visionassist.eyetest.domain.model.BlurLadder
import com.visionassist.eyetest.domain.model.BlurLadderDefaults
import com.visionassist.eyetest.domain.model.BlurLadderTier
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Remote → assets → defaults chain for the illustrative blur ladder. The ladder is data-driven
 * (count, blur amounts, titles, approx diopter-feel range) so nothing is hardcoded in Kotlin.
 */
class BlurLadderLoader(
    private val appContext: Context,
    private val httpClient: OkHttpClient = ConfigHttp.DefaultClient
) {

    suspend fun loadRemoteOrNull(): BlurLadder? = withContext(Dispatchers.IO) {
        val urls = listOf(REMOTE_URL_EMULATOR, REMOTE_URL_LOCALHOST)
        for (url in urls) {
            val raw = runCatching { httpGet(url) }.getOrNull() ?: continue
            val parsed = runCatching { parseBlurLadder(raw) }.getOrNull() ?: continue
            return@withContext parsed
        }
        null
    }

    suspend fun loadFromAssetsOrNull(): BlurLadder? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.assets.open(ASSET_NAME).use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    parseBlurLadder(reader.readText())
                }
            }
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("blur-ladder HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        private const val ASSET_NAME = "blur_ladder.json"
        private const val REMOTE_URL_EMULATOR = "http://10.0.2.2:3000/blur-ladder"
        private const val REMOTE_URL_LOCALHOST = "http://localhost:3000/blur-ladder"

        fun parseBlurLadder(json: String): BlurLadder {
            val defaults = BlurLadderDefaults.create()
            val root = JSONObject(json)
            val sample = root.optString("sample_line").ifBlank { defaults.sampleLine }
            val tiersJson = root.optJSONArray("tiers") ?: return defaults
            val tiers = ArrayList<BlurLadderTier>(tiersJson.length())
            for (i in 0 until tiersJson.length()) {
                val obj = tiersJson.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifBlank { "tier_$i" }
                val blurDp = obj.optDouble("blur_dp", -1.0).toFloat()
                if (blurDp < 0f) continue
                val title = obj.optString("title").ifBlank { "Row ${i + 1}" }
                val low = obj.optDouble("diopter_feel_low", 0.0).toFloat().coerceAtLeast(0f)
                val high = obj.optDouble("diopter_feel_high", low.toDouble()).toFloat().coerceAtLeast(low)
                tiers.add(BlurLadderTier(id, blurDp, title, low, high))
            }
            val safeTiers = tiers.takeIf { it.isNotEmpty() } ?: defaults.tiers
            return BlurLadder(sampleLine = sample, tiers = safeTiers)
        }
    }
}
