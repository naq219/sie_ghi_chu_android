package quangan.sreminder.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import quangan.sreminder.databinding.FragmentSettingsBinding
import quangan.sreminder.utils.BackupManager
import quangan.sreminder.utils.ShortcutManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var backupManager: BackupManager
    private lateinit var shortcutManager: ShortcutManager
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        
        // Khởi tạo các manager
        backupManager = BackupManager(requireContext())
        shortcutManager = ShortcutManager(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0)
        
        setupViews()
        setupClickListeners()
        loadSettings()
        
        return root
    }
    
    private fun setupViews() {
        // Load user ID từ SharedPreferences
        val savedUserId = sharedPreferences.getString("user_id", "")
        binding.editUserId.setText(savedUserId)
        
        // Load GitHub token từ SharedPreferences
        val savedGithubToken = sharedPreferences.getString("github_token", "")
        binding.editGithubToken.setText(savedGithubToken)
    }
    
    private fun setupClickListeners() {
        // Lưu User ID
        binding.btnSaveUserId.setOnClickListener {
            val userId = binding.editUserId.text.toString().trim()
            if (userId.isNotEmpty()) {
                sharedPreferences.edit().putString("user_id", userId).apply()
                Toast.makeText(context, "Đã lưu User ID: $userId", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Vui lòng nhập User ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Lưu GitHub Token
        binding.btnSaveGithubToken.setOnClickListener {
            val githubToken = binding.editGithubToken.text.toString().trim()
            if (githubToken.isNotEmpty()) {
                sharedPreferences.edit().putString("github_token", githubToken).apply()
                Toast.makeText(context, "Đã lưu GitHub Token thành công", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Vui lòng nhập GitHub Token", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Backup dữ liệu
        binding.btnBackup.setOnClickListener {
            val userId = binding.editUserId.text.toString().trim()
            if (userId.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập User ID trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.btnBackup.isEnabled = false
            binding.progressBackup.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    val success = backupManager.backupToGist(userId)
                    if (success) {
                        Toast.makeText(context, "Backup thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Backup thất bại!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi backup: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.btnBackup.isEnabled = true
                    binding.progressBackup.visibility = View.GONE
                }
            }
        }
        
        // Restore dữ liệu
        binding.btnRestore.setOnClickListener {
            val userId = binding.editUserId.text.toString().trim()
            if (userId.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập User ID trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.btnRestore.isEnabled = false
            binding.progressRestore.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    val success = backupManager.restoreFromGist(userId)
                    if (success) {
                        Toast.makeText(context, "Restore thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Restore thất bại!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi restore: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.btnRestore.isEnabled = true
                    binding.progressRestore.visibility = View.GONE
                }
            }
        }
        
        // Tạo shortcut
        binding.btnCreateShortcut.setOnClickListener {
            try {
                shortcutManager.createNoteShortcut()
                Toast.makeText(context, "Đã tạo shortcut thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi tạo shortcut: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadSettings() {
        // Load các setting khác nếu cần
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}