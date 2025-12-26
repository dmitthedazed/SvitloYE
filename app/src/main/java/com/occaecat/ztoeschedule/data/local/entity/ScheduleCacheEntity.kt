package com.occaecat.ztoeschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_cache", primaryKeys = ["cherga", "pidcherga"])
data class ScheduleCacheEntity(
    val cherga: Int,
    val pidcherga: Int,
    val scheduleJson: String, // List<Schedule> serialized
    val messagesJson: String, // List<ScheduleMessagePart> serialized
    val lastUpdated: Long
)