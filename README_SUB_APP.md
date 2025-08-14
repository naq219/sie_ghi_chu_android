# Hướng dẫn triển khai ứng dụng con

Tài liệu này mô tả cách triển khai các ứng dụng con để nhận thông báo từ ứng dụng chính theo cơ chế luân phiên.

## Cấu trúc package

Các ứng dụng con phải có cấu trúc package theo mẫu:
- `quangan.sreminder.contb.t1`
- `quangan.sreminder.contb.t2`
- v.v.

## Triển khai BroadcastReceiver

Mỗi ứng dụng con cần triển khai một BroadcastReceiver để nhận thông báo từ ứng dụng chính. Dưới đây là mẫu code:

```kotlin
package quangan.sreminder.contb.t1 // Đây là package cho ứng dụng con

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "sub_app_notifications"
        private const val ACTION_SHOW_NOTIFICATION = "quangan.sreminder.ACTION_SHOW_NOTIFICATION"
        private const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        private const val EXTRA_NOTIFICATION_CONTENT = "notification_content"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_NOTIFICATION) {
            val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: "Nhắc nhở"
            val content = intent.getStringExtra(EXTRA_NOTIFICATION_CONTENT) ?: ""
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            
            // Hiển thị thông báo
            showNotification(context, title, content, notificationId)
            
            Log.d("SubApp", "Đã nhận và hiển thị thông báo: $title - $content")
        }
    }
    
    private fun showNotification(context: Context, title: String, content: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Tạo notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thông báo từ ứng dụng chính",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hiển thị thông báo từ ứng dụng chính"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Tạo notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Thay bằng icon của ứng dụng con
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        // Hiển thị notification
        notificationManager.notify(notificationId, notification)
    }
}
```

## Đăng ký trong AndroidManifest.xml

Trong file AndroidManifest.xml của ứng dụng con, cần đăng ký BroadcastReceiver:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="quangan.sreminder.contb.t1"> <!-- Package cho ứng dụng con -->

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Contb T1" <!-- Tên hiển thị của ứng dụng con -->
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        
        <!-- Đăng ký BroadcastReceiver -->
        <receiver
            android:name=".NotificationReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="quangan.sreminder.ACTION_SHOW_NOTIFICATION" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </receiver>
        
        <!-- Activity chính của ứng dụng con -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## Cách hoạt động

Khi ứng dụng chính cần hiển thị thông báo, nó sẽ gửi broadcast theo thứ tự luân phiên:

1. Lần 1: Ứng dụng chính hiển thị thông báo
2. Lần 2: Ứng dụng con 1 (quangan.sreminder.contb.t1) hiển thị thông báo
3. Lần 3: Ứng dụng con 2 (quangan.sreminder.contb.t2) hiển thị thông báo
...
Lần n: Quay lại ứng dụng chính hiển thị thông báo

Thứ tự này được quản lý bởi `NotificationBroadcastReceiver` trong ứng dụng chính.