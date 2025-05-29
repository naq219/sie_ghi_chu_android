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
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.databinding.DialogNoteEditBinding
import quangan.sreminder.databinding.FragmentNotesBinding
import quangan.sreminder.ui.adapters.NoteAdapter
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
            onDeleteClick = { note -> deleteNote(note) }
        )
        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }
    }

    private fun setupObservers() {
        notesViewModel.allRegularNotes.observe(viewLifecycleOwner) { notes ->
            noteAdapter.submitList(notes)
            binding.textEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        binding.fabAddNote.setOnClickListener {
            showNoteEditDialog(null)
        }
    }

    private fun showNoteEditDialog(note: Note?) {
        val dialogBinding = DialogNoteEditBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(if (note == null) R.string.add_note else R.string.edit_note)
            .setView(dialogBinding.root)
            .create()

        // Thiết lập spinner cho đơn vị thời gian
        val intervalUnits = arrayOf("Giây", "Phút", "Giờ", "Ngày")
        val intervalMultipliers = arrayOf(1L, 60L, 3600L, 86400L) // Hệ số nhân để chuyển đổi sang giây
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, intervalUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerIntervalUnit.adapter = adapter

        // Nếu đang chỉnh sửa ghi chú hiện có
        if (note != null) {
            dialogBinding.editNoteTitle.setText(note.title)
            dialogBinding.editNoteContent.setText(note.content)
            
            // Thiết lập loại ghi chú
            when (note.noteType) {
                1 -> dialogBinding.radioRegularNote.isChecked = true
                2 -> dialogBinding.radioOneTimeReminder.isChecked = true
                3 -> dialogBinding.radioRepeatingReminder.isChecked = true
            }
        }

        // Xử lý hiển thị/ẩn các tùy chọn nhắc nhở dựa trên loại ghi chú
        dialogBinding.radioGroupNoteType.setOnCheckedChangeListener { _, checkedId ->
            val showReminderSettings = checkedId == R.id.radio_one_time_reminder || checkedId == R.id.radio_repeating_reminder
            dialogBinding.layoutReminderSettings.visibility = if (showReminderSettings) View.VISIBLE else View.GONE
            
            val showRepeatSettings = checkedId == R.id.radio_repeating_reminder
            dialogBinding.layoutRepeatSettings.visibility = if (showRepeatSettings) View.VISIBLE else View.GONE
        }

        // Xử lý hiển thị/ẩn các tùy chọn lặp lại dựa trên loại lặp
        dialogBinding.radioGroupRepeatType.setOnCheckedChangeListener { _, checkedId ->
            val showIntervalSettings = checkedId == R.id.radio_interval
            dialogBinding.layoutIntervalSettings.visibility = if (showIntervalSettings) View.VISIBLE else View.GONE
            
            val showMonthlySettings = checkedId == R.id.radio_solar_monthly || checkedId == R.id.radio_lunar_monthly
            dialogBinding.layoutMonthlySettings.visibility = if (showMonthlySettings) View.VISIBLE else View.GONE
        }

        // Thiết lập NumberPicker cho ngày trong tháng
        dialogBinding.numberPickerDay.minValue = 1
        dialogBinding.numberPickerDay.maxValue = 31
        dialogBinding.numberPickerDay.value = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Thiết lập các nút chọn ngày và giờ
        val calendar = Calendar.getInstance()
        var selectedDate = calendar.time

        dialogBinding.buttonDate.setOnClickListener {
            // Hiển thị DatePickerDialog
            // Trong ứng dụng thực tế, bạn sẽ thêm mã để hiển thị DatePickerDialog ở đây
            // và cập nhật selectedDate khi người dùng chọn ngày
        }

        dialogBinding.buttonTime.setOnClickListener {
            // Hiển thị TimePickerDialog
            // Trong ứng dụng thực tế, bạn sẽ thêm mã để hiển thị TimePickerDialog ở đây
            // và cập nhật selectedDate khi người dùng chọn giờ
        }

        // Xử lý nút Lưu
        dialogBinding.buttonSave.setOnClickListener {
            val title = dialogBinding.editNoteTitle.text.toString().trim()
            val content = dialogBinding.editNoteContent.text.toString().trim()
            
            if (title.isEmpty()) {
                dialogBinding.layoutNoteTitle.error = "Vui lòng nhập tiêu đề"
                return@setOnClickListener
            }
            
            // Xác định loại ghi chú
            val noteType = when (dialogBinding.radioGroupNoteType.checkedRadioButtonId) {
                R.id.radio_regular_note -> 1
                R.id.radio_one_time_reminder -> 2
                R.id.radio_repeating_reminder -> 3
                else -> 1
            }
            
            // Tạo hoặc cập nhật ghi chú
            val updatedNote = note?.copy(
                title = title,
                content = content,
                noteType = noteType,
                updatedAt = Date()
            ) ?: Note(
                id = UUID.randomUUID(),
                title = title,
                content = content,
                noteType = noteType,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )
            
            if (note == null) {
                notesViewModel.insert(updatedNote)
            } else {
                notesViewModel.update(updatedNote)
            }
            
            // Nếu là ghi chú có nhắc nhở, tạo hoặc cập nhật nhắc nhở
            if (noteType > 1) {
                // Trong ứng dụng thực tế, bạn sẽ thêm mã để tạo hoặc cập nhật nhắc nhở ở đây
                // dựa trên các tùy chọn người dùng đã chọn
            }
            
            alertDialog.dismiss()
        }

        // Xử lý nút Hủy
        dialogBinding.buttonCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
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