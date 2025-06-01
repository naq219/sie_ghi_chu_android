package quangan.sreminder.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
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
import quangan.sreminder.databinding.FragmentNotesBinding
import quangan.sreminder.ui.adapters.NoteAdapter
import quangan.sreminder.ui.notes.AddNoteDialog
import quangan.sreminder.MainActivity
import java.util.Calendar
import java.util.Date
import java.util.UUID

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)

        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupObservers()
        setupListeners()

        return root
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> showNoteEditDialog(note) },
            onDeleteClick = { note -> deleteNote(note) },
            onUpdateNote = { note -> notesViewModel.update(note) }
        )
        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
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
                return 5f
            }
            
            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                // Tăng vận tốc cần thiết để kích hoạt swipe
                return defaultValue * 6.3f
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = noteAdapter.currentList[position]
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Swipe left - toggle completion status
                        val updatedNote = note.copy(
                            status = if (note.status == "completed") "active" else "completed",
                            updatedAt = Date()
                        )
                        notesViewModel.update(updatedNote)
                        
                        val message = if (updatedNote.status == "completed") 
                            "Ghi chú đã được đánh dấu hoàn thành" 
                        else 
                            "Ghi chú đã được đánh dấu chưa hoàn thành"
                            
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                notesViewModel.update(note)
                            }
                            .show()
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Swipe right - delete
                        notesViewModel.delete(note)
                        
                        Snackbar.make(binding.root, "Ghi chú đã được xóa", Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                notesViewModel.insert(note)
                            }
                            .show()
                    }
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun setupObservers() {
        notesViewModel.allRegularNotes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.submitList(notes)
            binding.textEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        binding.fabAddNote.setOnClickListener {
            // Sử dụng MainActivity để tạo note mới, đảm bảo auto-save hoạt động
            (activity as? MainActivity)?.showAddNoteDialog()
        }
    }

    private fun showNoteEditDialog(note: Note?) {
        val dialog = AddNoteDialog.newInstance(note, useRemindersViewModel = false)
        dialog.show(parentFragmentManager, "AddNoteDialog")
    }

    private fun deleteNote(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_note)
            .setMessage(R.string.delete_note_confirm)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                notesViewModel.delete(note)
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}