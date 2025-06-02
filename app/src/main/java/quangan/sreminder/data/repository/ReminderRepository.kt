package quangan.sreminder.data.repository

import androidx.lifecycle.LiveData
import quangan.sreminder.data.dao.ReminderDao
import quangan.sreminder.data.entity.Reminder
import java.util.Calendar
import java.util.Date
import java.util.UUID

class ReminderRepository(private val reminderDao: ReminderDao) {
    
    fun getRemindersByNoteId(noteId: UUID): LiveData<List<Reminder>> {
        return reminderDao.getRemindersByNoteId(noteId)
    }
    
    suspend fun getDueReminders(date: Date = Date()): List<Reminder> {
        return reminderDao.getDueReminders(date)
    }
    
    suspend fun getActiveReminders(): List<Reminder> {
        return reminderDao.getActiveReminders()
    }
    
    suspend fun getReminderById(id: UUID): Reminder? {
        return reminderDao.getReminderById(id)
    }
    
    suspend fun insert(reminder: Reminder) {
        reminderDao.insert(reminder)
    }
    
    suspend fun update(reminder: Reminder) {
        val updatedReminder = reminder.copy(updatedAt = Date())
        reminderDao.update(updatedReminder)
    }
    
    suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
    }
    
    suspend fun updateActiveStatus(id: UUID, isActive: Boolean) {
        reminderDao.updateActiveStatus(id, isActive)
    }
    
    suspend fun updateNextRemindTime(id: UUID, nextRemindAt: Date) {
        reminderDao.updateNextRemindTime(id, nextRemindAt)
    }
    
    /**
     * Tính toán thời gian nhắc tiếp theo dựa trên loại lặp lại
     */
    suspend fun calculateNextRemindTime(reminder: Reminder, completedAt: Date = Date()): Date? {
        return when (reminder.repeatType) {
            "interval" -> {
                // Lần nhắc tiếp theo = thời điểm hoàn thành + khoảng thời gian lặp
                reminder.repeatIntervalSeconds?.let {
                    Date(completedAt.time + it * 1000)
                }
            }
            "solar_monthly" -> {
                // Lặp theo ngày dương lịch
                calculateNextSolarDate(reminder)
            }
            "lunar_monthly" -> {
                // Lặp theo ngày âm lịch - giả lập bằng cách thêm 30 ngày
                // Trong thực tế cần sử dụng thư viện chuyển đổi âm lịch
                calculateNextLunarDate(reminder)
            }
            else -> null
        }
    }
    
    private fun calculateNextSolarDate(reminder: Reminder): Date? {
        val repeatDay = reminder.repeatDay ?: return null
        val repeatTime = reminder.repeatTime ?: return null
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Phân tích giờ:phút
        val timeParts = repeatTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        // Đặt thời gian
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Nếu ngày lặp lại đã qua trong tháng này, chuyển sang tháng sau
        if (repeatDay < currentDay || (repeatDay == currentDay && calendar.time.before(Date()))) {
            calendar.add(Calendar.MONTH, 1)
        }
        
        // Đặt ngày lặp lại
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, Math.min(repeatDay, maxDay))
        
        return calendar.time
    }
    
    private fun calculateNextLunarDate(reminder: Reminder): Date? {
        // Giả lập tính toán ngày âm lịch bằng cách thêm 30 ngày
        // Trong thực tế cần sử dụng thư viện chuyển đổi âm lịch
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 30)
        return calendar.time
    }
}