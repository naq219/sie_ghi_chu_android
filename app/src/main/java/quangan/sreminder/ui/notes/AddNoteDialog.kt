package quangan.sreminder.ui.notes

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
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
import quangan.sreminder.utils.LunarCalendarUtils
import java.text.SimpleDateFormat
import java.util.*

class AddNoteDialog : DialogFragment() {
    
    private lateinit var binding: DialogNoteEditBinding
    private var notesViewModel: NotesViewModel? = null
    private var remindersViewModel: RemindersViewModel? = null
    private var editingNote: Note? = null
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    
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
        
        // Mặc định chọn lịch dương
        binding.radioSolarCalendar.isChecked = true
        
        // Khởi tạo ngày giờ mặc định
        selectedDate = Calendar.getInstance()
        selectedTime = Calendar.getInstance()
        
        // Cập nhật hiển thị ngày giờ ban đầu
        updateDateTimeDisplay()
        
        // Xử lý thay đổi loại lịch
        binding.radioGroupCalendarType.setOnCheckedChangeListener { _, _ ->
            updateDateTimeDisplay()
        }
        
        // Xử lý nút chọn ngày
        binding.buttonDate.setOnClickListener {
            showDatePicker()
        }
        
        // Xử lý nút chọn giờ
        binding.buttonTime.setOnClickListener {
            showTimePicker()
        }
        
        // Xử lý các tùy chọn thời gian nhanh
        binding.text15Minutes.setOnClickListener {
            setQuickTimeOption(15)
        }
        
        binding.text30Minutes.setOnClickListener {
            setQuickTimeOption(30)
        }
        
        binding.text1Hour.setOnClickListener {
            setQuickTimeOption(60)
        }
        
        binding.text2Hours.setOnClickListener {
            setQuickTimeOption(120)
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
            // Ẩn tất cả các layout trước
            binding.layoutHoursMinutesSettings.visibility = View.GONE
            binding.layoutWeekdaysSettings.visibility = View.GONE
            binding.layoutMonthlySettings.visibility = View.GONE
            
            // Hiển thị layout tương ứng
            when (checkedId) {
                R.id.radio_interval_hours_minutes -> {
                    binding.layoutHoursMinutesSettings.visibility = View.VISIBLE
                }
                // Hằng tháng và hằng năm sẽ dựa vào ngày giờ được chọn, không cần cài đặt thêm
                // Các tùy chọn khác (hằng ngày, hằng tuần) không cần cài đặt thêm
            }
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
        
        // Tính toán repeatInterval cho ghi chú lặp lại
        var repeatInterval = 0L
        if (noteType == 3) {
            val repeatType = binding.radioGroupRepeatType.checkedRadioButtonId
            if (repeatType == R.id.radio_interval_hours_minutes) {
                val hoursText = binding.editHours.text.toString().trim()
                val minutesText = binding.editMinutes.text.toString().trim()
                
                val hours = if (hoursText.isNotEmpty()) hoursText.toLongOrNull() ?: 0 else 0
                val minutes = if (minutesText.isNotEmpty()) minutesText.toLongOrNull() ?: 0 else 0
                
                if (hours == 0L && minutes == 0L) {
                    Toast.makeText(requireContext(), "Vui lòng nhập ít nhất 1 phút hoặc 1 giờ", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Chuyển đổi giờ thành phút và cộng với phút
                repeatInterval = hours * 60 + minutes
            }
        }
        
        val note = if (editingNote != null) {
            // Cập nhật ghi chú hiện có
            editingNote!!.copy(
                content = content,
                noteType = noteType,
                repeatInterval = repeatInterval,
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
                repeatInterval = repeatInterval,
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
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
    
    private fun updateDateTimeDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val dateText = if (selectedDate != null) {
            if (binding.radioLunarCalendar.isChecked) {
                LunarCalendarUtils.formatDateWithLunar(selectedDate!!.time)
            } else {
                dateFormat.format(selectedDate!!.time)
            }
        } else "Chọn ngày"
        
        val timeText = if (selectedTime != null) timeFormat.format(selectedTime!!.time) else "Chọn giờ"
        
        binding.textSelectedDatetime.text = "$dateText - $timeText"
    }
    
    /**
     * Thiết lập thời gian nhanh dựa trên số phút từ thời điểm hiện tại
     * @param minutes Số phút cần thêm vào thời gian hiện tại
     */
    private fun setQuickTimeOption(minutes: Int) {
        // Đảm bảo đã chọn loại nhắc nhở
        if (binding.radioGroupNoteType.checkedRadioButtonId == R.id.radio_regular_note) {
            binding.radioOneTimeReminder.isChecked = true
        }
        
        // Thiết lập ngày là ngày hiện tại nếu chưa chọn
        if (selectedDate == null) {
            selectedDate = Calendar.getInstance()
        }
        
        // Thiết lập thời gian là thời gian hiện tại + số phút đã chọn
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, minutes)
        
        selectedTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
        }
        
        // Cập nhật hiển thị
        updateDateTimeDisplay()
        
        // Hiển thị thông báo
        Toast.makeText(
            requireContext(), 
            "Đã thiết lập nhắc nhở sau $minutes phút", 
            Toast.LENGTH_SHORT
        ).show()
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
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn ngày và giờ nhắc nhở", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Kết hợp ngày và giờ đã chọn
        val reminderCalendar = Calendar.getInstance().apply {
            time = selectedDate!!.time
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Xác định loại lặp lại
        var repeatType = when (binding.radioGroupRepeatType.checkedRadioButtonId) {
            R.id.radio_interval_hours_minutes -> {
                // Phân biệt minutely và hourly dựa trên input
                val hours = binding.editHours.text.toString().toLongOrNull() ?: 0
                val minutes = binding.editMinutes.text.toString().toLongOrNull() ?: 0
                
                if (hours > 0 && minutes > 0) {
                    "minutely" // Có cả giờ và phút -> dùng minutely
                } else if (hours > 0) {
                    "hourly" // Chỉ có giờ -> dùng hourly
                } else {
                    "minutely" // Chỉ có phút -> dùng minutely
                }
            }
            R.id.radio_daily -> "daily"
            R.id.radio_weekly -> "weekly"
            R.id.radio_monthly -> {
                if (binding.radioLunarCalendar.isChecked) "lunar_monthly" else "solar_monthly"
            }
            R.id.radio_yearly -> {
                if (binding.radioLunarCalendar.isChecked) "lunar_yearly" else "solar_yearly"
            }
            else -> null
        }
        
        // Tính interval cho loại minutely/hourly (để tương thích với code cũ)
        var intervalSeconds: Long? = null
        if (repeatType == "minutely" || repeatType == "hourly") {
            val hours = binding.editHours.text.toString().toLongOrNull() ?: 0
            val minutes = binding.editMinutes.text.toString().toLongOrNull() ?: 0
            intervalSeconds = hours * 3600 + minutes * 60
            
            if (intervalSeconds <= 0) {
                Toast.makeText(requireContext(), "Vui lòng nhập khoảng thời gian hợp lệ", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Tạo reminder
        val reminder = Reminder(
            id = UUID.randomUUID(),
            noteId = note.id,
            remindAt = reminderCalendar.time,
            repeatType = repeatType,
            repeatIntervalSeconds = intervalSeconds,
            repeatDay = if (repeatType?.contains("monthly") == true || repeatType?.contains("yearly") == true) {
                reminderCalendar.get(Calendar.DAY_OF_MONTH)
            } else null,
            repeatTime = if (repeatType != null && repeatType != "minutely" && repeatType != "hourly") {
                String.format("%02d:%02d", 
                    reminderCalendar.get(Calendar.HOUR_OF_DAY),
                    reminderCalendar.get(Calendar.MINUTE)
                )
            } else null,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // Lưu reminder bằng ViewModel phù hợp
        if (remindersViewModel != null) {
            remindersViewModel!!.insertReminder(reminder)
        } else {
            // Nếu không có remindersViewModel, tạo một instance tạm thời
            val tempViewModel = ViewModelProvider(requireActivity()).get(RemindersViewModel::class.java)
            tempViewModel.insertReminder(reminder)
        }
        
        Toast.makeText(requireContext(), "Đã tạo nhắc nhở thành công", Toast.LENGTH_SHORT).show()
    }
}