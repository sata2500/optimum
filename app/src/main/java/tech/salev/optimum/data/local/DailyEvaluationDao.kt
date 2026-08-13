package tech.salev.optimum.data.local

import androidx.room.*
import tech.salev.optimum.data.model.DailyEvaluation
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEvaluationDao {
    @Query("SELECT * FROM daily_evaluations WHERE date = :date LIMIT 1")
    fun getEvaluationForDate(date: String): Flow<DailyEvaluation?>

    @Query("SELECT * FROM daily_evaluations ORDER BY date DESC")
    fun getAllEvaluations(): Flow<List<DailyEvaluation>>

    @Query("SELECT * FROM daily_evaluations WHERE date = :date LIMIT 1")
    suspend fun getEvaluationForDateSync(date: String): DailyEvaluation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEvaluation(evaluation: DailyEvaluation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluations(evaluations: List<DailyEvaluation>)

    @Delete
    suspend fun deleteEvaluation(evaluation: DailyEvaluation)

    @Query("DELETE FROM daily_evaluations")
    suspend fun clearAllEvaluations()
}
