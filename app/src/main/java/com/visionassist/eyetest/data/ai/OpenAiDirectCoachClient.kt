package com.visionassist.eyetest.data.ai

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only: sends requests straight to OpenAI using the build-config key. Never use this path
 * for release — keys in an APK are trivially extracted. See [ProxyCoachClient] for release.
 */
class OpenAiDirectCoachClient(
    private val apiKey: String,
    private val client: OkHttpClient = DefaultClient
) : CoachClient {

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchCoachingTip(
        testedEyeLabel: String,
        levelLabel: String,
        onScreenInstruction: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("missing_api_key"))
        }
        runCatching {
            val system = (
                "You are a friendly vision-screening coach for a self-administered app test. " +
                    "Give exactly one short sentence (max 25 words). " +
                    "Encourage good posture, lighting, covering the non-tested eye, and correct viewing distance. " +
                    "Do not diagnose, do not name conditions, do not claim medical accuracy."
                )
            val user = (
                "Eye under test: $testedEyeLabel. Approximate row label: $levelLabel. " +
                    "On-screen instruction: $onScreenInstruction"
                )
            val body = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("temperature", 0.6)
                put("max_tokens", 80)
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", system))
                        put(JSONObject().put("role", "user").put("content", user))
                    }
                )
            }
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            val request = Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(bytes.toRequestBody(jsonMedia))
                .build()
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: $responseText")
                }
                val root = JSONObject(responseText)
                val choices = root.getJSONArray("choices")
                val content = choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
                if (content.isEmpty()) error("empty_content")
                content
            }
        }
    }

    companion object {
        private const val CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        val DefaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
