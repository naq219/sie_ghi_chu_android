package quangan.sreminder

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import quangan.sreminder.data.DemoDataGenerator
import quangan.sreminder.databinding.ActivityMainBinding
import quangan.sreminder.ui.notes.AddNoteDialog
import quangan.sreminder.service.ReminderService
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onResume() {
        super.onResume()
        logIntent(intent)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logIntent(intent)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        
        // Ẩn action bar
        supportActionBar?.hide()
        
        // Tạo dữ liệu demo
        DemoDataGenerator(this).generateDemoData()
        
        // Khởi động service nhắc nhở
        ReminderService.startService(this)
        
        // Kiểm tra xem có được mở từ shortcut không để cho hiển thị dialog tạo ghi chú
        checkShortcutIntent()
    }
    
    private fun checkShortcutIntent() {
        if (intent?.action == "CREATE_NOTE_SHORTCUT") {
            // Hiển thị dialog tạo ghi chú từ shortcut
            val addNoteDialog = AddNoteDialog.newInstance(null, useRemindersViewModel = false)

            addNoteDialog.show(supportFragmentManager, "AddNoteDialog")
        }
    }
    
    // Phương thức public để các fragment có thể gọi khi tạo note từ home screen
    fun showAddNoteDialog() {
        val addNoteDialog = AddNoteDialog.newInstance(null, useRemindersViewModel = false)
        addNoteDialog.show(supportFragmentManager, "AddNoteDialog")
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        logIntent(intent)

        setIntent(intent)
        checkShortcutIntent()
    }

    private fun logIntent(intent: Intent?) {
        if (intent == null) {
            android.util.Log.d("naq", "Intent is null")
            return
        }

        val sb = StringBuilder()
        sb.append("Action: ${intent.action}\n")
        sb.append("Data: ${intent.data}\n")
        sb.append("Flags: ${intent.flags}\n")

        val extras = intent.extras
        if (extras != null) {
            sb.append("Extras:\n")
            for (key in extras.keySet()) {
                sb.append("  $key = ${extras[key]}\n")
            }
        } else {
            sb.append("Extras: null\n")
        }

        android.util.Log.d("IntentLog", sb.toString())
    }


    override fun onPause() {
        super.onPause()
        // Tự động lưu ghi chú khi người dùng bấm nút home hoặc thoát ứng dụng
        saveCurrentNoteIfExists()
    }
    
    private fun saveCurrentNoteIfExists() {
        // Tìm dialog AddNoteDialog đang mở và lưu ghi chú
        val addNoteDialog = supportFragmentManager.findFragmentByTag("AddNoteDialog") as? AddNoteDialog
        addNoteDialog?.let {
            // Gọi phương thức lưu ghi chú nếu dialog đang mở
            it.saveNoteFromActivity()
        }
    }
}