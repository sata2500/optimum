package tech.salev.optimum.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncPayload(
    val userId: String,
    val userEmail: String,
    val syncedAt: String,
    val categories: List<Category>,
    val activities: List<ActivityItem>,
    val logs: List<TimeSlotLog>,
    val evaluations: List<DailyEvaluation>
)

@Singleton
class SyncRepository @Inject constructor(
    private val optimumRepository: OptimumRepository,
    private val authRepository: AuthRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun performCloudSync(): Result<String> {
        return try {
            val userProfile = authRepository.userProfileFlow.first()
            if (!userProfile.isLoggedIn) {
                return Result.failure(IllegalStateException("Bulut senkronizasyonu için Google hesabı ile giriş yapılmalıdır."))
            }

            val categories = optimumRepository.allCategories.first()
            val activities = optimumRepository.allActivities.first()
            val evaluations = optimumRepository.getAllEvaluations().first()
            
            // Get last 30 days logs or all logs
            val today = java.time.LocalDate.now()
            val monthAgo = today.minusDays(60)
            val logs = optimumRepository.getLogsBetweenDates(
                monthAgo.toString(),
                today.plusDays(30).toString()
            ).first()

            val nowStr = java.time.LocalDateTime.now().toString()
            val payload = SyncPayload(
                userId = userProfile.id,
                userEmail = userProfile.email,
                syncedAt = nowStr,
                categories = categories,
                activities = activities,
                logs = logs,
                evaluations = evaluations
            )

            // Convert to JSON string for Web API sync
            val payloadJson = json.encodeToString(payload)

            // Push payload to Next.js Vercel Sync Endpoint
            kotlin.runCatching {
                val url = java.net.URL("https://optimum-pi-black.vercel.app/api/sync")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                conn.outputStream.use { os ->
                    val input = payloadJson.toByteArray(charset("utf-8"))
                    os.write(input, 0, input.size)
                }

                conn.responseCode
            }

            // Update local last sync time
            authRepository.updateLastSyncTime()

            Result.success("Veriler başarıyla buluta eşitlendi! (${categories.size} kategori, ${logs.size} kayıt)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
