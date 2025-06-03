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
    private val onCompleteClick: (Note, Reminder) -> Unit
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
            
            // Hiển thị thông tin phụ (loại nhắc nhở hoặc thông tin khác)
            if (reminder != null) {
                val reminderInfo = when (reminder.repeatType) {
                    "interval" -> {
                        val seconds = reminder.repeatIntervalSeconds ?: 0
                        when {
                            seconds < 60 -> "Lặp mỗi $seconds giây"
                            seconds < 3600 -> "Lặp mỗi ${seconds / 60} phút"
                            seconds < 86400 -> "Lặp mỗi ${seconds / 3600} giờ"
                            else -> "Lặp mỗi ${seconds / 86400} ngày"
                        }
                    }
                    "minutely" -> {
                        // Lấy repeatInterval từ Note
                        val minutes = note.repeatInterval
                        when {
                            minutes < 60 -> "Lặp mỗi $minutes phút"
                            minutes % 60 == 0L -> "Lặp mỗi ${minutes / 60} giờ"
                            else -> {
                                val hours = minutes / 60
                                val remainingMinutes = minutes % 60
                                "Lặp mỗi ${hours}h${remainingMinutes}p"
                            }
                        }
                    }
                    "hourly" -> {
                        // Lấy repeatInterval từ Note
                        val minutes = note.repeatInterval
                        when {
                            minutes < 60 -> "Lặp mỗi $minutes phút"
                            minutes % 60 == 0L -> "Lặp mỗi ${minutes / 60} giờ"
                            else -> {
                                val hours = minutes / 60
                                val remainingMinutes = minutes % 60
                                "Lặp mỗi ${hours}h${remainingMinutes}p"
                            }
                        }
                    }
                    "solar_monthly" -> "Lặp hàng tháng (dương lịch)"
                    "lunar_monthly" -> "Lặp hàng tháng (âm lịch)"
                    else -> "Nhắc một lần"
                }
                binding.textReminderTime.text = reminderInfo
            } else {
                binding.textReminderTime.text = "Nhắc nhở"
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