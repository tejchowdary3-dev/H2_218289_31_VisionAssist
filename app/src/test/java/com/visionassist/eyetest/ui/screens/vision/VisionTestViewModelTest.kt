package com.visionassist.eyetest.ui.screens.vision

import androidx.room.Room
import com.visionassist.eyetest.data.local.VisionAssistDatabase
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class VisionTestViewModelTest {

    @Test
    fun acuity_correctSubmit_advancesApproxSnellenRow() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, VisionAssistDatabase::class.java).build()
        val repo = VisionAssistRepository(app, db.testResultDao())
        val vm = VisionTestViewModel(repo)
        delay(1500)
        assertEquals(VisionTestPhase.ACUITY, vm.uiState.value.internalPhase)
        assertEquals("20/200", vm.uiState.value.levelLabel)
        val line = vm.uiState.value.line
        line.forEach { ch -> vm.appendLetter(ch) }
        vm.submitLine()
        delay(200)
        assertEquals("20/100", vm.uiState.value.levelLabel)
    }
}
