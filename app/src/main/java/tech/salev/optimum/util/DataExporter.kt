package tech.salev.optimum.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.model.TimeSlotLog
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Handles data export functionality for the Optimum app.
 * Supports CSV and JSON formats. Uses FileProvider for secure file sharing.
 */
object DataExporter {

    private val fileTimestampFmt = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    /**
     * Exports time logs to a CSV file and launches the system share sheet.
     */
    suspend fun exportLogsAsCsv(
        context: Context,
        logs: List<TimeSlotLog>,
        categories: List<Category>,
        activities: List<ActivityItem>
    ) = withContext(Dispatchers.IO) {
        val catMap = categories.associateBy { it.id }
        val actMap = activities.associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("Tarih,Başlangıç,Bitiş,Kategori,Aktivite,Not,Verimli")

        logs.sortedWith(compareBy({ it.date }, { it.startTime })).forEach { log ->
            val cat = catMap[log.categoryId]
            val act = actMap[log.activityId]
            sb.appendLine(
                "${csvEscape(log.date)}," +
                "${csvEscape(log.startTime)}," +
                "${csvEscape(log.endTime)}," +
                "${csvEscape(cat?.name ?: "")}," +
                "${csvEscape(act?.name ?: "")}," +
                "${csvEscape(log.note)}," +
                "${if (cat?.isProductive == true) "Evet" else "Hayır"}"
            )
        }

        val timestamp = LocalDateTime.now().format(fileTimestampFmt)
        val file = createShareFile(context, "optimum_kayitlar_$timestamp.csv", sb.toString())
        shareFile(context, file, "text/csv")
    }

    /**
     * Exports daily evaluations to a CSV file.
     */
    suspend fun exportEvaluationsAsCsv(
        context: Context,
        evaluations: List<DailyEvaluation>
    ) = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("Tarih,Puan,Not")

        evaluations.sortedBy { it.date }.forEach { eval ->
            sb.appendLine(
                "${csvEscape(eval.date)}," +
                "${eval.rating}," +
                "${csvEscape(eval.journalNote)}"
            )
        }

        val timestamp = LocalDateTime.now().format(fileTimestampFmt)
        val file = createShareFile(context, "optimum_degerlendirmeler_$timestamp.csv", sb.toString())
        shareFile(context, file, "text/csv")
    }

    // -------------------------------------------------------------------------
    // JSON Export (Full Backup)
    // -------------------------------------------------------------------------

    /**
     * Exports all data as a single JSON backup file.
     */
    suspend fun exportFullBackupAsJson(
        context: Context,
        logs: List<TimeSlotLog>,
        categories: List<Category>,
        activities: List<ActivityItem>,
        evaluations: List<DailyEvaluation>
    ) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("exportDate", LocalDateTime.now().toString())
        root.put("version", 1)

        // Categories
        val catsArr = JSONArray()
        categories.forEach { cat ->
            catsArr.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("code", cat.code)
                put("colorHex", cat.colorHex)
                put("isProductive", cat.isProductive)
                put("iconName", cat.iconName)
            })
        }
        root.put("categories", catsArr)

        // Activities
        val actsArr = JSONArray()
        activities.forEach { act ->
            actsArr.put(JSONObject().apply {
                put("id", act.id)
                put("categoryId", act.categoryId)
                put("name", act.name)
                put("activityNumber", act.activityNumber)
                put("description", act.description)
                put("colorHex", act.colorHex)
            })
        }
        root.put("activities", actsArr)

        // Time Logs
        val logsArr = JSONArray()
        logs.sortedWith(compareBy({ it.date }, { it.startTime })).forEach { log ->
            logsArr.put(JSONObject().apply {
                put("id", log.id)
                put("date", log.date)
                put("startTime", log.startTime)
                put("endTime", log.endTime)
                put("categoryId", log.categoryId)
                put("activityId", log.activityId)
                put("note", log.note)
            })
        }
        root.put("timeLogs", logsArr)

        // Evaluations
        val evalsArr = JSONArray()
        evaluations.sortedBy { it.date }.forEach { eval ->
            evalsArr.put(JSONObject().apply {
                put("date", eval.date)
                put("rating", eval.rating)
                put("journalNote", eval.journalNote)
            })
        }
        root.put("evaluations", evalsArr)

        val timestamp = LocalDateTime.now().format(fileTimestampFmt)
        val file = createShareFile(context, "optimum_yedek_$timestamp.json", root.toString(2))
        shareFile(context, file, "application/json")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun createShareFile(context: Context, fileName: String, content: String): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        return File(dir, fileName).also { it.writeText(content, Charsets.UTF_8) }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Dışa Aktar").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
