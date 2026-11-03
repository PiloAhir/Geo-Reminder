package com.studymate.georeminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val ids = event.triggeringGeofences?.map { it.requestId } ?: return
        val reminders = ReminderStore.get(context)
        ids.forEach { id ->
            val r = reminders.firstOrNull { it.id == id && it.active } ?: return@forEach
            showNotification(context, r)
        }
    }

    private fun showNotification(context: Context, r: Reminder) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "geo_reminders"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Location reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(r.title)
            .setContentText(r.message.ifBlank { "તમે નિર્ધારિત લોકેશન પર પહોંચી ગયા છો." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.message.ifBlank { "તમે નિર્ધારિત લોકેશન પર પહોંચી ગયા છો." }))
            .setAutoCancel(true).setContentIntent(open).build()
        manager.notify(r.id.hashCode(), notification)
    }
}
