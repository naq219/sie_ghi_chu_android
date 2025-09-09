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
            //createDynamicShortcut()
        }
        else {
            Toast.makeText(context,"sdk thâp ",Toast.LENGTH_LONG).show()
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O) // API 26+
//    private fun createPinnedShortcut() {
//        val shortcutManager = getSystemService(ShortcutManager::class.java)
//
//        if (shortcutManager.isRequestPinShortcutSupported) {
//            val shortcutInfo = ShortcutInfo.Builder(this, "pinned_note")
//                .setShortLabel("Tạo ghi chú")
//                .setLongLabel("Mở nhanh tạo ghi chú")
//                .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
//                .setIntent(Intent(this, MainActivity::class.java).apply {
//                    action = Intent.ACTION_VIEW
//                })
//                .build()
//
//            val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
//
//            val successCallback = PendingIntent.getBroadcast(
//                this, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
//            )
//
//            shortcutManager.requestPinShortcut(shortcutInfo, successCallback.intentSender)
//        }
//    }


//    @RequiresApi(Build.VERSION_CODES.N_MR1)
//    private fun createDynamicShortcut() {
//        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
//
//        val intent = Intent(context, MainActivity::class.java).apply {
//            action = Intent.ACTION_VIEW   // hoặc "CREATE_NOTE_SHORTCUT" nếu bạn handle riêng
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val shortcut = ShortcutInfo.Builder(context, "create_note")
//            .setShortLabel(context.getString(R.string.create_note))
//            .setLongLabel(context.getString(R.string.create_new_note))
//            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
//            .setIntent(intent)
//            .build()
//
//        // Nếu muốn chỉ có shortcut này
//        // shortcutManager?.dynamicShortcuts = listOf(shortcut)
//
//        // Nếu muốn thêm mà không xoá cái cũ
////        shortcutManager?.addDynamicShortcuts(listOf(shortcut))
//    }
}