package quangan.sreminder.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager as AndroidShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import quangan.sreminder.MainActivity
import quangan.sreminder.R

class ShortcutManager(private val context: Context) {
    
    fun createNoteShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createDynamicShortcut()
        }
        else {
            Toast.makeText(context,"sdk thâp ",Toast.LENGTH_LONG).show()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createDynamicShortcut() {
        val shortcutManager = context.getSystemService(AndroidShortcutManager::class.java)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "CREATE_NOTE_SHORTCUT"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val shortcut = ShortcutInfo.Builder(context, "create_note")
            .setShortLabel(context.getString(R.string.create_note))
            .setLongLabel(context.getString(R.string.create_new_note))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setIntent(intent)
            .build()
        
        shortcutManager?.dynamicShortcuts = listOf(shortcut)
    }
}