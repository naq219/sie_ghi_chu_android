package quangan.sreminder.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder

/**
 * Lớp quan hệ giữa Note và Reminder
 */
data class NoteWithReminders(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val reminders: List<Reminder>
)