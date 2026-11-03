package com.studymate.georeminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var titleInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var latInput: EditText
    private lateinit var lonInput: EditText
    private lateinit var radiusInput: EditText
    private lateinit var listLayout: LinearLayout

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocation()
        } else toast("લોકેશન પરવાનગી આપવી જરૂરી છે")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the app content below the Android status bar; do not draw controls behind it.
        window.decorView.systemUiVisibility = 0
        setContentView(R.layout.activity_main)
        titleInput = findViewById(R.id.titleInput)
        messageInput = findViewById(R.id.messageInput)
        latInput = findViewById(R.id.latInput)
        lonInput = findViewById(R.id.lonInput)
        radiusInput = findViewById(R.id.radiusInput)
        listLayout = findViewById(R.id.reminderList)

        findViewById<Button>(R.id.currentLocationButton).setOnClickListener {
            if (GeofenceHelper.hasLocationPermission(this)) getCurrentLocation()
            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveReminder() }
        findViewById<Button>(R.id.backgroundPermissionButton).setOnClickListener { requestBackgroundPermission() }

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 91)
        }
        renderList()
    }

    private fun getCurrentLocation() {
        if (!GeofenceHelper.hasLocationPermission(this)) return
        try {
            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        latInput.setText(location.latitude.toString())
                        lonInput.setText(location.longitude.toString())
                        toast("હાલનું લોકેશન ભરાઈ ગયું")
                    } else toast("લોકેશન મળ્યું નથી. GPS ચાલુ કરીને ફરી પ્રયાસ કરો.")
                }.addOnFailureListener { toast("લોકેશન મેળવવામાં ભૂલ: ${it.localizedMessage}") }
        } catch (_: SecurityException) { toast("લોકેશન પરવાનગી આપો") }
    }

    private fun saveReminder() {
        val title = titleInput.text.toString().trim()
        val message = messageInput.text.toString().trim()
        val lat = latInput.text.toString().toDoubleOrNull()
        val lon = lonInput.text.toString().toDoubleOrNull()
        val radius = radiusInput.text.toString().toFloatOrNull() ?: 150f
        if (title.isBlank() || lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 || radius < 100f) {
            toast("નામ, સાચું Latitude/Longitude અને ઓછામાં ઓછો 100m રેડિયસ દાખલ કરો")
            return
        }
        if (!GeofenceHelper.hasLocationPermission(this)) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            toast("પહેલાં લોકેશન પરવાનગી આપો, પછી ફરી સાચવો")
            return
        }
        if (!GeofenceHelper.hasBackgroundPermission(this)) {
            toast("'Allow all the time' બેકગ્રાઉન્ડ પરવાનગી આપો")
            requestBackgroundPermission()
            return
        }
        val r = Reminder(UUID.randomUUID().toString(), title, message, lat, lon, radius)
        val all = ReminderStore.get(this)
        if (all.size >= 100) { toast("વધુમાં વધુ 100 રિમાઇન્ડર રાખી શકાય"); return }
        all.add(r)
        ReminderStore.save(this, all)
        GeofenceHelper.register(this, r) { ok, error ->
            runOnUiThread {
                if (ok) {
                    toast("રિમાઇન્ડર સક્રિય થયું")
                    titleInput.text.clear(); messageInput.text.clear()
                    renderList()
                } else {
                    val current = ReminderStore.get(this).filterNot { it.id == r.id }
                    ReminderStore.save(this, current)
                    toast("સક્રિય થઈ શક્યું નહીં: ${error ?: "અજ્ઞાત ભૂલ"}")
                }
            }
        }
    }

    private fun requestBackgroundPermission() {
        if (Build.VERSION.SDK_INT < 29) { toast("આ Android વર્ઝનમાં અલગ બેકગ્રાઉન્ડ પરવાનગી જરૂરી નથી"); return }
        if (Build.VERSION.SDK_INT >= 30) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
            toast("Permissions > Location > Allow all the time પસંદ કરો")
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 92)
        }
    }

    private fun renderList() {
        listLayout.removeAllViews()
        val all = ReminderStore.get(this)
        if (all.isEmpty()) {
            val empty = TextView(this).apply { text = "હજુ કોઈ રિમાઇન્ડર નથી."; textSize = 14f; setPadding(8, 12, 8, 12) }
            listLayout.addView(empty); return
        }
        all.forEach { r ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val title = TextView(this).apply { text = "📍 ${r.title}"; textSize = 16f; setTextColor(0xFF17383D.toInt()) }
            val details = TextView(this).apply {
                text = "${r.message}\n${"%.5f".format(r.latitude)}, ${"%.5f".format(r.longitude)} • ${r.radius.toInt()} m"
                textSize = 13f; setTextColor(0xFF52666B.toInt())
            }
            val delete = Button(this).apply {
                text = "ડિલીટ કરો"; isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity).setMessage("આ રિમાઇન્ડર ડિલીટ કરવું છે?")
                        .setNegativeButton("રદ", null).setPositiveButton("ડિલીટ") { _, _ ->
                            GeofenceHelper.remove(this@MainActivity, r.id)
                            ReminderStore.save(this@MainActivity, ReminderStore.get(this@MainActivity).filterNot { it.id == r.id })
                            renderList()
                        }.show()
                }
            }
            item.addView(title); item.addView(details); item.addView(delete)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 12
            listLayout.addView(item, params)
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
