package com.visionassist.eyetest

import android.app.Application
import androidx.room.Room
import com.visionassist.eyetest.data.local.VisionAssistDatabase
import com.visionassist.eyetest.data.repository.VisionAssistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VisionAssistApp : Application() {

    private val appIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: VisionAssistDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            VisionAssistDatabase::class.java,
            "vision_assist.db"
        ).build()
    }

    val repository: VisionAssistRepository by lazy {
        VisionAssistRepository(
            applicationContext,
            database.testResultDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        appIoScope.launch {
            repository.hydrateLastSessionFromHistory()
            // Warm up runtime configs so downstream screens see the latest remote / asset values
            // before the user enters them. Each call already falls back to defaults on failure.
            runCatching { repository.fetchAppConfig() }
            runCatching { repository.fetchBlurLadder() }
            runCatching { repository.fetchVisionTestConfig() }
        }
    }
}
