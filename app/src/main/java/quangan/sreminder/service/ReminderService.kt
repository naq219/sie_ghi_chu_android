package quangan.sreminder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import quangan.sreminder.R
import quangan.sreminder.data.AppDatabase
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.data.repository.ReminderRepository
import quangan.sreminder.data.repository.NoteRepository
import quangan.sreminder.utils.LunarCalendarUtils
import java.util.*
import kotlin.math.abs

class ReminderService : LifecycleService() {
    
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var noteRepository: NoteRepository
    private var checkJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "reminder_channel"
        private const val CHECK_INTERVAL = 30000L // 30 giây
        
        fun startService(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val database = AppDatabase.getDatabase(this)
        reminderRepository = ReminderRepository(database.reminderDao())
        noteRepository = NoteRepository(database.noteDao())
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        
        startReminderCheck()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        checkJob?.cancel()
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminder Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service chạy ngầm để kiểm tra nhắc nhở"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dịch vụ nhắc nhở")
            .setContentText("Đang chạy ngầm để kiểm tra nhắc nhở")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun startReminderCheck() {
        checkJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    checkAndTriggerReminders()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(CHECK_INTERVAL)
            }
        }
    }
    
    private suspend fun checkAndTriggerReminders() {
        val currentTime = Date()
        val activeReminders = reminderRepository.getActiveReminders()
        
        for (reminder in activeReminders) {
            if (shouldTriggerReminder(reminder, currentTime)) {
                showReminderNotification(reminder)
                
                // Tạo nhắc nhở tiếp theo nếu có lặp lại
                createNextReminder(reminder)
            }
        }
    }
    
    private fun shouldTriggerReminder(reminder: Reminder, currentTime: Date): Boolean {
        val timeDiff = abs(currentTime.time - reminder.remindAt.time)
        return timeDiff <= CHECK_INTERVAL // Trong khoảng thời gian check
    }
    
    private suspend fun createNextReminder(reminder: Reminder) {
        when (reminder.repeatType) {
            "interval" -> {
                reminder.repeatIntervalSeconds?.let { intervalSeconds ->
                    val nextTime = Date(reminder.remindAt.time + intervalSeconds * 1000)
                    val nextReminder = reminder.copy(
                        id = UUID.randomUUID(),
                        remindAt = nextTime,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    reminderRepository.insert(nextReminder)
                }
            }
            "daily" -> {
                val calendar = Calendar.getInstance().apply {
                    time = reminder.remindAt
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = calendar.time,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
            "weekly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = reminder.remindAt
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = calendar.time,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
            "solar_monthly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = reminder.remindAt
                    add(Calendar.MONTH, 1)
                }
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = calendar.time,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
            "lunar_monthly" -> {
                // Sử dụng LunarCalendarUtils để tính tháng âm tiếp theo
                val nextLunarDate = LunarCalendarUtils.getNextLunarMonth(reminder.remindAt)
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = nextLunarDate,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
            "solar_yearly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = reminder.remindAt
                    add(Calendar.YEAR, 1)
                }
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = calendar.time,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
            "lunar_yearly" -> {
                // Sử dụng LunarCalendarUtils để tính năm âm tiếp theo
                val nextLunarDate = LunarCalendarUtils.getNextLunarYear(reminder.remindAt)
                val nextReminder = reminder.copy(
                    id = UUID.randomUUID(),
                    remindAt = nextLunarDate,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                reminderRepository.insert(nextReminder)
            }
        }
        
        // Đánh dấu reminder hiện tại là đã hoàn thành
        reminderRepository.update(reminder.copy(isActive = false))
    }
    
    private suspend fun showReminderNotification(reminder: Reminder) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Lấy nội dung ghi chú
        val note = noteRepository.getNoteById(reminder.noteId)
        val noteContent = note?.content ?: "Đã đến lúc nhắc nhở của bạn!"
        
        // Tạo channel cho thông báo nhắc nhở
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
        
        val notification = NotificationCompat.Builder(this, "reminder_alerts")
            .setContentTitle("Nhắc nhở")
            .setContentText(noteContent)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(reminder.id.hashCode(), notification)
    }
}