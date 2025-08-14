package quangan.sreminder.service // Or your actual service package

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import quangan.sreminder.receiver.NotificationBroadcastReceiver // Import your receiver

class ReminderService : Service() {

    private val SUB_APP_PACKAGE_PREFIX = "quangan.sreminder.contb."

    override fun onCreate() {
        super.onCreate()
        Log.d("ReminderService", "Service Created. Loading sub-app packages...")
        loadAndSetSubAppPackages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ReminderService", "Service Started.")
        // If you want to refresh the list periodically or on certain events,
        // you can call loadAndSetSubAppPackages() here as well.
        // For this example, it's primarily done in onCreate.
        return START_STICKY // Or your preferred restart behavior
    }

    private fun loadAndSetSubAppPackages() {
        val packageManager = packageManager
        val discoveredPackages = mutableListOf<String>()

        try {
            // Get all installed applications
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo: ApplicationInfo in installedApps) {
                if (appInfo.packageName.startsWith(SUB_APP_PACKAGE_PREFIX)) {
                    discoveredPackages.add(appInfo.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderService", "Error loading sub-app packages", e)
        }

        if (discoveredPackages.isNotEmpty()) {
            // Sort them if the order matters (e.g., t1, t2, t10)
            discoveredPackages.sort() // Simple lexicographical sort
            NotificationBroadcastReceiver.updateSubAppPackages(applicationContext, discoveredPackages)
        } else {
            Log.w("ReminderService", "No sub-app packages found with prefix: $SUB_APP_PACKAGE_PREFIX")
            // Update with an empty list or handle as needed
            NotificationBroadcastReceiver.updateSubAppPackages(applicationContext, emptyList())
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ReminderService", "Service Destroyed.")
    }
}
