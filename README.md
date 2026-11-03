# Geo Reminder — Android / GitHub Actions

ગુજરાતી location-based reminder app. Reminders are stored locally on the phone and use Android Geofencing (Google Play services) to notify when the device enters the saved radius.

## Build APK with GitHub
1. Create a new GitHub repository.
2. Extract this ZIP and upload the **contents** of `GeoReminder/` to the repository root.
3. Open **Actions** → **Build Geo Reminder APK** → **Run workflow**.
4. When the run completes, open its Artifacts section and download `GeoReminder-debug-apk`.
5. Extract the artifact ZIP to get `app-debug.apk`.

## First-time setup on Android
1. Install and open the app.
2. Allow precise location. Turn on the phone's Location setting.
3. Tap **બેકગ્રાઉન્ડ લોકેશન પરવાનગી આપો** → App info → Permissions → Location → **Allow all the time**.
4. Allow notifications.
5. Enter a reminder name and message. Tap **હાલનું લોકેશન મેળવો** to fill coordinates, or enter latitude/longitude manually.
6. Save. Minimum radius is 100 metres; default is 150 metres.

## Important behavior and limitations
- The app can store up to 100 geofences. It triggers on entering the saved radius.
- Geofence alerts are handled by Google Play services; the app does not need to stay open.
- Android may delay geofence delivery due to battery saver, device vendor restrictions, disabled location, or missing Google Play services. This is not a guaranteed instant GPS alarm.
- Background location permission is required for reminders to work when the app is not open.
- Data is local to this device; uninstalling the app or clearing its data removes reminders.
- The app does not request INTERNET permission. Google Play services/geofencing availability is still required by the Android device.
- App layout uses normal system window insets: content begins below the status bar, not behind it.
