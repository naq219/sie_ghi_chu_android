package quangan.sreminder.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import quangan.sreminder.data.entity.Reminder
import java.util.Date
import java.util.UUID

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE noteId = :noteId")
    fun getRemindersByNoteId(noteId: UUID): LiveData<List<Reminder>>
    
    @Query("SELECT * FROM reminders ORDER BY remindAt DESC")
    suspend fun getAllReminders(): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE remindAt <= :date AND isActive = 1")
    suspend fun getDueReminders(date: Date): List<Reminder>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: UUID): Reminder?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long
    
    @Update
    suspend fun update(reminder: Reminder)
    
    @Delete
    suspend fun delete(reminder: Reminder)
    
    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: UUID, isActive: Boolean)
    
    @Query("UPDATE reminders SET remindAt = :nextRemindAt WHERE id = :id")
    suspend fun updateNextRemindTime(id: UUID, nextRemindAt: Date)
    
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
    
    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}