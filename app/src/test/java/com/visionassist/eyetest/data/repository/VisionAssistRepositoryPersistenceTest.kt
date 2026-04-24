package com.visionassist.eyetest.data.repository

import androidx.room.Room
import com.visionassist.eyetest.data.local.VisionAssistDatabase
import com.visionassist.eyetest.data.local.VisionSessionResultJsonMapper
import com.visionassist.eyetest.domain.model.AcuityResult
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionSessionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class VisionAssistRepositoryPersistenceTest {

    @Test
    fun commitSessionResult_canBeLoadedFromDao() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, VisionAssistDatabase::class.java).build()
        val dao = db.testResultDao()
        val repo = VisionAssistRepository(app, dao)
        val session = VisionSessionResult(
            acuityRight = AcuityResult(TestedEye.RIGHT, "20/50", 3, 0),
            acuityLeft = AcuityResult(TestedEye.LEFT, "20/50", 3, 0),
            nearVisionScore01 = 0.5f,
            astigmatismFlag = false,
            contrastSensitivity01 = 0.5f,
            reliability = ReliabilityLevel.HIGH,
            reliabilityNotes = "OK",
            responseTimeAvgMs = 2000L,
            lineConsistencyScore01 = 0.8f
        )
        repo.commitSessionResult(session)
        val entity = dao.loadLatest()
        assertNotNull(entity)
        val loaded = VisionSessionResultJsonMapper.fromJson(entity!!.sessionJson)
        assertEquals("20/50", loaded.acuityRight?.snellenApproxLabel)
        assertEquals("20/50", loaded.acuityLeft?.snellenApproxLabel)
    }
}
