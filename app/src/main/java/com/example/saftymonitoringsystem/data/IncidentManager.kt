package com.example.saftymonitoringsystem.data

import android.content.Context
import com.example.saftymonitoringsystem.data.model.SafetyIncident
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage of [SafetyIncident] records using SharedPreferences.
 * Keeps a maximum of [MAX_INCIDENTS] entries to avoid unbounded storage growth.
 */
class IncidentManager(context: Context) {

    companion object {
        private const val MAX_INCIDENTS = 200
    }

    private val prefs = context.getSharedPreferences("safety_incidents", Context.MODE_PRIVATE)

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getIncidents(): List<SafetyIncident> {
        val jsonString = prefs.getString("incidents_list", "[]") ?: "[]"
        val jsonArray  = JSONArray(jsonString)
        val incidents  = mutableListOf<SafetyIncident>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val objectsArray = obj.getJSONArray("objects")
            val objects = (0 until objectsArray.length()).map { objectsArray.getString(it) }

            incidents.add(
                SafetyIncident(
                    id             = obj.getString("id"),
                    timestamp      = obj.getLong("timestamp"),
                    emotion        = obj.getString("emotion"),
                    emotionConf    = obj.optDouble("emotionConf", 0.0).toFloat(),
                    motionActivity = obj.optString("motionActivity", "Unknown"),
                    threatLevel    = obj.getInt("threatLevel"),
                    location       = obj.getString("location"),
                    mapsUrl        = obj.optString("mapsUrl", ""),
                    detectedObjects = objects,
                    alertSent      = obj.optBoolean("alertSent", false),
                    notes          = obj.optString("notes", "")
                )
            )
        }
        return incidents.sortedByDescending { it.timestamp }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    fun saveIncident(incident: SafetyIncident) {
        val incidents = getIncidents().toMutableList()
        incidents.add(0, incident)

        // Trim to max size
        val trimmed = if (incidents.size > MAX_INCIDENTS) incidents.take(MAX_INCIDENTS) else incidents
        saveList(trimmed)
    }

    fun deleteIncident(id: String) {
        saveList(getIncidents().filter { it.id != id })
    }

    fun clearAll() {
        prefs.edit().putString("incidents_list", "[]").apply()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun saveList(incidents: List<SafetyIncident>) {
        val jsonArray = JSONArray()
        incidents.forEach { item ->
            jsonArray.put(JSONObject().apply {
                put("id",             item.id)
                put("timestamp",      item.timestamp)
                put("emotion",        item.emotion)
                put("emotionConf",    item.emotionConf.toDouble())
                put("motionActivity", item.motionActivity)
                put("threatLevel",    item.threatLevel)
                put("location",       item.location)
                put("mapsUrl",        item.mapsUrl)
                put("alertSent",      item.alertSent)
                put("notes",          item.notes)
                val objectsArray = JSONArray()
                item.detectedObjects.forEach { objectsArray.put(it) }
                put("objects", objectsArray)
            })
        }
        prefs.edit().putString("incidents_list", jsonArray.toString()).apply()
    }
}
