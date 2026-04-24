package com.visionassist.eyetest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {

    @Query("SELECT * FROM test_results ORDER BY timestampMillis DESC LIMIT 200")
    fun observeRecentDesc(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun loadLatest(): TestResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TestResultEntity)
}
