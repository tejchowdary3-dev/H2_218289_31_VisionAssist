package com.visionassist.eyetest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val sessionJson: String
)
