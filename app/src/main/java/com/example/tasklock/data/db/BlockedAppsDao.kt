package com.example.tasklock.data.dao

import androidx.room.*
import com.example.tasklock.data.model.BlockedAppEntity

@Dao
interface BlockedAppsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: BlockedAppEntity)

    @Delete
    suspend fun delete(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun remove(pkg: String)

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): BlockedAppEntity?

    @Query("UPDATE blocked_apps SET usedTodayMs = 0")
    suspend fun resetDailyUsage()

    @Query("UPDATE blocked_apps SET bonusMs = :bonus WHERE packageName = :pkg")
    suspend fun atualizarBonusApp(pkg: String, bonus: Long)
}

