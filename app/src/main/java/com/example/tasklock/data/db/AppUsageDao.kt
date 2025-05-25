package com.example.tasklock.data.db

import androidx.room.*
import com.example.tasklock.data.model.AppUsageEntity

@Dao
interface AppUsageDao {

    @Query("""
        SELECT packageName, MAX(lastUsed) as lastUsed, SUM(totalTimeMs) as totalTimeMs
        FROM app_usage
        GROUP BY packageName
        ORDER BY totalTimeMs DESC
    """)
    fun getAllAgrupado(): List<AppUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppUsageEntity)

    @Transaction
    suspend fun insertOrUpdate(pkg: String, timestamp: Long, duration: Long) {
        val existing = getByPackage(pkg)
        if (existing != null) {
            insert(existing.copy(
                totalTimeMs = existing.totalTimeMs + duration,
                lastUsed = timestamp
            ))
        } else {
            insert(AppUsageEntity(pkg, lastUsed = timestamp, totalTimeMs = duration))
        }
    }

    @Query("SELECT * FROM app_usage WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppUsageEntity?

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()
}
