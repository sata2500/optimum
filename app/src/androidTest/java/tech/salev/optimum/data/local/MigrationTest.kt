package tech.salev.optimum.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OptimumDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To6() {
        // Create earliest version of the database.
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert some data if needed here...
        
        // Prepare for the next version.
        db.close()

        // Re-open the database with version 6 and provide ALL manual migrations
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            OptimumDatabase.MIGRATION_1_2,
            OptimumDatabase.MIGRATION_2_3,
            OptimumDatabase.MIGRATION_3_4,
            OptimumDatabase.MIGRATION_4_5,
            OptimumDatabase.MIGRATION_5_6
        )
        
        // If this doesn't throw, our migrations are 100% perfect!
    }
}
