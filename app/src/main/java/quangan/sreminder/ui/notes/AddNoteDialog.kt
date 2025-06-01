package quangan.sreminder.ui.notes

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.databinding.DialogNoteEditBinding
import quangan.sreminder.ui.notes.NotesViewModel
import quangan.sreminder.ui.reminders.RemindersViewModel
import java.util.*

class AddNoteDialog : DialogFragment() {
    
    private lateinit var binding: DialogNoteEditBinding
    private var notesViewModel: NotesViewModel? = null
    private var remindersViewModel: RemindersViewModel? = null
    private var editingNote: Note? = null
    
    companion object {
        private const val ARG_NOTE = "note"
        private const val ARG_USE_REMINDERS_VM = "use_reminders_vm"
        
        fun newInstance(note: Note? = null, useRemindersViewModel: Boolean = false): AddNoteDialog {
            val dialog = AddNoteDialog()
            val args = Bundle()
            note?.let { args.putSerializable(ARG_NOTE, it) }
            args.putBoolean(ARG_USE_REMINDERS_VM, useRemindersViewModel)
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogNoteEditBinding.inflate(layoutInflater)
        
        // Lấy dữ liệu từ arguments
        editingNote = arguments?.getSerializable(ARG_NOTE) as? Note
        val useRemindersViewModel = arguments?.getBoolean(ARG_USE_REMINDERS_VM, false) ?: false
        
        // Khởi tạo ViewModel phù hợp
        if (useRemindersViewModel) {
            remindersViewModel = ViewModelProvider(requireActivity()).get(RemindersViewModel::class.java)
        } else {
            notesViewModel = ViewModelProvider(requireActivity()).get(NotesViewModel::class.java)
        }
        
        // Tạo dialog full màn hình không có title với theme sáng
        val dialog = Dialog(requireContext(), android.R.style.Theme_Light_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        
        // Thiết lập layout params để full màn hình
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        setupDialog()
        
        // Hiển thị bàn phím tự động khi dialog mở
        dialog.setOnShowListener {
            binding.editNoteContent.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editNoteContent, InputMethodManager.SHOW_IMPLICIT)
        }
        
        return dialog
    }
    
    private fun setupDialog() {
        // Thiết lập spinner cho đơn vị thời gian
        val intervalUnits = arrayOf("Giây", "Phút", "Giờ", "Ngày")
        val intervalMultipliers = arrayOf(1L, 60L, 3600L, 86400L)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, intervalUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIntervalUnit.adapter = adapter
        
        // Thiết lập dữ liệu nếu đang chỉnh sửa
        editingNote?.let { note ->
            binding.editNoteContent.setText(note.content)
            
            // Thiết lập loại ghi chú
            when (note.noteType) {
                1 -> binding.radioRegularNote.isChecked = true
                2 -> binding.radioOneTimeReminder.isChecked = true
                3 -> binding.radioRepeatingReminder.isChecked = true
                else -> binding.radioRegularNote.isChecked = true
            }
        } ?: run {
            // Mặc định chọn ghi chú thường khi tạo mới
            binding.radioRegularNote.isChecked = true
        }
        
        // Xử lý hiển thị/ẩn các tùy chọn nhắc nhở dựa trên loại ghi chú
        binding.radioGroupNoteType.setOnCheckedChangeListener { _, checkedId ->
            val showReminderSettings = checkedId == R.id.radio_one_time_reminder || checkedId == R.id.radio_repeating_reminder
            binding.layoutReminderSettings.visibility = if (showReminderSettings) View.VISIBLE else View.GONE
            
            val showRepeatSettings = checkedId == R.id.radio_repeating_reminder
            binding.layoutRepeatSettings.visibility = if (showRepeatSettings) View.VISIBLE else View.GONE
        }
        
        // Xử lý hiển thị/ẩn các tùy chọn lặp lại dựa trên loại lặp
        binding.radioGroupRepeatType.setOnCheckedChangeListener { _, checkedId ->
            val showIntervalSettings = checkedId == R.id.radio_interval
            binding.layoutIntervalSettings.visibility = if (showIntervalSettings) View.VISIBLE else View.GONE
            
            val showMonthlySettings = checkedId == R.id.radio_solar_monthly || checkedId == R.id.radio_lunar_monthly
            binding.layoutMonthlySettings.visibility = if (showMonthlySettings) View.VISIBLE else View.GONE
        }
        
        // Thiết lập NumberPicker cho ngày trong tháng
        binding.numberPickerDay.minValue = 1
        binding.numberPickerDay.maxValue = 31
        binding.numberPickerDay.value = 1
        
        // Xử lý nút Lưu
        binding.buttonSave.setOnClickListener {
            saveNote()
        }
        
        // Xử lý nút Hủy
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        
        // Xử lý nút đóng trên toolbar
        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }
        
        // Xử lý nút lưu trên toolbar
        binding.btnSaveDialog.setOnClickListener {
            saveNote()
        }
        
        // Cập nhật title dựa trên chế độ
        binding.textDialogTitle.text = if (editingNote != null) "Chỉnh sửa ghi chú" else "Thêm ghi chú"
    }
    
    private fun saveNote() {
        val content = binding.editNoteContent.text.toString().trim()
        
        if (content.isEmpty()) {
            binding.editNoteContent.error = "Nội dung không được để trống"
            return
        }
        
        // Xác định loại ghi chú
        val noteType = when (binding.radioGroupNoteType.checkedRadioButtonId) {
            R.id.radio_regular_note -> 1
            R.id.radio_one_time_reminder -> 2
            R.id.radio_repeating_reminder -> 3
            else -> 1
        }
        
        val note = if (editingNote != null) {
            // Cập nhật ghi chú hiện có
            editingNote!!.copy(
                content = content,
                noteType = noteType,
                updatedAt = Date()
            )
        } else {
            // Tạo ghi chú mới
            Note(
                id = UUID.randomUUID(),
                title = "", // Không sử dụng title
                content = content,
                noteType = noteType,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )
        }
        
        // Lưu ghi chú bằng ViewModel phù hợp
        if (remindersViewModel != null) {
            if (editingNote != null) {
                remindersViewModel!!.update(note)
            } else {
                remindersViewModel!!.insert(note)
            }
        } else {
            if (editingNote != null) {
                notesViewModel!!.update(note)
            } else {
                notesViewModel!!.insert(note)
            }
        }
        
        // Nếu là ghi chú nhắc nhở, tạo reminder
        if (noteType == 2 || noteType == 3) {
            createReminder(note)
        }
        
        dismiss()
    }
    
    // Phương thức để MainActivity gọi khi cần tự động lưu ghi chú
    fun saveNoteFromActivity() {
        val content = binding.editNoteContent.text.toString().trim()
        
        // Chỉ lưu nếu có nội dung
        if (content.isNotEmpty()) {
            saveNote()
            // Hiển thị toast thông báo auto-save thành công
            Toast.makeText(requireContext(), "Ghi chú đã được tự động lưu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createReminder(note: Note) {
        // Logic tạo reminder sẽ được implement sau
        // Hiện tại chỉ tạo ghi chú cơ bản
    }
}