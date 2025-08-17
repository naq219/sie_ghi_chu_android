package quangan.sreminder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat


/**
 * BroadcastReceiver để nhận thông báo từ ứng dụng chính và chuyển tiếp đến các ứng dụng con
 * theo thứ tự luân phiên.
 */
class NotificationBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "quangan.sreminder.ACTION_SHOW_NOTIFICATION"
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_CONTENT = "notification_content"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        // Danh sách các package name của ứng dụng con

        var nextIndex=0
        var countShow=1
        // Key để lưu trữ index của ứng dụng hiển thị thông báo cuối cùng
        private const val PREF_LAST_NOTIFICATION_APP_INDEX = "last_notification_app_index"
        private const val PREF_NAME = "notification_rotation_prefs"
        private var SUB_APP_PACKAGES: List<String> = listOf() // Initialize as empty or with a default
        fun updateSubAppPackages(context: Context, packages: List<String>) {
            SUB_APP_PACKAGES = packages +"chinhlatoi"

            Log.d("NotificationBroadcast 5", "Updated sub-app packages: $SUB_APP_PACKAGES")
            // Optionally, you might want to persist this list in SharedPreferences
            // if ReminderService doesn't run every time a notification is sent.
            // For simplicity, this example keeps it in memory.
        }


        /**
         * Gửi broadcast thông báo đến ứng dụng tiếp theo trong danh sách luân phiên
         */
        fun sendNotificationBroadcast(
            context: Context,
            title: String,
            content: String,
            notificationId: Int
        ) {
           // val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            //val lastIndex = sharedPreferences.getInt(PREF_LAST_NOTIFICATION_APP_INDEX, -1)

            // Tính toán index tiếp theo
           // val nextIndex = getNextAppIndex(lastIndex)

            // Lưu index mới
           // sharedPreferences.edit().putInt(PREF_LAST_NOTIFICATION_APP_INDEX, nextIndex).apply()

            // Xác định package name của ứng dụng sẽ hiển thị thông báo
            val targetPackage = SUB_APP_PACKAGES[nextIndex]
            Log.d("NotificationBroadcast targetPackage naq",targetPackage)
            nextIndex=nextIndex+1
            if (nextIndex >= SUB_APP_PACKAGES.size)  nextIndex=0
            if (targetPackage=="chinhlatoi"){
                showNotificationInternal(context, title+" "+countShow, content, notificationId)
                return // Không gửi broadcast nếu là ứng dụng chính
            }



            Log.d("NotificationBroadcast", "Gửi thông báo đến: $targetPackage")

            // Tạo intent broadcast
            val intent = Intent(ACTION_SHOW_NOTIFICATION)
            intent.setPackage(targetPackage)
            intent.putExtra(EXTRA_NOTIFICATION_TITLE, title)
            intent.putExtra(EXTRA_NOTIFICATION_CONTENT, content)
            intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)

            // Gửi broadcast
            context.sendBroadcast(intent)
        }


        private fun showNotificationInternal(context: Context, title: String, content: String, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "reminder_alerts",
                    "Nhắc nhở",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Thông báo nhắc nhở"
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, "reminder_alerts")
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            notificationManager.notify(notificationId, notification)
            Log.d("NotificationBroadcast", "Hiển thị thông báo (nội bộ): $title - $content")
        }

        /**
         * Tính toán index của ứng dụng tiếp theo sẽ hiển thị thông báo
         * -1: Ứng dụng chính
         * 0..n: Các ứng dụng con
         */
        private fun getNextAppIndex(lastIndex: Int): Int {
            // Nếu không có ứng dụng con, luôn trả về -1 (ứng dụng chính)
            if (SUB_APP_PACKAGES.isEmpty()) {
                return -1
            }

            // Tính toán index tiếp theo
            return when {
                lastIndex == -1 -> 0 // Từ ứng dụng chính -> ứng dụng con đầu tiên
                lastIndex >= SUB_APP_PACKAGES.size - 1 -> -1 // Từ ứng dụng con cuối -> ứng dụng chính
                else -> lastIndex + 1 // Đến ứng dụng con tiếp theo
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_NOTIFICATION) {
            val title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: "Nhắc nhở"
            val content = intent.getStringExtra(EXTRA_NOTIFICATION_CONTENT) ?: ""
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            // Hiển thị thông báo trong ứng dụng hiện tại
            showNotification(context, title, content, notificationId)
        }
    }

    /**
     * Hiển thị thông báo trong ứng dụng hiện tại
     */
    private fun showNotification(context: Context, title: String, content: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_alerts",
                "Nhắc nhở",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo notification
        val notification = NotificationCompat.Builder(context, "reminder_alerts")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Thay bằng icon của ứng dụng
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Hiển thị notification
        notificationManager.notify(notificationId, notification)

        Log.d("NotificationReceiver", "Hiển thị thông báo: $title - $content")
    }
}