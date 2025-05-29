package quangan.sreminder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import quangan.sreminder.data.entity.Note
import java.util.UUID

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE noteType = 1 ORDER BY createdAt DESC")
    fun getAllRegularNotes(): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes WHERE noteType IN (2, 3) ORDER BY createdAt DESC")
    fun getAllReminderNotes(): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: UUID): Note?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long
    
    @Update
    suspend fun update(note: Note)
    
    @Delete
    suspend fun delete(note: Note)
    
    @Query("UPDATE notes SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: UUID, status: String)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}