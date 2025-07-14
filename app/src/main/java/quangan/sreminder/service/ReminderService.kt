package quangan.sreminder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import quangan.sreminder.R
import quangan.sreminder.data.AppDatabase
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.data.repository.ReminderRepository
import quangan.sreminder.data.repository.NoteRepository
import quangan.sreminder.utils.LunarCalendarUtils
import android.util.*
import java.util.*
import kotlin.math.abs

class ReminderService : LifecycleService() {
    
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var noteRepository: NoteRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var checkJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "reminder_channel"
        private const val CHECK_INTERVAL = 30000L // 30 giây
        private const val ACTION_TOGGLE_REMINDERS = "quangan.sreminder.TOGGLE_REMINDERS"
        
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
        
        sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        
        val database = AppDatabase.getDatabase(this)
        reminderRepository = ReminderRepository(database.reminderDao())
        noteRepository = NoteRepository(database.noteDao())
        
        startReminderCheck()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        checkJob?.cancel()
        stopForeground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_TOGGLE_REMINDERS == intent.action) {
            toggleReminders()
            updateNotification()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun toggleReminders() {
        val current = sharedPreferences.getBoolean("global_reminders_enabled", true)
        sharedPreferences.edit().putBoolean("global_reminders_enabled", !current).apply()
    }

    private fun updateNotification() {
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
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
        val enabled = sharedPreferences.getBoolean("global_reminders_enabled", true)
        val statusText = if (enabled) "Bật" else "Tắt"
        val statusTextNguoc = if (enabled) "Tắt" else "Bật"

        val toggleIntent = Intent(this, ReminderService::class.java)
        toggleIntent.action = ACTION_TOGGLE_REMINDERS
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dịch vụ nhắc nhở")
            .setContentText("Đang $statusText thông báo. 👉 click để $statusTextNguoc")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
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
        // Kiểm tra cài đặt toàn cục trước khi xử lý nhắc nhở
        val globalRemindersEnabled = sharedPreferences.getBoolean("global_reminders_enabled", true)
        Log.d("ReminderService", "Global reminders enabled: $globalRemindersEnabled")
        if (!globalRemindersEnabled) {
            return // Không xử lý nhắc nhở nếu đã tắt toàn cục
        }
        
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
        // Nếu thời gian hiện tại >= thời gian nhắc nhở thì kích hoạt
        // Điều này bao gồm cả trường hợp đã "lỡ" thời gian
        return currentTime.time >= reminder.remindAt.time
    }
    
    private suspend fun createNextReminder(reminder: Reminder) {
        // Nếu là reminder một lần, vô hiệu hóa sau khi kích hoạt
        if (reminder.repeatType == "none" || reminder.repeatType.isNullOrEmpty()) {
            val updatedReminder = reminder.copy(
                isActive = false,
                updatedAt = Date()
            )
            reminderRepository.update(updatedReminder)
            return
        }
        
        val currentTime = Date()
        when (reminder.repeatType) {
            "interval" -> {
                reminder.repeatIntervalSeconds?.let { intervalSeconds ->
                    // Tính thời gian tiếp theo từ thời gian hiện tại
                    val nextTime = Date(currentTime.time + intervalSeconds * 1000)
                    val updatedReminder = reminder.copy(
                        remindAt = nextTime,
                        updatedAt = Date()
                    )
                    reminderRepository.update(updatedReminder)
                }
            }
            "minutely" -> {
                // Lấy repeatInterval từ Note entity (tính bằng phút)
                val note = noteRepository.getNoteById(reminder.noteId)
                note?.let { n ->
                    if (n.repeatInterval > 0) {
                        // Tính thời gian tiếp theo từ thời gian hiện tại
                        val nextTime = Date(currentTime.time + n.repeatInterval * 60 * 1000)
                        val updatedReminder = reminder.copy(
                            remindAt = nextTime,
                            updatedAt = Date()
                        )
                        reminderRepository.update(updatedReminder)
                    }
                }
            }
            "hourly" -> {
                // Lấy repeatInterval từ Note entity (tính bằng phút)
                val note = noteRepository.getNoteById(reminder.noteId)
                note?.let { n ->
                    if (n.repeatInterval > 0) {
                        // Tính thời gian tiếp theo từ thời gian hiện tại
                        val nextTime = Date(currentTime.time + n.repeatInterval * 60 * 1000)
                        val updatedReminder = reminder.copy(
                            remindAt = nextTime,
                            updatedAt = Date()
                        )
                        reminderRepository.update(updatedReminder)
                    }
                }
            }
            "daily" -> {
                val calendar = Calendar.getInstance().apply {
                    time = currentTime
                    // Giữ nguyên giờ phút giây từ reminder gốc
                    val originalCalendar = Calendar.getInstance().apply { time = reminder.remindAt }
                    set(Calendar.HOUR_OF_DAY, originalCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, originalCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, originalCalendar.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, originalCalendar.get(Calendar.MILLISECOND))
                    // Nếu thời gian đã qua trong ngày hôm nay, chuyển sang ngày mai
                    if (time.before(currentTime)) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val updatedReminder = reminder.copy(
                    remindAt = calendar.time,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
            "weekly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = currentTime
                    // Giữ nguyên giờ phút giây từ reminder gốc
                    val originalCalendar = Calendar.getInstance().apply { time = reminder.remindAt }
                    set(Calendar.HOUR_OF_DAY, originalCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, originalCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, originalCalendar.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, originalCalendar.get(Calendar.MILLISECOND))
                    // Tìm ngày trong tuần tiếp theo
                    val targetDayOfWeek = originalCalendar.get(Calendar.DAY_OF_WEEK)
                    while (get(Calendar.DAY_OF_WEEK) != targetDayOfWeek || time.before(currentTime)) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val updatedReminder = reminder.copy(
                    remindAt = calendar.time,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
            "solar_monthly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = currentTime
                    // Giữ nguyên giờ phút giây từ reminder gốc
                    val originalCalendar = Calendar.getInstance().apply { time = reminder.remindAt }
                    set(Calendar.HOUR_OF_DAY, originalCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, originalCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, originalCalendar.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, originalCalendar.get(Calendar.MILLISECOND))
                    // Đặt ngày trong tháng
                    val targetDay = originalCalendar.get(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, minOf(targetDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
                    // Nếu thời gian đã qua trong tháng này, chuyển sang tháng sau
                    if (time.before(currentTime)) {
                        add(Calendar.MONTH, 1)
                        set(Calendar.DAY_OF_MONTH, minOf(targetDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
                    }
                }
                val updatedReminder = reminder.copy(
                    remindAt = calendar.time,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
            "lunar_monthly" -> {
                // Sử dụng LunarCalendarUtils để tính tháng âm tiếp theo từ thời gian hiện tại
                val nextLunarDate = LunarCalendarUtils.getNextLunarMonth(currentTime)
                val updatedReminder = reminder.copy(
                    remindAt = nextLunarDate,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
            "solar_yearly" -> {
                val calendar = Calendar.getInstance().apply {
                    time = currentTime
                    // Giữ nguyên giờ phút giây từ reminder gốc
                    val originalCalendar = Calendar.getInstance().apply { time = reminder.remindAt }
                    set(Calendar.HOUR_OF_DAY, originalCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, originalCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, originalCalendar.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, originalCalendar.get(Calendar.MILLISECOND))
                    // Đặt tháng và ngày
                    set(Calendar.MONTH, originalCalendar.get(Calendar.MONTH))
                    val targetDay = originalCalendar.get(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, minOf(targetDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
                    // Nếu thời gian đã qua trong năm này, chuyển sang năm sau
                    if (time.before(currentTime)) {
                        add(Calendar.YEAR, 1)
                        set(Calendar.DAY_OF_MONTH, minOf(targetDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
                    }
                }
                val updatedReminder = reminder.copy(
                    remindAt = calendar.time,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
            "lunar_yearly" -> {
                // Sử dụng LunarCalendarUtils để tính năm âm tiếp theo từ thời gian hiện tại
                val nextLunarDate = LunarCalendarUtils.getNextLunarYear(currentTime)
                val updatedReminder = reminder.copy(
                    remindAt = nextLunarDate,
                    updatedAt = Date()
                )
                reminderRepository.update(updatedReminder)
            }
        }
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