package quangan.sreminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Entity đại diện cho một ghi chú
 * note_type: 1 - Ghi chú thông thường, 2 - Nhắc hẹn một lần, 3 - Nhắc lặp
 * status: active, completed
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val content: String?,
    val noteType: Int, // 1: thông thường, 2: nhắc hẹn 1 lần, 3: nhắc lặp
    val status: String = "active",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)