package com.visionassist.eyetest.data.repository

import android.content.Context
import com.visionassist.eyetest.BuildConfig
import com.visionassist.eyetest.data.ai.CoachClient
import com.visionassist.eyetest.data.ai.CoachClientProvider
import com.visionassist.eyetest.data.clinic.ClinicDirectory
import com.visionassist.eyetest.data.clinic.GooglePlacesClinicService
import com.visionassist.eyetest.data.clinic.OsmClinicService
import com.visionassist.eyetest.data.config.AppConfigLoader
import com.visionassist.eyetest.data.config.BlurLadderLoader
import com.visionassist.eyetest.data.config.VisionTestPlanLoader
import com.visionassist.eyetest.data.local.TestResultDao
import com.visionassist.eyetest.data.local.TestResultEntity
import com.visionassist.eyetest.data.local.VisionSessionResultJsonMapper
import com.visionassist.eyetest.domain.engine.RefractionEstimator
import com.visionassist.eyetest.domain.model.AppRuntimeConfig
import com.visionassist.eyetest.domain.model.AppRuntimeConfigDefaults
import com.visionassist.eyetest.domain.model.BlurLadder
import com.visionassist.eyetest.domain.model.BlurLadderDefaults
import com.visionassist.eyetest.domain.model.Clinic
import com.visionassist.eyetest.domain.model.EnvironmentFlags
import com.visionassist.eyetest.domain.model.QuestionnaireAnswers
import com.visionassist.eyetest.domain.model.RefractionEstimate
import com.visionassist.eyetest.domain.model.TestResultRecord
import com.visionassist.eyetest.domain.model.UserProfile
import com.visionassist.eyetest.domain.model.VisionSessionResult
import com.visionassist.eyetest.domain.model.VisionTestConfig
import com.visionassist.eyetest.domain.model.VisionTestConfigDefaults
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * App-level session state and persistence.
 *
 * Nothing about the test is baked into Kotlin code. Every piece of configuration follows the same
 * chain: remote HTTP (dev server) → bundled assets → in-code defaults as the absolute last resort.
 */
class VisionAssistRepository(
    appContext: Context,
    private val testResultDao: TestResultDao,
    private val planLoader: VisionTestPlanLoader = VisionTestPlanLoader(appContext),
    private val blurLadderLoader: BlurLadderLoader = BlurLadderLoader(appContext),
    private val appConfigLoader: AppConfigLoader = AppConfigLoader(appContext),
    private val googlePlacesClinicService: GooglePlacesClinicService = GooglePlacesClinicService(),
    private val osmClinicService: OsmClinicService = OsmClinicService()
) {

    var userProfile: UserProfile? = null
        private set

    var calibrationScale: Float = 1f
        private set

    var questionnaireAnswers: QuestionnaireAnswers? = null
        private set

    /**
     * User-selected row index from the illustrative blur ladder (0 = sharpest demo).
     * Not a refractive power measurement.
     */
    var blurIllustrationLevelIndex: Int? = null
        private set

    var environmentFlags: EnvironmentFlags = EnvironmentFlags(lightingGood = true, distanceGood = true)

    var lastSessionResult: VisionSessionResult? = null
        private set

    private var cachedTestConfig: VisionTestConfig = VisionTestConfigDefaults.create()
    private var cachedBlurLadder: BlurLadder = BlurLadderDefaults.create()
    private var cachedAppConfig: AppRuntimeConfig = AppRuntimeConfigDefaults.create()

    val history: Flow<List<TestResultRecord>> = testResultDao.observeRecentDesc().map { rows ->
        rows.map { entity ->
            TestResultRecord(
                id = entity.id,
                timestampMillis = entity.timestampMillis,
                session = VisionSessionResultJsonMapper.fromJson(entity.sessionJson)
            )
        }
    }

    fun setUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    fun setCalibrationScale(scale: Float) {
        calibrationScale = scale.coerceIn(0.6f, 1.6f)
    }

    fun setQuestionnaire(answers: QuestionnaireAnswers) {
        questionnaireAnswers = answers
        blurIllustrationLevelIndex = null
    }

    fun setBlurIllustrationLevelIndex(index: Int?) {
        blurIllustrationLevelIndex = index
    }

    fun setEnvironment(flags: EnvironmentFlags) {
        environmentFlags = flags
    }

    suspend fun commitSessionResult(result: VisionSessionResult) = withContext(Dispatchers.IO) {
        lastSessionResult = result
        val id = UUID.randomUUID().toString()
        val entity = TestResultEntity(
            id = id,
            timestampMillis = System.currentTimeMillis(),
            sessionJson = VisionSessionResultJsonMapper.toJson(result)
        )
        testResultDao.insert(entity)
    }

    fun currentVisionTestConfig(): VisionTestConfig = cachedTestConfig
    fun currentBlurLadder(): BlurLadder = cachedBlurLadder
    fun currentAppConfig(): AppRuntimeConfig = cachedAppConfig

    fun currentCoachClient(): CoachClient? = CoachClientProvider.select(cachedAppConfig.aiCoach)

    /**
     * Build a refraction estimate using current dynamic config + session inputs.
     * Uses the live blur ladder and estimator weights from [cachedAppConfig], so no hardcoded weights.
     */
    fun estimateRefraction(session: VisionSessionResult): RefractionEstimate {
        val estimator = RefractionEstimator(
            config = cachedAppConfig.refractionEstimator,
            blurLadder = cachedBlurLadder
        )
        return estimator.estimate(
            session = session,
            questionnaire = questionnaireAnswers,
            blurTierIndex = session.blurIllustrationTierIndex
        )
    }

    suspend fun fetchVisionTestConfig(): VisionTestConfig = withContext(Dispatchers.IO) {
        val remote = planLoader.loadRemoteOrNull()
        if (remote != null) {
            cachedTestConfig = remote
            return@withContext remote
        }
        val asset = planLoader.loadFromAssetsOrNull()
        if (asset != null) {
            cachedTestConfig = asset
            return@withContext asset
        }
        cachedTestConfig
    }

    suspend fun fetchBlurLadder(): BlurLadder = withContext(Dispatchers.IO) {
        blurLadderLoader.loadRemoteOrNull()?.let { cachedBlurLadder = it; return@withContext it }
        blurLadderLoader.loadFromAssetsOrNull()?.let { cachedBlurLadder = it; return@withContext it }
        cachedBlurLadder
    }

    suspend fun fetchAppConfig(): AppRuntimeConfig = withContext(Dispatchers.IO) {
        appConfigLoader.loadRemoteOrNull()?.let { cachedAppConfig = it; return@withContext it }
        appConfigLoader.loadFromAssetsOrNull()?.let { cachedAppConfig = it; return@withContext it }
        cachedAppConfig
    }

    fun mockClinicsNear(lat: Double, lng: Double): List<Clinic> = ClinicDirectory.mockClinicsNear(lat, lng)

    suspend fun nearbyHospitals(lat: Double, lng: Double): Result<List<Clinic>> {
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY.trim()
        if (apiKey.isNotBlank()) {
            val googleResult = googlePlacesClinicService.fetchNearbyHospitals(
                apiKey = apiKey,
                latitude = lat,
                longitude = lng
            )
            if (googleResult.isSuccess) return googleResult
        }
        return osmClinicService.fetchNearbyEyeHospitals(
            latitude = lat,
            longitude = lng
        )
    }

    /** Restores the latest screening after process death so the result screen is populated. */
    suspend fun hydrateLastSessionFromHistory() = withContext(Dispatchers.IO) {
        if (lastSessionResult != null) return@withContext
        val latest = testResultDao.loadLatest() ?: return@withContext
        lastSessionResult = VisionSessionResultJsonMapper.fromJson(latest.sessionJson)
    }
}
