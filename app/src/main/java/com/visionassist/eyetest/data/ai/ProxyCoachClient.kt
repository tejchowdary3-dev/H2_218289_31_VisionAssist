package com.visionassist.eyetest.data.ai

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Release-safe AI coach: forwards a small payload to the developer's backend proxy, which is
 * responsible for holding the model provider key and enforcing rate limits. The proxy must
 * respond with JSON `{"tip": "...one sentence..."}`.
 */
class ProxyCoachClient(
    private val baseUrl: String,
    private val tipPath: String,
    private val client: OkHttpClient = DefaultClient
) : CoachClient {

    override suspend fun isAvailable(): Boolean = baseUrl.isNotBlank()

    override suspend fun fetchCoachingTip(
        testedEyeLabel: String,
        levelLabel: String,
        onScreenInstruction: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("missing_proxy_base_url"))
        }
        runCatching {
            val body = JSONObject().apply {
                put("eye", testedEyeLabel)
                put("level_label", levelLabel)
                put("instruction", onScreenInstruction)
            }
            val url = baseUrl.trimEnd('/') + "/" + tipPath.trimStart('/')
            val request = Request.Builder()
                .url(url)
                .post(body.toString().toByteArray(Charsets.UTF_8).toRequestBody(jsonMedia))
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("HTTP ${response.code}: $raw")
                val tip = JSONObject(raw).optString("tip").trim()
                if (tip.isEmpty()) error("empty_tip")
                tip
            }
        }
    }

    companion object {
        private val jsonMedia = "application/json; charset=utf-8".toMediaType()
        val DefaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
