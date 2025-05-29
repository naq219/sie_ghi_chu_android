package quangan.sreminder.data.repository

import androidx.lifecycle.LiveData
import quangan.sreminder.data.dao.NoteDao
import quangan.sreminder.data.entity.Note
import java.util.Date
import java.util.UUID

class NoteRepository(private val noteDao: NoteDao) {
    
    val allRegularNotes: LiveData<List<Note>> = noteDao.getAllRegularNotes()
    val allReminderNotes: LiveData<List<Note>> = noteDao.getAllReminderNotes()
    
    suspend fun getNoteById(id: UUID): Note? {
        return noteDao.getNoteById(id)
    }
    
    suspend fun insert(note: Note) {
        noteDao.insert(note)
    }
    
    suspend fun update(note: Note) {
        val updatedNote = note.copy(updatedAt = Date())
        noteDao.update(updatedNote)
    }
    
    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }
    
    suspend fun updateStatus(id: UUID, status: String) {
        noteDao.updateStatus(id, status)
    }
}