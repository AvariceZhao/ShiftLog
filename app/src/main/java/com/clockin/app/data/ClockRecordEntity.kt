package com.clockin.app.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.DaySchedule

@Entity(tableName = "clock_records")
data class ClockRecordEntity(
    @PrimaryKey val shiftDate: String,
    val clockInTime: Long?,
    val clockOutTime: Long?,
    @ColumnInfo(defaultValue = "0") val isLeave: Boolean = false,
    @ColumnInfo(defaultValue = "'AUTO'") val daySchedule: String = DaySchedule.AUTO.name,
    @ColumnInfo(defaultValue = "0") val courseMinutes: Int = 0,
)

fun ClockRecordEntity.toDomain() = ClockRecord(
    shiftDate = shiftDate,
    clockInTime = clockInTime,
    clockOutTime = clockOutTime,
    isLeave = isLeave,
    daySchedule = runCatching { DaySchedule.valueOf(daySchedule) }.getOrDefault(DaySchedule.AUTO),
    courseMinutes = courseMinutes,
)

fun ClockRecord.toEntity() = ClockRecordEntity(
    shiftDate = shiftDate,
    clockInTime = clockInTime,
    clockOutTime = clockOutTime,
    isLeave = isLeave,
    daySchedule = daySchedule.name,
    courseMinutes = courseMinutes,
)
