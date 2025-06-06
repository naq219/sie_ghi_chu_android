package quangan.sreminder.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import quangan.sreminder.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReminderAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit,
    private val onCompleteClick: (Note, Reminder) -> Unit,
    private val onToggleReminderActive: (Reminder, Boolean) -> Unit
) : ListAdapter<Note, ReminderAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    private val reminderMap = mutableMapOf<String, List<Reminder>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val note = getItem(position)
        val reminders = reminderMap[note.id.toString()] ?: emptyList()
        holder.bind(note, reminders.firstOrNull())
    }

    fun setReminders(noteId: String, reminders: List<Reminder>) {
        reminderMap[noteId] = reminders
        notifyDataSetChanged() // Trong ứng dụng thực tế, bạn nên sử dụng notifyItemChanged để tối ưu hiệu suất
    }

    inner class ReminderViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }
        
        fun bind(note: Note, reminder: Reminder?) {
            // Hiển thị nội dung nhắc nhở
            binding.textReminderContent.text = note.content
            
            // Hiển thị thông tin loại nhắc nhở và thời gian nhắc nhở tiếp theo
            if (reminder != null) {
                // Format thời gian khác nhau cho reminder một lần và lặp lại
                val timeFormat = if (reminder.repeatType == "none" || reminder.repeatType.isNullOrEmpty()) {
                    SimpleDateFormat("dd/MM/yyyy HH'h'mm", Locale.getDefault())
                } else {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                }
                
                val nextReminderTime = if (reminder.remindAt != null) {
                    timeFormat.format(reminder.remindAt)
                } else {
                    "Không có thời gian"
                }
                
                val reminderInfo = when (reminder.repeatType) {
                    "interval" -> {
                        val seconds = reminder.repeatIntervalSeconds ?: 0
                        when {
                            seconds < 60 -> "Lặp mỗi $seconds giây - $nextReminderTime"
                            seconds < 3600 -> "Lặp mỗi ${seconds / 60} phút - $nextReminderTime"
                            seconds < 86400 -> "Lặp mỗi ${seconds / 3600} giờ - $nextReminderTime"
                            else -> "Lặp mỗi ${seconds / 86400} ngày - $nextReminderTime"
                        }
                    }
                    "minutely" -> {
                        // Lấy repeatInterval từ Note
                        val minutes = note.repeatInterval
                        when {
                            minutes < 60 -> "Lặp mỗi $minutes phút - $nextReminderTime"
                            minutes % 60 == 0L -> "Lặp mỗi ${minutes / 60} giờ - $nextReminderTime"
                            else -> {
                                val hours = minutes / 60
                                val remainingMinutes = minutes % 60
                                "Lặp mỗi ${hours}h${remainingMinutes}p - $nextReminderTime"
                            }
                        }
                    }
                    "hourly" -> {
                        // Lấy repeatInterval từ Note
                        val minutes = note.repeatInterval
                        when {
                            minutes < 60 -> "Lặp mỗi $minutes phút - $nextReminderTime"
                            minutes % 60 == 0L -> "Lặp mỗi ${minutes / 60} giờ - $nextReminderTime"
                            else -> {
                                val hours = minutes / 60
                                val remainingMinutes = minutes % 60
                                "Lặp mỗi ${hours}h${remainingMinutes}p - $nextReminderTime"
                            }
                        }
                    }
                    "daily" -> "Lặp 1 ngày - $nextReminderTime"
                    "weekly" -> "Lặp 1 tuần - $nextReminderTime"
                    "solar_monthly" -> "Lặp hàng tháng (dương lịch) - $nextReminderTime"
                    "lunar_monthly" -> "Lặp hàng tháng (âm lịch) - $nextReminderTime"
                    "solar_yearly" -> "Lặp hàng năm (dương lịch) - $nextReminderTime"
                    "lunar_yearly" -> "Lặp hàng năm (âm lịch) - $nextReminderTime"
                    else -> nextReminderTime // Nhắc một lần chỉ hiển thị thời gian
                }
                binding.textReminderTime.text = reminderInfo
                
                // Cài đặt trạng thái switch với xử lý tốt hơn cho RecyclerView
                binding.switchReminderEnabled.setOnCheckedChangeListener(null)
                binding.switchReminderEnabled.isChecked = reminder.isActive
                
                // Xử lý sự kiện khi switch thay đổi
                binding.switchReminderEnabled.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != reminder.isActive) {
                        onToggleReminderActive(reminder, isChecked)
                    }
                }
            } else {
                binding.textReminderTime.text = "Nhắc nhở"
                binding.switchReminderEnabled.isChecked = false
                binding.switchReminderEnabled.setOnCheckedChangeListener(null)
            }
        }
        

    }

    class ReminderDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}