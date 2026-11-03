package com.studymate.georeminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import androidx.core.content.ContextCompat

object GeofenceHelper {
    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, 7102, intent, flags)
    }

    fun register(context: Context, reminder: Reminder, onResult: (Boolean, String?) -> Unit) {
        if (!hasLocationPermission(context)) { onResult(false, "લોકેશન પરવાનગી જરૂરી છે"); return }
        val geofence = Geofence.Builder().setRequestId(reminder.id)
            .setCircularRegion(reminder.latitude, reminder.longitude, reminder.radius.coerceAtLeast(100f))
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE).build()
        val request = GeofencingRequest.Builder().setInitialTrigger(0)
            .addGeofence(geofence).build()
        try {
            LocationServices.getGeofencingClient(context).addGeofences(request, pendingIntent(context))
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { onResult(false, it.localizedMessage) }
        } catch (e: SecurityException) { onResult(false, e.localizedMessage) }
    }

    fun remove(context: Context, id: String) {
        try { LocationServices.getGeofencingClient(context).removeGeofences(listOf(id)) } catch (_: Exception) {}
    }

    fun restoreAll(context: Context) {
        if (!hasLocationPermission(context) || !hasBackgroundPermission(context)) return
        ReminderStore.get(context).filter { it.active }.forEach { r -> register(context, r) { _, _ -> } }
    }
}
