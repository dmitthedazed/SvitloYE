package com.occaecat.ztoeschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.occaecat.ztoeschedule.data.local.entity.ScheduleCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_cache WHERE cherga = :cherga AND pidcherga = :pidcherga")
    fun getSchedule(cherga: Int, pidcherga: Int): Flow<ScheduleCacheEntity?>

    @Query("SELECT * FROM schedule_cache WHERE cherga = :cherga AND pidcherga = :pidcherga")
    suspend fun getScheduleOnce(cherga: Int, pidcherga: Int): ScheduleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleCacheEntity)

    @Query("DELETE FROM schedule_cache")
    suspend fun deleteAll()
}