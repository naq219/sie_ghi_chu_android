package quangan.sreminder.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import java.util.Calendar
import java.util.Date
import java.util.UUID

class DemoDataGenerator(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val noteDao = database.noteDao()
    private val reminderDao = database.reminderDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun generateDemoData() {
        if(true) return
        scope.launch {
            // Xóa dữ liệu cũ
            noteDao.deleteAll()
            reminderDao.deleteAll()

            // Tạo ghi chú thông thường
            val regularNotes = listOf(
                Note(
                    id = UUID.randomUUID(),
                    title = "Danh sách mua sắm",
                    content = "- Sữa\n- Bánh mì\n- Trứng\n- Rau xanh\n- Thịt gà",
                    noteType = 1,
                    status = "active",
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                Note(
                    id = UUID.randomUUID(),
                    title = "Ý tưởng dự án",
                    content = "1. Ứng dụng quản lý chi tiêu\n2. Website bán hàng online\n3. Ứng dụng học ngoại ngữ\n4. Trò chơi giáo dục cho trẻ em",
                    noteType = 1,
                    status = "active",
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                Note(
                    id = UUID.randomUUID(),
                    title = "Công thức nấu ăn",
                    content = "Cách làm bánh mì sandwich:\n\n- 2 lát bánh mì\n- 1 lát phô mai\n- 1 lát thịt nguội\n- Rau xà lách\n- Cà chua\n- Sốt mayonnaise",
                    noteType = 1,
                    status = "active",
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )

            // Tạo ghi chú nhắc nhở một lần
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR, 2)
            val oneTimeNoteId = UUID.randomUUID()
            val oneTimeNote = Note(
                id = oneTimeNoteId,
                title = "Họp nhóm dự án",
                content = "Thảo luận về tiến độ và phân công công việc cho tuần tới",
                noteType = 2,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )

            val oneTimeReminder = Reminder(
                id = UUID.randomUUID(),
                noteId = oneTimeNoteId,
                remindAt = calendar.time,
                repeatType = null,
                repeatIntervalSeconds = null,
                repeatDay = null,
                repeatTime = null,
                isActive = true,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Tạo ghi chú nhắc nhở lặp lại theo khoảng thời gian
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val intervalNoteId = UUID.randomUUID()
            val intervalNote = Note(
                id = intervalNoteId,
                title = "Uống thuốc",
                content = "Uống thuốc theo đơn của bác sĩ",
                noteType = 3,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )

            val intervalReminder = Reminder(
                id = UUID.randomUUID(),
                noteId = intervalNoteId,
                remindAt = calendar.time,
                repeatType = "interval",
                repeatIntervalSeconds = 8 * 3600L, // 8 giờ
                repeatDay = null,
                repeatTime = null,
                isActive = true,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Tạo ghi chú nhắc nhở lặp lại theo ngày trong tháng (dương lịch)
            calendar.set(Calendar.DAY_OF_MONTH, 15)
            val solarNoteId = UUID.randomUUID()
            val solarNote = Note(
                id = solarNoteId,
                title = "Đóng tiền nhà",
                content = "Chuyển khoản tiền thuê nhà cho chủ nhà",
                noteType = 3,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )

            val solarReminder = Reminder(
                id = UUID.randomUUID(),
                noteId = solarNoteId,
                remindAt = calendar.time,
                repeatType = "solar_monthly",
                repeatIntervalSeconds = null,
                repeatDay = 15,
                repeatTime = null,
                isActive = true,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Tạo ghi chú nhắc nhở lặp lại theo ngày trong tháng (âm lịch)
            val lunarNoteId = UUID.randomUUID()
            val lunarNote = Note(
                id = lunarNoteId,
                title = "Giỗ ông nội",
                content = "Chuẩn bị đồ cúng và về quê dự giỗ ông nội",
                noteType = 3,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )

            val lunarReminder = Reminder(
                id = UUID.randomUUID(),
                noteId = lunarNoteId,
                remindAt = calendar.time,
                repeatType = "lunar_monthly",
                repeatIntervalSeconds = null,
                repeatDay = 10,
                repeatTime = null,
                isActive = true,
                createdAt = Date(),
                updatedAt = Date()
            )

            // Lưu tất cả dữ liệu vào cơ sở dữ liệu
            regularNotes.forEach { noteDao.insert(it) }
            
            noteDao.insert(oneTimeNote)
            reminderDao.insert(oneTimeReminder)
            
            noteDao.insert(intervalNote)
            reminderDao.insert(intervalReminder)
            
            noteDao.insert(solarNote)
            reminderDao.insert(solarReminder)
            
            noteDao.insert(lunarNote)
            reminderDao.insert(lunarReminder)
        }
    }
}