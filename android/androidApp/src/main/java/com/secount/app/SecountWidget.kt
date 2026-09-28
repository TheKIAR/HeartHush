package com.secount.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home-screen widget: shows the next countdown. Updates when the app writes a
 * snapshot (each save), when the app resumes, and every ~30 min via
 * updatePeriodMillis (recomputed straight from events.json).
 */
class SecountWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateOne(context, mgr, id)
    }

    companion object {
        fun requestRefresh(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, SecountWidget::class.java))
                // Let the framework route through onUpdate.
                val intent = Intent(context, SecountWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            } catch (ignored: Exception) {
            }
        }

        private fun updateOne(context: Context, mgr: AppWidgetManager, id: Int) {
            try {
                var title: String? = null
                var sub: String? = null
                try {
                    val prefs = context.getSharedPreferences("secount_widget", Context.MODE_PRIVATE)
                    title = prefs.getString("title", null)
                    sub = prefs.getString("sub", null)
                } catch (ignored: Exception) {
                }
                if (title.isNullOrEmpty()) {
                    val snap = readNextFromFile(context)
                    title = snap.first
                    sub = snap.second
                }
                val views = RemoteViews(context.packageName, R.layout.secount_widget)
                views.setTextViewText(R.id.widget_title, title ?: "Secount")
                views.setTextViewText(R.id.widget_sub, sub ?: "Open the app")
                try {
                    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    if (launch != null) {
                        val pi = PendingIntent.getActivity(
                            context, 0, launch,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, pi)
                    }
                } catch (ignored: Exception) {
                }
                mgr.updateAppWidget(id, views)
            } catch (ignored: Exception) {
            }
        }

        private fun readNextFromFile(context: Context): Pair<String, String> {
            return try {
                val f = java.io.File(context.filesDir, "events.json")
                if (!f.exists()) return "Secount" to "No countdowns yet"
                val txt = f.readText()
                val titles = Regex("\"title\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(txt).map {
                    it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
                }.toList()
                val dates = Regex("\"date\"\\s*:\\s*\"([0-9]{4}-[0-9]{2}-[0-9]{2})\"").findAll(txt).map {
                    it.groupValues[1]
                }.toList()
                if (titles.isEmpty()) return "Secount" to "No countdowns yet"
                val today = java.time.LocalDate.now()
                var bestT = ""
                var bestD = Long.MAX_VALUE
                for (i in titles.indices) {
                    val ds = dates.getOrNull(i) ?: continue
                    val d = try {
                        java.time.LocalDate.parse(ds)
                    } catch (e: Exception) {
                        continue
                    }
                    if (d.isBefore(today)) continue
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
                    if (days < bestD) {
                        bestD = days
                        bestT = titles[i]
                    }
                }
                if (bestT.isEmpty()) "Secount" to "All done — add one!"
                else bestT to when (bestD) {
                    0L -> "Today! ♥"
                    1L -> "Tomorrow"
                    else -> "$bestD days left"
                }
            } catch (e: Exception) {
                "Secount" to "Open the app"
            }
        }
    }
}
