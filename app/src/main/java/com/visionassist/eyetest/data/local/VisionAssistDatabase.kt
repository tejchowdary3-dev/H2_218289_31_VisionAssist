package com.visionassist.eyetest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TestResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VisionAssistDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao
}
