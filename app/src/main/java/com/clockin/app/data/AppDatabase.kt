package com.clockin.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClockRecordEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clockRecordDao(): ClockRecordDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clock_records ADD COLUMN isLeave INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 fresh installs and upgrades had different isLeave SQL defaults.
                db.execSQL(
                    "CREATE TABLE clock_records_new (shiftDate TEXT NOT NULL PRIMARY KEY, " +
                        "clockInTime INTEGER, clockOutTime INTEGER, " +
                        "isLeave INTEGER NOT NULL DEFAULT 0, daySchedule TEXT NOT NULL DEFAULT 'AUTO')",
                )
                db.execSQL(
                    "INSERT INTO clock_records_new (shiftDate, clockInTime, clockOutTime, isLeave) " +
                        "SELECT shiftDate, clockInTime, clockOutTime, isLeave FROM clock_records",
                )
                db.execSQL("DROP TABLE clock_records")
                db.execSQL("ALTER TABLE clock_records_new RENAME TO clock_records")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clock_records ADD COLUMN courseMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "clock_in.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
