package com.clockin.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clockin.app.domain.ClockRecord

@Entity(tableName = "clock_records")
data class ClockRecordEntity(
    @PrimaryKey val shiftDate: String,
    val clockInTime: Long?,
    val clockOutTime: Long?,
)

fun ClockRecordEntity.toDomain() = ClockRecord(
    shiftDate = shiftDate,
    clockInTime = clockInTime,
    clockOutTime = clockOutTime,
)

fun ClockRecord.toEntity() = ClockRecordEntity(
    shiftDate = shiftDate,
    clockInTime = clockInTime,
    clockOutTime = clockOutTime,
)
