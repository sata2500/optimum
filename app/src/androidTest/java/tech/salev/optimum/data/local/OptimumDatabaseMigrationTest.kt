package tech.salev.optimum.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room Migration Testleri
 *
 * Bu testler her migration adımının:
 *   1. Şemayı doğru şekilde değiştirdiğini
 *   2. Mevcut veriyi kaybetmediğini
 *   3. Yeni sütunların doğru default değerlere sahip olduğunu
 * doğrular.
 *
 * Çalıştırmak için:
 *   ./gradlew :app:connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=tech.salev.optimum.data.local.OptimumDatabaseMigrationTest
 *
 * NOT: schemas/ klasörü androidTest assets'ine dahil edilmiştir (build.gradle.kts).
 */
@RunWith(AndroidJUnit4::class)
class OptimumDatabaseMigrationTest {

    private val TEST_DB = "optimum_migration_test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OptimumDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Tekil migration testleri
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun migrate1To2_createsEvaluationTable() {
        // Version 1 DB oluştur ve örnek veri ekle
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Eğitim', 'EG', '#4CAF50', 'School', 1)")
            close()
        }

        // Migration uygula ve şemayı doğrula
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, OptimumDatabase.MIGRATION_1_2)

        // daily_evaluations tablosunun oluşturulduğunu doğrula
        val cursor = db.query("SELECT COUNT(*) FROM `daily_evaluations`")
        assertTrue("daily_evaluations tablosu oluşturulmalı", cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        cursor.close()

        // Kategorilerin korunduğunu doğrula
        val catCursor = db.query("SELECT `name` FROM `categories` WHERE `id` = 1")
        assertTrue(catCursor.moveToFirst())
        assertEquals("Eğitim", catCursor.getString(0))
        catCursor.close()
    }

    @Test
    fun migrate2To3_noStructuralChanges_dataPreserved() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Spor', 'SP', '#FF5722', 'FitnessCenter', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, OptimumDatabase.MIGRATION_2_3)

        val cursor = db.query("SELECT `name` FROM `categories` WHERE `id` = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Spor", cursor.getString(0))
        cursor.close()
    }

    @Test
    fun migrate3To4_addsShortCodeAndUpdatedTimestamp() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Çalışma', 'CL', '#2196F3', 'Work', 1)")
            execSQL("INSERT INTO `activity_items` (`id`, `categoryId`, `name`, `activityNumber`, `description`, `colorHex`) VALUES (1, 1, 'Kitap Okuma', 1, '', '#2196F3')")
            execSQL("INSERT INTO `daily_evaluations` (`date`, `rating`, `journalNote`) VALUES ('2024-01-01', 4, 'İyi bir gün')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, OptimumDatabase.MIGRATION_3_4)

        // shortCode default '' olmalı
        val actCursor = db.query("SELECT `shortCode` FROM `activity_items` WHERE `id` = 1")
        assertTrue(actCursor.moveToFirst())
        assertEquals("", actCursor.getString(0))
        actCursor.close()

        // updatedTimestamp default 0 olmalı
        val evalCursor = db.query("SELECT `updatedTimestamp` FROM `daily_evaluations` WHERE `date` = '2024-01-01'")
        assertTrue(evalCursor.moveToFirst())
        assertEquals(0L, evalCursor.getLong(0))
        evalCursor.close()
    }

    @Test
    fun migrate4To5_addsMoodColumn() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Sağlık', 'SG', '#4CAF50', 'Favorite', 1)")
            execSQL("INSERT INTO `daily_evaluations` (`date`, `rating`, `journalNote`, `updatedTimestamp`) VALUES ('2024-02-01', 5, 'Harika gün', 1000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, OptimumDatabase.MIGRATION_4_5)

        // mood sütunu eklenmeli, default 0 olmalı
        val cursor = db.query("SELECT `mood` FROM `daily_evaluations` WHERE `date` = '2024-02-01'")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        cursor.close()
    }

    @Test
    fun migrate5To6_updatedTimestampIdempotent() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Dinlence', 'DN', '#9C27B0', 'Bedtime', 0)")
            execSQL("INSERT INTO `daily_evaluations` (`date`, `rating`, `journalNote`, `updatedTimestamp`, `mood`) VALUES ('2024-03-01', 3, '', 5000, 2)")
            close()
        }

        // Bu migration idempotent — varolan sütunu tekrar eklemeye çalışır ama log ile geçer
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, OptimumDatabase.MIGRATION_5_6)

        // Veri korunmalı
        val cursor = db.query("SELECT `updatedTimestamp`, `mood` FROM `daily_evaluations` WHERE `date` = '2024-03-01'")
        assertTrue(cursor.moveToFirst())
        assertEquals(5000L, cursor.getLong(0))
        assertEquals(2, cursor.getInt(1))
        cursor.close()
    }

    @Test
    fun migrate6To7_addsDisplayOrderToCategories() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'İş', 'IS', '#FF9800', 'Business', 1)")
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (2, 'Hobi', 'HB', '#E91E63', 'Star', 0)")
            execSQL("INSERT INTO `activity_items` (`id`, `categoryId`, `name`, `activityNumber`, `description`, `colorHex`, `shortCode`) VALUES (1, 1, 'Toplantı', 1, '', '#FF9800', 'M')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, OptimumDatabase.MIGRATION_6_7)

        // Kategorilere displayOrder eklenmeli, default 0
        val catCursor = db.query("SELECT `id`, `displayOrder` FROM `categories` ORDER BY `id`")
        assertTrue(catCursor.moveToFirst())
        assertEquals(0, catCursor.getInt(1)) // id=1
        catCursor.moveToNext()
        assertEquals(0, catCursor.getInt(1)) // id=2
        catCursor.close()

        // Aktivitelere displayOrder eklenmeli, default 0
        val actCursor = db.query("SELECT `displayOrder` FROM `activity_items` WHERE `id` = 1")
        assertTrue(actCursor.moveToFirst())
        assertEquals(0, actCursor.getInt(0))
        actCursor.close()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tam zincir testleri — veri bütünlüğü (data integrity)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Version 3'ten başlayan kurulumun (en yaygın senaryo) version 7'ye
     * hiçbir veri kaybı olmadan yükseltildiğini doğrular.
     *
     * Bu test, asıl çöken senaryoyu (3→7 migration zinciri) simüle eder.
     */
    @Test
    fun migrateFrom3To7_fullChain_dataIntact() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Eğitim', 'EG', '#4CAF50', 'School', 1)")
            execSQL("INSERT INTO `activity_items` (`id`, `categoryId`, `name`, `activityNumber`, `description`, `colorHex`) VALUES (1, 1, 'Kitap', 1, 'Günlük okuma', '#4CAF50')")
            execSQL("INSERT INTO `daily_evaluations` (`date`, `rating`, `journalNote`) VALUES ('2024-06-15', 4, 'Çok verimli')")
            execSQL("INSERT INTO `time_slot_logs` (`id`, `date`, `startTime`, `endTime`, `categoryId`, `activityId`, `note`, `timestamp`) VALUES (1, '2024-06-15', '09:00', '09:30', 1, 1, '', 1718436000000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 7, true,
            OptimumDatabase.MIGRATION_3_4,
            OptimumDatabase.MIGRATION_4_5,
            OptimumDatabase.MIGRATION_5_6,
            OptimumDatabase.MIGRATION_6_7
        )

        // Kategoriler korundu mu?
        val catCursor = db.query("SELECT `name`, `displayOrder` FROM `categories` WHERE `id` = 1")
        assertTrue("Kategori korunmalı", catCursor.moveToFirst())
        assertEquals("Eğitim", catCursor.getString(0))
        assertEquals(0, catCursor.getInt(1))
        catCursor.close()

        // Aktiviteler korundu mu?
        val actCursor = db.query("SELECT `name`, `shortCode`, `displayOrder` FROM `activity_items` WHERE `id` = 1")
        assertTrue("Aktivite korunmalı", actCursor.moveToFirst())
        assertEquals("Kitap", actCursor.getString(0))
        assertEquals("", actCursor.getString(1))
        assertEquals(0, actCursor.getInt(2))
        actCursor.close()

        // Günlük değerlendirmeler korundu mu?
        val evalCursor = db.query("SELECT `rating`, `journalNote`, `mood`, `updatedTimestamp` FROM `daily_evaluations` WHERE `date` = '2024-06-15'")
        assertTrue("Değerlendirme korunmalı", evalCursor.moveToFirst())
        assertEquals(4, evalCursor.getInt(0))
        assertEquals("Çok verimli", evalCursor.getString(1))
        assertEquals(0, evalCursor.getInt(2)) // mood default 0
        evalCursor.close()

        // Zaman logları korundu mu?
        val logCursor = db.query("SELECT `startTime`, `endTime`, `categoryId`, `activityId` FROM `time_slot_logs` WHERE `id` = 1")
        assertTrue("Zaman logu korunmalı", logCursor.moveToFirst())
        assertEquals("09:00", logCursor.getString(0))
        assertEquals("09:30", logCursor.getString(1))
        assertEquals(1L, logCursor.getLong(2))
        assertEquals(1L, logCursor.getLong(3))
        logCursor.close()
    }

    /**
     * Version 1'den version 7'ye tam zincir — tüm ALL_MIGRATIONS geçerli.
     */
    @Test
    fun migrateAllFrom1To7_fullChain_schemaValid() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO `categories` (`id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`) VALUES (1, 'Test', 'TST', '#000000', 'Star', 1)")
            close()
        }

        // Tüm migration'lar sırayla uygulanır ve şema doğrulanır
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 7, true,
            *OptimumDatabase.ALL_MIGRATIONS
        )

        // Kategorinin tüm sütunları mevcut olmalı
        val cursor = db.query("SELECT `id`, `name`, `code`, `colorHex`, `iconName`, `isProductive`, `displayOrder` FROM `categories`")
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("Test", cursor.getString(1))
        cursor.close()
    }
}
