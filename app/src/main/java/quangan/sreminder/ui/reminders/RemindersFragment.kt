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
        val dialogBinding = DialogNoteEditBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(if (note == null) R.string.add_reminder else R.string.edit_reminder)
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
            //dialogBinding.editNoteTitle.setText(note.title)
            dialogBinding.editNoteContent.setText(note.content)
            
            // Thiết lập loại ghi chú
            when (note.noteType) {
                2 -> dialogBinding.radioOneTimeReminder.isChecked = true
                3 -> dialogBinding.radioRepeatingReminder.isChecked = true
                else -> dialogBinding.radioOneTimeReminder.isChecked = true
            }
            
            // Hiển thị các tùy chọn nhắc nhở
            dialogBinding.layoutReminderSettings.visibility = View.VISIBLE
            if (note.noteType == 3) {
                dialogBinding.layoutRepeatSettings.visibility = View.VISIBLE
                
                // Trong ứng dụng thực tế, bạn sẽ tải thông tin nhắc nhở hiện có và thiết lập các tùy chọn tương ứng
                remindersViewModel.getRemindersByNoteId(note.id).observe(viewLifecycleOwner) { reminders ->
                    if (reminders.isNotEmpty()) {
                        val reminder = reminders[0]
                        // Thiết lập các tùy chọn dựa trên thông tin nhắc nhở
                        when (reminder.repeatType) {
                            "interval" -> {
                                dialogBinding.radioInterval.isChecked = true
                                dialogBinding.layoutIntervalSettings.visibility = View.VISIBLE
                                // Tính toán và hiển thị giá trị khoảng thời gian
                                val seconds = reminder.repeatIntervalSeconds ?: 0
                                var unitIndex = 0
                                var value = seconds
                                for (i in intervalMultipliers.indices) {
                                    if (seconds % intervalMultipliers[i] == 0L) {
                                        unitIndex = i
                                        value = seconds / intervalMultipliers[i]
                                    }
                                }
                                dialogBinding.editIntervalValue.setText(value.toString())
                                dialogBinding.spinnerIntervalUnit.setSelection(unitIndex)
                            }
                            "solar_monthly" -> {
                                dialogBinding.radioSolarMonthly.isChecked = true
                                dialogBinding.layoutMonthlySettings.visibility = View.VISIBLE
                                dialogBinding.numberPickerDay.value = reminder.repeatDay ?: 1
                            }
                            "lunar_monthly" -> {
                                dialogBinding.radioLunarMonthly.isChecked = true
                                dialogBinding.layoutMonthlySettings.visibility = View.VISIBLE
                                dialogBinding.numberPickerDay.value = reminder.repeatDay ?: 1
                            }
                        }
                    }
                }
            }
        } else {
            // Nếu đang tạo nhắc nhở mới, mặc định chọn nhắc nhở một lần
            dialogBinding.radioOneTimeReminder.isChecked = true
            dialogBinding.layoutReminderSettings.visibility = View.VISIBLE
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
           // val title = dialogBinding.editNoteTitle.text.toString().trim()
            val content = dialogBinding.editNoteContent.text.toString().trim()
            
//            if (title.isEmpty()) {
//                dialogBinding.layoutNoteTitle.error = "Vui lòng nhập tiêu đề"
//                return@setOnClickListener
//            }
            
            // Xác định loại ghi chú
            val noteType = when (dialogBinding.radioGroupNoteType.checkedRadioButtonId) {
                R.id.radio_one_time_reminder -> 2
                R.id.radio_repeating_reminder -> 3
                else -> 2
            }
            
            // Tạo hoặc cập nhật ghi chú
            val updatedNote = note?.copy(
                title = "title",
                content = content,
                noteType = noteType,
                updatedAt = Date()
            ) ?: Note(
                id = UUID.randomUUID(),
                title = "title",
                content = content,
                noteType = noteType,
                status = "active",
                createdAt = Date(),
                updatedAt = Date()
            )
            
            if (note == null) {
                remindersViewModel.insert(updatedNote)
            } else {
                remindersViewModel.update(updatedNote)
            }
            
            // Tạo hoặc cập nhật nhắc nhở
            // Trong ứng dụng thực tế, bạn sẽ thêm mã để tạo hoặc cập nhật nhắc nhở ở đây
            // dựa trên các tùy chọn người dùng đã chọn
            
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