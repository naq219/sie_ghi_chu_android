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
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.buttonMore.setOnClickListener { view ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(view, getItem(position))
                }
            }
            
            binding.buttonComplete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val note = getItem(position)
                    val reminders = reminderMap[note.id.toString()] ?: emptyList()
                    if (reminders.isNotEmpty()) {
                        onCompleteClick(note, reminders.first())
                    }
                }
            }
        }
        
        fun bind(note: Note, reminder: Reminder?) {
            binding.textReminderTitle.text = note.title
            binding.textReminderContent.text = note.content
            
            if (reminder != null) {
                binding.textReminderTime.text = "Nhắc lúc: ${dateFormat.format(reminder.remindAt)}"
                
                // Hiển thị thông tin lặp lại
                when (reminder.repeatType) {
                    "interval" -> {
                        val seconds = reminder.repeatIntervalSeconds ?: 0
                        val repeatText = when {
                            seconds < 60 -> "Lặp lại: Mỗi $seconds giây"
                            seconds < 3600 -> "Lặp lại: Mỗi ${seconds / 60} phút"
                            seconds < 86400 -> "Lặp lại: Mỗi ${seconds / 3600} giờ"
                            else -> "Lặp lại: Mỗi ${seconds / 86400} ngày"
                        }
                        binding.textReminderRepeat.text = repeatText
                        binding.textReminderRepeat.visibility = View.VISIBLE
                    }
                    "solar_monthly" -> {
                        binding.textReminderRepeat.text = "Lặp lại: Ngày ${reminder.repeatDay} hàng tháng (dương lịch)"
                        binding.textReminderRepeat.visibility = View.VISIBLE
                    }
                    "lunar_monthly" -> {
                        binding.textReminderRepeat.text = "Lặp lại: Ngày ${reminder.repeatDay} hàng tháng (âm lịch)"
                        binding.textReminderRepeat.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.textReminderRepeat.visibility = View.GONE
                    }
                }
                
                // Hiển thị nút hoàn thành
                binding.buttonComplete.visibility = if (reminder.isActive) View.VISIBLE else View.GONE
            } else {
                binding.textReminderTime.text = "Không có thông tin nhắc nhở"
                binding.textReminderRepeat.visibility = View.GONE
                binding.buttonComplete.visibility = View.GONE
            }
        }
        
        private fun showPopupMenu(view: View, note: Note) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_reminder_options, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onItemClick(note)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(note)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
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