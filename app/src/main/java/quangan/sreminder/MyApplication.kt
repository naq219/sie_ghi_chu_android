package quangan.sreminder

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Khởi tạo Firebase Crashlytics - tự động bắt crashes
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}