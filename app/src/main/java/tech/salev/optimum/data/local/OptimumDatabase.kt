package tech.salev.optimum.data.local

import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog

private const val TAG = "OptimumDB_Migration"

@Database(
    entities = [
        Category::class,
        ActivityItem::class,
        TimeSlotLog::class,
        DailyEvaluation::class
    ],
    version = 8,
    exportSchema = true
)
abstract class OptimumDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun activityDao(): ActivityDao
    abstract fun timeSlotLogDao(): TimeSlotLogDao
    abstract fun dailyEvaluationDao(): DailyEvaluationDao

    companion object {

        // ─────────────────────────────────────────────────────────────────────
        // Migration tanımları — her birinin amacı ve yaptığı değişiklik belgelenmiş.
        // Yeni bir migration eklediğinde MUTLAKA ALL_MIGRATIONS dizisine de ekle!
        // ─────────────────────────────────────────────────────────────────────

        /** 1 → 2: DailyEvaluation tablosu oluşturuldu */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 1 → 2: creating daily_evaluations table")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `daily_evaluations` (
                        `date` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `journalNote` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`date`)
                    )"""
                )
            }
        }

        /** 2 → 3: exportSchema etkinleştirildi — yapısal değişiklik yok */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 2 → 3: schema export bump, no structural changes")
            }
        }

        /** 3 → 4: activity_items.shortCode ve daily_evaluations.updatedTimestamp eklendi */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 3 → 4: adding shortCode and updatedTimestamp columns")
                try {
                    db.execSQL("ALTER TABLE `activity_items` ADD COLUMN `shortCode` TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Sütun zaten varsa idempotent geçiş — güvenli
                    Log.w(TAG, "3→4: shortCode column likely already exists: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE `daily_evaluations` ADD COLUMN `updatedTimestamp` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.w(TAG, "3→4: updatedTimestamp column likely already exists: ${e.message}")
                }
            }
        }

        /** 4 → 5: daily_evaluations.mood eklendi */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 4 → 5: adding mood column to daily_evaluations")
                db.execSQL("ALTER TABLE `daily_evaluations` ADD COLUMN `mood` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 5 → 6: updatedTimestamp sütunu eksik kalan cihazlar için güvenlik geçişi */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 5 → 6: ensuring updatedTimestamp column exists")
                try {
                    db.execSQL("ALTER TABLE `daily_evaluations` ADD COLUMN `updatedTimestamp` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Sütun zaten varsa idempotent geçiş — güvenli
                    Log.w(TAG, "5→6: updatedTimestamp column likely already exists: ${e.message}")
                }
            }
        }

        /** 6 → 7: categories ve activity_items tablolarına displayOrder eklendi */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 6 → 7: adding displayOrder to categories and activity_items")
                try {
                    db.execSQL("ALTER TABLE `categories` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.w(TAG, "6→7: displayOrder column in categories likely already exists: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE `activity_items` ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.w(TAG, "6→7: displayOrder column in activity_items likely already exists: ${e.message}")
                }
            }
        }

        /** 7 → 8: time_slot_logs.date kolonu için performans index'i eklendi */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating 7 → 8: adding index on time_slot_logs.date")
                // ÖNEMLİ: Index ismi Room'un @Index("date") annotasyonundan ürettiği
                // "index_time_slot_logs_date" ile TAMAMEN eşleşmelidir.
                // Migration sonrası Room schema doğrulaması yapar;
                // isim uyuşmazlığı IllegalStateException'a yol açar.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_time_slot_logs_date` " +
                    "ON `time_slot_logs` (`date`)"
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // ✅ TEK KAYNAK (Single Source of Truth)
        //
        // Yeni migration eklediğinde:
        //   1. Yukarıya MIGRATION_X_Y olarak tanımla
        //   2. Bu diziye ekle
        //   3. @Database(version = ...) değerini güncelle
        //   4. schemas/ klasörüne yeni JSON export edildiğini doğrula
        //
        // AppModule.kt bu diziyi kullanır — başka bir yerde migration
        // kaydetmeye GEREK YOKTUR ve kaydetme!
        // ─────────────────────────────────────────────────────────────────────
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )
    }
}
