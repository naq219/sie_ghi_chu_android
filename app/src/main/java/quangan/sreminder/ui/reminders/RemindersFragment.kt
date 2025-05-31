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
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.databinding.DialogNoteEditBinding
import quangan.sreminder.databinding.FragmentRemindersBinding
import quangan.sreminder.ui.adapters.ReminderAdapter
import java.util.Calendar
import java.util.Date
import java.util.UUID

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
            onCompleteClick = { note, reminder -> completeReminder(note, reminder) }
        )
        binding.recyclerViewReminders.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reminderAdapter
        }
    }

    private fun setupObservers() {
        remindersViewModel.allReminderNotes.observe(viewLifecycleOwner) { notes ->
            reminderAdapter.submitList(notes)
            binding.textEmptyReminders.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}