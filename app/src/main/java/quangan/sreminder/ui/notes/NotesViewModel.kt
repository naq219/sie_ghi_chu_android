package quangan.sreminder.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import quangan.sreminder.data.AppDatabase
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.repository.NoteRepository
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allRegularNotes: LiveData<List<Note>>

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allRegularNotes = repository.allRegularNotes
    }

    fun insert(note: Note) = viewModelScope.launch {
        repository.insert(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
    }

    fun updateStatus(id: UUID, status: String) = viewModelScope.launch {
        repository.updateStatus(id, status)
    }
}