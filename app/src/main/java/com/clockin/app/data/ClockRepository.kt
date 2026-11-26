package com.clockin.app.data

import com.clockin.app.domain.AppBackup
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.AppTimeTicker
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.DaySchedule
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.PunchResult
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.toLocalDate
import com.clockin.app.holiday.HolidayCalendarRepository
import com.clockin.app.domain.toShiftDateString
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalDate

class ClockRepository(
    private val dao: ClockRecordDao,
    private val settingsRepository: SettingsRepository,
    private val db: AppDatabase,
    private val holidayRepository: HolidayCalendarRepository,
    private val onRecordsChanged: () -> Unit = {},
    private val onSettingsChanged: () -> Unit = {},
) {
    val settings: Flow<AppSettings> = combine(
        settingsRepository.settings,
        holidayRepository.calendars,
    ) { settings, calendars -> settings.copy(holidayCalendars = calendars) }
        .distinctUntilChanged()

    val holidayCalendarStatus = holidayRepository.status

    suspend fun refreshHolidayCalendar(cycle: PayCycle? = null, force: Boolean = false) =
        holidayRepository.refresh(
            requestedYears = buildSet {
                getAllRecords().forEach { record ->
                    runCatching { record.shiftDate.toLocalDate().year }.getOrNull()?.let { year ->
                        add(year)
                        add(year + 1)
                    }
                }
                cycle?.let {
                    addAll(it.start.year..it.end.year + 1)
                }
            },
            force = force,
            today = LocalDate.now(),
        )

    fun observeActiveShift(): Flow<Pair<String, ClockRecord?>> =
        combine(
            settings,
            dao.observeLatestOpenShift(),
            AppTimeTicker.localDateFlow(),
            AppTimeTicker.minuteTickFlow(),
        ) { currentSettings, openEntity, _, _ ->
            val open = openEntity?.toDomain()
            ShiftCalculator.resolveActiveShiftDate(LocalDateTime.now(), currentSettings, open)
        }.flatMapLatest { shiftDate ->
            dao.observeByShiftDate(shiftDate).map { entity ->
                shiftDate to entity?.toDomain()
            }
        }

    fun observeCycleRecords(cycle: PayCycle): Flow<List<ClockRecord>> =
        dao.observeInRange(
            cycle.start.toShiftDateString(),
            cycle.end.toShiftDateString(),
        ).map { list -> list.map { it.toDomain() } }

    suspend fun performClockIn(): PunchResult {
        val settings = settings.first()
        val now = LocalDateTime.now()
        val open = dao.findLatestOpenShift()?.toDomain()
        if (ShiftCalculator.openShiftBlocksClockIn(now, settings, open)) {
            return PunchResult.AlreadyClockedIn
        }
        val shiftDate = ShiftCalculator.currentShiftDate(now, settings)
        val existing = dao.getByShiftDate(shiftDate)?.toDomain()
        if (existing?.clockInTime != null) return PunchResult.AlreadyClockedIn
        dao.upsert(
            ClockRecord(
                shiftDate = shiftDate,
                clockInTime = System.currentTimeMillis(),
                clockOutTime = existing?.clockOutTime,
                isLeave = false,
                daySchedule = existing?.daySchedule ?: DaySchedule.AUTO,
                courseMinutes = existing?.courseMinutes ?: 0,
            ).toEntity(),
        )
        onRecordsChanged()
        return PunchResult.Success
    }

    suspend fun performClockOut(): PunchResult {
        val settings = settings.first()
        val nowMs = System.currentTimeMillis()
        val open = dao.findLatestOpenShift()?.toDomain()
        val shiftDate = ShiftCalculator.resolveActiveShiftDate(
            LocalDateTime.now(),
            settings,
            open,
        )
        val existing = dao.getByShiftDate(shiftDate)?.toDomain()
        if (existing?.clockInTime == null) return PunchResult.NotClockedInYet
        dao.upsert(
            ClockRecord(
                shiftDate = shiftDate,
                clockInTime = existing.clockInTime,
                clockOutTime = nowMs,
                isLeave = false,
                daySchedule = existing.daySchedule,
                courseMinutes = existing.courseMinutes,
            ).toEntity(),
        )
        onRecordsChanged()
        return PunchResult.Success
    }

    suspend fun saveRecord(record: ClockRecord) {
        dao.upsert(record.toEntity())
        onRecordsChanged()
    }

    suspend fun deleteRecord(shiftDate: String) {
        dao.delete(shiftDate)
        onRecordsChanged()
    }

    suspend fun updateSettings(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
        onSettingsChanged()
    }

    suspend fun getCycleRecords(cycle: PayCycle): List<ClockRecord> =
        observeCycleRecords(cycle).first()

    suspend fun loadActiveShift(): Pair<String, ClockRecord?> {
        val settings = settings.first()
        val now = LocalDateTime.now()
        val open = dao.findLatestOpenShift()?.toDomain()
        val shiftDate = ShiftCalculator.resolveActiveShiftDate(now, settings, open)
        val record = dao.getByShiftDate(shiftDate)?.toDomain()
        return shiftDate to record
    }

    suspend fun getAllRecords(): List<ClockRecord> =
        dao.getAll().map { it.toDomain() }

    suspend fun mergeRecords(records: List<ClockRecord>) {
        if (records.isEmpty()) return
        dao.upsertAll(records.map { it.toEntity() })
        onRecordsChanged()
    }

    suspend fun restoreBackup(backup: AppBackup, replaceExisting: Boolean) {
        db.withTransaction {
            if (replaceExisting) {
                dao.deleteAll()
                settingsRepository.updateSettings(backup.settings)
            }
            dao.upsertAll(backup.records.map { it.toEntity() })
        }
        onRecordsChanged()
        if (replaceExisting) {
            onSettingsChanged()
        }
    }
}
