package tech.salev.optimum.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog

data class OptimumBackupData(
    val categories: List<Category>,
    val activities: List<ActivityItem>,
    val timeLogs: List<TimeSlotLog>,
    val evaluations: List<DailyEvaluation>
)

object DataImporter {

    suspend fun parseJsonBackup(context: Context, uri: Uri): OptimumBackupData = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: throw IllegalArgumentException("Dosya okunamadı.")

        val root = JSONObject(jsonString)

        val categories = mutableListOf<Category>()
        if (root.has("categories")) {
            val catsArr = root.getJSONArray("categories")
            for (i in 0 until catsArr.length()) {
                val obj = catsArr.getJSONObject(i)
                categories.add(
                    Category(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        code = obj.getString("code"),
                        colorHex = obj.getString("colorHex"),
                        isProductive = obj.getBoolean("isProductive"),
                        iconName = obj.optString("iconName", "Work")
                    )
                )
            }
        }

        val activities = mutableListOf<ActivityItem>()
        if (root.has("activities")) {
            val actsArr = root.getJSONArray("activities")
            for (i in 0 until actsArr.length()) {
                val obj = actsArr.getJSONObject(i)
                activities.add(
                    ActivityItem(
                        id = obj.getLong("id"),
                        categoryId = obj.getLong("categoryId"),
                        name = obj.getString("name"),
                        activityNumber = obj.getInt("activityNumber"),
                        description = obj.optString("description", ""),
                        colorHex = obj.getString("colorHex"),
                        shortCode = obj.optString("shortCode", "")
                    )
                )
            }
        }

        val timeLogs = mutableListOf<TimeSlotLog>()
        if (root.has("timeLogs")) {
            val logsArr = root.getJSONArray("timeLogs")
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.getJSONObject(i)
                timeLogs.add(
                    TimeSlotLog(
                        id = obj.optLong("id", 0L).takeIf { it > 0 } ?: 0L,
                        date = obj.getString("date"),
                        startTime = obj.getString("startTime"),
                        endTime = obj.getString("endTime"),
                        categoryId = obj.getLong("categoryId"),
                        activityId = obj.getLong("activityId"),
                        note = obj.optString("note", "")
                    )
                )
            }
        }

        val evaluations = mutableListOf<DailyEvaluation>()
        if (root.has("evaluations")) {
            val evalsArr = root.getJSONArray("evaluations")
            for (i in 0 until evalsArr.length()) {
                val obj = evalsArr.getJSONObject(i)
                evaluations.add(
                    DailyEvaluation(
                        date = obj.getString("date"),
                        rating = obj.getInt("rating"),
                        journalNote = obj.optString("journalNote", ""),
                        mood = obj.optInt("mood", 0),
                        updatedTimestamp = obj.optLong("updatedTimestamp", 0L)
                    )
                )
            }
        }

        OptimumBackupData(categories, activities, timeLogs, evaluations)
    }
}
