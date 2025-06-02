package quangan.sreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import quangan.sreminder.service.ReminderService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Khởi động service nhắc nhở sau khi thiết bị khởi động
            ReminderService.startService(context)
        }
    }
}