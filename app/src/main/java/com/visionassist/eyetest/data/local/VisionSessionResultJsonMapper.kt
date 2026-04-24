package com.visionassist.eyetest.data.local

import com.visionassist.eyetest.domain.model.AcuityResult
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionSessionResult
import org.json.JSONObject

object VisionSessionResultJsonMapper {

    fun toJson(session: VisionSessionResult): String {
        val o = JSONObject()
        session.acuityRight?.let { o.put("acuityRight", acuityToJson(it)) }
        session.acuityLeft?.let { o.put("acuityLeft", acuityToJson(it)) }
        o.put("nearVisionScore01", session.nearVisionScore01.toDouble())
        o.put("astigmatismFlag", session.astigmatismFlag)
        o.put("contrastSensitivity01", session.contrastSensitivity01.toDouble())
        o.put("reliability", session.reliability.name)
        o.put("reliabilityNotes", session.reliabilityNotes)
        o.put("responseTimeAvgMs", session.responseTimeAvgMs)
        o.put("lineConsistencyScore01", session.lineConsistencyScore01.toDouble())
        session.blurIllustrationTierIndex?.let { o.put("blurIllustrationTierIndex", it) }
        return o.toString()
    }

    fun fromJson(json: String): VisionSessionResult {
        val o = JSONObject(json)
        return VisionSessionResult(
            acuityRight = o.optJSONObject("acuityRight")?.let { acuityFromJson(it) },
            acuityLeft = o.optJSONObject("acuityLeft")?.let { acuityFromJson(it) },
            nearVisionScore01 = o.optDouble("nearVisionScore01", 0.5).toFloat(),
            astigmatismFlag = o.optBoolean("astigmatismFlag"),
            contrastSensitivity01 = o.optDouble("contrastSensitivity01", 0.5).toFloat(),
            reliability = runCatching { ReliabilityLevel.valueOf(o.getString("reliability")) }
                .getOrElse { ReliabilityLevel.MEDIUM },
            reliabilityNotes = o.optString("reliabilityNotes", ""),
            responseTimeAvgMs = o.optLong("responseTimeAvgMs", 0L),
            lineConsistencyScore01 = o.optDouble("lineConsistencyScore01", 0.75).toFloat(),
            blurIllustrationTierIndex = when {
                !o.has("blurIllustrationTierIndex") || o.isNull("blurIllustrationTierIndex") -> null
                else -> o.getInt("blurIllustrationTierIndex")
            }
        )
    }

    private fun acuityToJson(a: AcuityResult): JSONObject = JSONObject().apply {
        put("eye", a.eye.name)
        put("snellenApproxLabel", a.snellenApproxLabel)
        put("levelIndexAchieved", a.levelIndexAchieved)
        put("retriesUsed", a.retriesUsed)
    }

    private fun acuityFromJson(o: JSONObject): AcuityResult = AcuityResult(
        eye = runCatching { TestedEye.valueOf(o.getString("eye")) }.getOrDefault(TestedEye.RIGHT),
        snellenApproxLabel = o.optString("snellenApproxLabel", ""),
        levelIndexAchieved = o.optInt("levelIndexAchieved", 0),
        retriesUsed = o.optInt("retriesUsed", 0)
    )
}
