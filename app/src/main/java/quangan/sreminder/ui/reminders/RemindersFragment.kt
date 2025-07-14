package quangan.sreminder.ui.reminders

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.databinding.DialogNoteEditBinding
import quangan.sreminder.databinding.FragmentRemindersBinding
import quangan.sreminder.ui.adapters.ReminderAdapter
import java.util.*
import java.util.Date

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var remindersViewModel: RemindersViewModel
    private lateinit var reminderAdapter: ReminderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        remindersViewModel = ViewModelProvider(this).get(RemindersViewModel::class.java)

        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupObservers()
        setupListeners()

        return root
    }

    private fun setupRecyclerView() {
        reminderAdapter = ReminderAdapter(
            onItemClick = { note -> showNoteEditDialog(note) },
            onDeleteClick = { note -> deleteNote(note) },
            onCompleteClick = { note, reminder -> completeReminder(note, reminder) },
            onToggleReminderActive = { reminder, isActive -> toggleReminderActive(reminder, isActive) }
        )
        binding.recyclerViewReminders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reminderAdapter
        }
        
        // Thêm ItemTouchHelper cho swipe gestures với ngưỡng swipe cao hơn
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                // Tăng ngưỡng swipe từ 0.5 (mặc định) lên 0.7 để tránh nhầm lẫn
                return 0.7f
            }
            
            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                // Tăng vận tốc cần thiết để kích hoạt swipe
                return defaultValue * 2.0f
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = reminderAdapter.currentList[position]
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Swipe left - toggle completion status
                        val updatedNote = note.copy(
                            status = if (note.status == "completed") "active" else "completed",
                            updatedAt = Date()
                        )
                        remindersViewModel.update(updatedNote)
                        
                        val message = if (updatedNote.status == "completed") 
                            "Nhắc nhở đã được đánh dấu hoàn thành" 
                        else 
                            "Nhắc nhở đã được đánh dấu chưa hoàn thành"
                            
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                remindersViewModel.update(note)
                            }
                            .show()
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Swipe right - delete
                        remindersViewModel.delete(note)
                        
                        Snackbar.make(binding.root, "Nhắc nhở đã được xóa", Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                remindersViewModel.insert(note)
                            }
                            .show()
                    }
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewReminders)
    }

    private fun setupObservers() {
        remindersViewModel.allReminderNotes.observe(viewLifecycleOwner) { notes ->
            reminderAdapter.submitList(notes)
            binding.textEmptyReminders.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            
            // Load reminders cho mỗi note
            notes.forEach { note ->
                remindersViewModel.getRemindersByNoteId(note.id).observe(viewLifecycleOwner) { reminders ->
                    reminderAdapter.setReminders(note.id.toString(), reminders)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddReminder.setOnClickListener {
            showNoteEditDialog(null)
        }
    }

    private fun showNoteEditDialog(note: Note?) {
        val dialog = quangan.sreminder.ui.notes.AddNoteDialog.newInstance(note, useRemindersViewModel = true)
        dialog.show(parentFragmentManager, "AddNoteDialog")
    }

    private fun deleteNote(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_reminder)
            .setMessage(R.string.delete_reminder_confirm)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                remindersViewModel.delete(note)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    private fun completeReminder(note: Note, reminder: Reminder) {
        remindersViewModel.completeReminder(reminder)
    }
    
    private fun toggleReminderActive(reminder: Reminder, isActive: Boolean) {
        remindersViewModel.updateReminderActiveStatus(reminder.id, isActive)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}