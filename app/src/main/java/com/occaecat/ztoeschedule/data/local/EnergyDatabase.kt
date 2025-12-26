package com.occaecat.ztoeschedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.occaecat.ztoeschedule.data.local.dao.ScheduleDao
import com.occaecat.ztoeschedule.data.local.entity.ScheduleCacheEntity

@Database(entities = [ScheduleCacheEntity::class], version = 2, exportSchema = false)
abstract class EnergyDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}