package quangan.sreminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Entity đại diện cho một nhắc nhở
 * repeat_type: 'interval', 'solar_monthly', 'lunar_monthly', null nếu không lặp
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Reminder(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val noteId: UUID,
    val remindAt: Date,
    val repeatType: String? = null, // 'interval', 'solar_monthly', 'lunar_monthly', null nếu không lặp
    val repeatIntervalSeconds: Long? = null, // Khoảng thời gian lặp tính theo giây
    val repeatDay: Int? = null, // Ngày trong tháng (1-31)
    val repeatTime: String? = null, // Giờ trong ngày để nhắc, format "HH:mm"
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)