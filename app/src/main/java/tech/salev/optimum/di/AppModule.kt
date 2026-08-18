package tech.salev.optimum.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.salev.optimum.data.local.ActivityDao
import tech.salev.optimum.data.local.CategoryDao
import tech.salev.optimum.data.local.DailyEvaluationDao
import tech.salev.optimum.data.local.OptimumDatabase
import tech.salev.optimum.data.local.TimeSlotLogDao
import tech.salev.optimum.data.local.dataStore
import tech.salev.optimum.data.repository.OptimumRepository
import tech.salev.optimum.data.repository.OptimumRepositoryImpl
import tech.salev.optimum.data.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * Binds the interface to its implementation.
     * Hilt will inject OptimumRepositoryImpl wherever OptimumRepository is requested.
     */
    @Binds
    @Singleton
    abstract fun bindOptimumRepository(impl: OptimumRepositoryImpl): OptimumRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): OptimumDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OptimumDatabase::class.java,
                "optimum_database"
            )
            // ✅ OptimumDatabase.ALL_MIGRATIONS tek kaynak olarak kullanılır.
            // Yeni migration eklediğinde SADECE OptimumDatabase.kt'yi güncelle —
            // bu satıra dokunmana gerek yok.
            .addMigrations(*OptimumDatabase.ALL_MIGRATIONS)
            // ✅ Kullanıcı eski bir sürüme dönerse (beta rollback vb.) çökmek yerine
            // temiz bir veritabanıyla başlar (tüm tablolar silinir).
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            // ✅ WAL modu: Eş zamanlı okuma/yazma desteği — UI okurken arka plan servisi
            // yazabilir; bildirim + ana ekran aynı anda çalışırken kritik öneme sahip.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        }

        @Provides
        fun provideCategoryDao(db: OptimumDatabase): CategoryDao = db.categoryDao()

        @Provides
        fun provideActivityDao(db: OptimumDatabase): ActivityDao = db.activityDao()

        @Provides
        fun provideTimeSlotLogDao(db: OptimumDatabase): TimeSlotLogDao = db.timeSlotLogDao()

        @Provides
        fun provideDailyEvaluationDao(db: OptimumDatabase): DailyEvaluationDao = db.dailyEvaluationDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.dataStore

        @Provides
        @Singleton
        fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
            SettingsRepository(dataStore)
    }
}
