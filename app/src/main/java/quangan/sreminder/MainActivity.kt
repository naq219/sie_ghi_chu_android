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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        
        // Ẩn action bar
        supportActionBar?.hide()
        
        // Tạo dữ liệu demo
        DemoDataGenerator(this).generateDemoData()
        
        // Kiểm tra xem có được mở từ shortcut không để cho hiển thị dialog tạo ghi chú
        checkShortcutIntent()
    }
    
    private fun checkShortcutIntent() {
        if (intent?.action == "CREATE_NOTE_SHORTCUT") {
            // Hiển thị dialog tạo ghi chú
            val addNoteDialog = AddNoteDialog.newInstance(null, useRemindersViewModel = false)
            addNoteDialog.show(supportFragmentManager, "AddNoteDialog")
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkShortcutIntent()
    }
}