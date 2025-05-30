package quangan.sreminder.ui.adapters

import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import quangan.sreminder.R
import quangan.sreminder.data.entity.Note
import quangan.sreminder.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }
        
        fun bind(note: Note) {
            // Hiển thị nội dung ghi chú với dòng đầu in đậm
            val firstLineBreak = note.content?.indexOf('\n')
            val firstLine = if (firstLineBreak != -1) firstLineBreak?.let {
                note.content?.substring(0, it)
            } else note.content
            
            val spannable = SpannableString(note.content)
            if (firstLine != null) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    firstLine.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.textNoteContent.text = spannable
            
            // Hiển thị thông tin phụ (có thể là loại ghi chú hoặc thông tin khác)

        }
        

    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}