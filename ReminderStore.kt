package com.studymate.georeminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderStore {
    private const val PREFS = "geo_reminder_store"
    private const val KEY = "items"

    fun get(context: Context): MutableList<Reminder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Reminder>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Reminder(o.getString("id"), o.getString("title"), o.getString("message"),
                o.getDouble("lat"), o.getDouble("lon"), o.getDouble("radius").toFloat(), o.optBoolean("active", true)))
        }
        return list
    }

    fun save(context: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("title", r.title).put("message", r.message)
                .put("lat", r.latitude).put("lon", r.longitude).put("radius", r.radius).put("active", r.active))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
