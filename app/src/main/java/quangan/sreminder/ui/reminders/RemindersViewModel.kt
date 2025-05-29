package quangan.sreminder.ui.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import quangan.sreminder.data.AppDatabase
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.data.repository.NoteRepository
import quangan.sreminder.data.repository.ReminderRepository
import java.util.Date
import java.util.UUID

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    private val reminderRepository: ReminderRepository
    val allReminderNotes: LiveData<List<Note>>

    init {
        val database = AppDatabase.getDatabase(application)
        noteRepository = NoteRepository(database.noteDao())
        reminderRepository = ReminderRepository(database.reminderDao())
        allReminderNotes = noteRepository.allReminderNotes
    }

    fun getRemindersByNoteId(noteId: UUID): LiveData<List<Reminder>> {
        return reminderRepository.getRemindersByNoteId(noteId)
    }

    fun insert(note: Note) = viewModelScope.launch {
        noteRepository.insert(note)
    }

    fun insert(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.insert(reminder)
    }

    fun update(note: Note) = viewModelScope.launch {
        noteRepository.update(note)
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.update(reminder)
    }

    fun delete(note: Note) = viewModelScope.launch {
        noteRepository.delete(note)
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        reminderRepository.delete(reminder)
    }

    fun updateNoteStatus(id: UUID, status: String) = viewModelScope.launch {
        noteRepository.updateStatus(id, status)
    }

    fun updateReminderActiveStatus(id: UUID, isActive: Boolean) = viewModelScope.launch {
        reminderRepository.updateActiveStatus(id, isActive)
    }

    fun completeReminder(reminder: Reminder) = viewModelScope.launch {
        val completedAt = Date()
        
        // Nếu là nhắc nhở lặp lại, tính thời gian nhắc tiếp theo
        if (reminder.repeatType != null) {
            val nextRemindAt = reminderRepository.calculateNextRemindTime(reminder, completedAt)
            if (nextRemindAt != null) {
                reminderRepository.updateNextRemindTime(reminder.id, nextRemindAt)
            }
        } else {
            // Nếu là nhắc nhở một lần, đánh dấu là không còn hoạt động
            reminderRepository.updateActiveStatus(reminder.id, false)
            // Đánh dấu ghi chú là đã hoàn thành
            noteRepository.updateStatus(reminder.noteId, "completed")
        }
    }
}