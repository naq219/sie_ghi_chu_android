package quangan.sreminder.ui.adapters

import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.TextWatcher
import android.text.Editable
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
import android.os.Handler
import android.os.Looper
import java.util.Date

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit,
    private val onUpdateNote: (Note) -> Unit
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
        private var isEditing = false
        private var originalContent = ""
        private var isExpanded = false
        private var lastClickTime = 0L
        private val doubleClickDelay = 400L
        
        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION && !isEditing) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < doubleClickDelay) {
                        // Double click - kiểm tra số dòng để quyết định edit mode
                        val note = getItem(position)
                        val lineCount = note.content?.split('\n')?.size ?: 0
                        if (lineCount > 7) {
                            // Nếu nhiều hơn 7 dòng, hiển thị dialog edit
                            onItemClick(note)
                        } else {
                            // Nếu ít hơn hoặc bằng 7 dòng, vào edit mode inline
                            enterEditMode(note)
                        }
                    } else {
                        // Single click - toggle hiển thị
                        toggleTextDisplay()
                    }
                    lastClickTime = currentTime
                }
            }
            
            // Xử lý nút Save
            binding.btnSaveEdit.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    saveNote(getItem(position))
                    exitEditMode()
                }
            }
            
            // Xử lý nút Cancel
            binding.btnCancelEdit.setOnClickListener {
                binding.editNoteContent.setText(originalContent)
                exitEditMode()
            }
        }
        
        private fun toggleTextDisplay() {
            isExpanded = !isExpanded
            if (isExpanded) {
                binding.textNoteContent.maxLines = Int.MAX_VALUE
                binding.textNoteContent.ellipsize = null
            } else {
                binding.textNoteContent.maxLines = 2
                binding.textNoteContent.ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }
        
        private fun enterEditMode(note: Note) {
            isEditing = true
            originalContent = note.content ?: ""
            binding.textNoteContent.visibility = View.GONE
            binding.editNoteContent.visibility = View.VISIBLE
            binding.layoutEditButtons.visibility = View.VISIBLE
            binding.editNoteContent.setText(note.content)
            binding.editNoteContent.requestFocus()
            binding.editNoteContent.setSelection(binding.editNoteContent.text.length)
        }
        
        private fun exitEditMode() {
            isEditing = false
            binding.editNoteContent.visibility = View.GONE
            binding.layoutEditButtons.visibility = View.GONE
            binding.textNoteContent.visibility = View.VISIBLE
            binding.editNoteContent.clearFocus()
        }
        

        
        private fun saveNote(note: Note) {
            val newContent = binding.editNoteContent.text.toString()
            if (newContent != note.content) {
                val updatedNote = note.copy(
                    content = newContent,
                    updatedAt = Date()
                )
                onUpdateNote(updatedNote)
            }
        }
        
        fun bind(note: Note) {
            // Reset trạng thái
            isEditing = false
            isExpanded = false
            binding.editNoteContent.visibility = View.GONE
            binding.layoutEditButtons.visibility = View.GONE
            binding.textNoteContent.visibility = View.VISIBLE
            
            // Kiểm tra số dòng trong note
            val lineCount = note.content?.split('\n')?.size ?: 0
            
            // Thiết lập hiển thị mặc định (ngắn)
            binding.textNoteContent.maxLines = 2
            binding.textNoteContent.ellipsize = android.text.TextUtils.TruncateAt.END
            
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