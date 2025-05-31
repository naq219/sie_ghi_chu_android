package quangan.sreminder.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import quangan.sreminder.data.AppDatabase
import quangan.sreminder.data.entity.Note
import quangan.sreminder.data.entity.Reminder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.Base64

class BackupManager(private val context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val repoOwner = "naq219"
    private val repoName = "sie_ghi_chu_android"
    private val fileName = "backup_data.json"
    
    private fun getGithubToken(): String {
        return sharedPreferences.getString("github_token", "") ?: ""
    }
    
    private val database = AppDatabase.getDatabase(context)
    
    data class BackupData(
        val notes: List<Note>,
        val reminders: List<Reminder>,
        val timestamp: String
    )
    
    suspend fun createBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val notes = database.noteDao().getAllNotes()
                val reminders = database.reminderDao().getAllReminders()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                
                val backupData = BackupData(notes, reminders, timestamp)
                val jsonData = gson.toJson(backupData)
                
                // Save to local file first
                val backupFile = File(context.getExternalFilesDir(null), "backup_$timestamp.json")
                FileOutputStream(backupFile).use { it.write(jsonData.toByteArray()) }
                
                // Upload to GitHub
                uploadToGitHub(jsonData)
                
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Error creating backup", e)
                false
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun uploadToGitHub(jsonData: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$fileName"
                
                // Get current file SHA if exists
                val getRequest = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val getResponse = client.newCall(getRequest).execute()
                val sha = if (getResponse.isSuccessful) {
                    val responseBody = getResponse.body?.string()
                    val jsonObject = gson.fromJson(responseBody, Map::class.java)
                    jsonObject["sha"] as? String
                } else null
                
                // Create or update file
                val content = Base64.getEncoder().encodeToString(jsonData.toByteArray())
                val requestBody = mutableMapOf(
                    "message" to "Update backup data",
                    "content" to content
                )
                if (sha != null) {
                    requestBody["sha"] = sha
                }
                
                val putRequest = Request.Builder()
                    .url(url)
                    .put(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val putResponse = client.newCall(putRequest).execute()
                putResponse.isSuccessful
            } catch (e: Exception) {
                Log.e("BackupManager", "Error uploading to GitHub", e)
                false
            }
        }
    }
    
    suspend fun restoreBackup(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = downloadFromGitHub()
                if (jsonData != null) {
                    val backupData = gson.fromJson(jsonData, BackupData::class.java)
                    
                    // Clear existing data
                    database.noteDao().deleteAllNotes()
                    database.reminderDao().deleteAllReminders()
                    
                    // Restore data
                    for (note in backupData.notes) {
                        database.noteDao().insert(note)
                    }
                    for (reminder in backupData.reminders) {
                        database.reminderDao().insert(reminder)
                    }
                    
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("BackupManager", "Error restoring backup", e)
                false
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun downloadFromGitHub(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$fileName"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = gson.fromJson(responseBody, Map::class.java)
                    val content = jsonObject["content"] as? String
                    if (content != null) {
                        String(Base64.getDecoder().decode(content.replace("\n", "")))
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e("BackupManager", "Error downloading from GitHub", e)
                null
            }
        }
    }
    
    suspend fun getLocalBackups(): List<File> {
        return withContext(Dispatchers.IO) {
            val backupDir = context.getExternalFilesDir(null)
            backupDir?.listFiles { file -> file.name.startsWith("backup_") && file.name.endsWith(".json") }?.toList() ?: emptyList()
        }
    }
    
    suspend fun restoreFromLocalFile(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = FileInputStream(file).use { it.readBytes().toString(Charset.defaultCharset()) }
                val backupData = gson.fromJson(jsonData, BackupData::class.java)
                
                // Clear existing data
                database.noteDao().deleteAllNotes()
                database.reminderDao().deleteAllReminders()
                
                // Restore data
                for (note in backupData.notes) {
                    database.noteDao().insert(note)
                }
                for (reminder in backupData.reminders) {
                    database.reminderDao().insert(reminder)
                }
                
                true
            } catch (e: Exception) {
                Log.e("BackupManager", "Error restoring from local file", e)
                false
            }
        }
    }
    
    suspend fun backupToGist(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val notes = database.noteDao().getAllNotes()
                val reminders = database.reminderDao().getAllReminders()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                
                val backupData = BackupData(notes, reminders, timestamp)
                val jsonData = gson.toJson(backupData)
                
                // Save to local file first
                val backupFile = File(context.getExternalFilesDir(null), "backup_${userId}_$timestamp.json")
                FileOutputStream(backupFile).use { it.write(jsonData.toByteArray()) }
                
                // Upload to GitHub with user-specific filename
                uploadToGitHubWithUserId(jsonData, userId)
            } catch (e: Exception) {
                Log.e("BackupManager", "Error creating backup to gist", e)
                false
            }
        }
    }
    
    suspend fun restoreFromGist(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = downloadFromGitHubWithUserId(userId)
                if (jsonData != null) {
                    val backupData = gson.fromJson(jsonData, BackupData::class.java)
                    
                    // Clear existing data
                    database.noteDao().deleteAllNotes()
                    database.reminderDao().deleteAllReminders()
                    
                    // Restore data
                    for (note in backupData.notes) {
                        database.noteDao().insert(note)
                    }
                    for (reminder in backupData.reminders) {
                        database.reminderDao().insert(reminder)
                    }
                    
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("BackupManager", "Error restoring from gist", e)
                false
            }
        }
    }
    
    private suspend fun uploadToGitHubWithUserId(jsonData: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val userFileName = "backup_$userId.json"
                val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$userFileName"
                
                // Get current file SHA if exists
                val getRequest = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val getResponse = client.newCall(getRequest).execute()
                val sha = if (getResponse.isSuccessful) {
                    val responseBody = getResponse.body?.string()
                    val jsonObject = gson.fromJson(responseBody, Map::class.java)
                    jsonObject["sha"] as? String
                } else null
                
                // Create or update file
                val content = Base64.getEncoder().encodeToString(jsonData.toByteArray())
                val requestBody = mutableMapOf(
                    "message" to "Update backup data for user $userId",
                    "content" to content
                )
                if (sha != null) {
                    requestBody["sha"] = sha
                }
                
                val putRequest = Request.Builder()
                    .url(url)
                    .put(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val putResponse = client.newCall(putRequest).execute()
                putResponse.isSuccessful
            } catch (e: Exception) {
                Log.e("BackupManager", "Error uploading to GitHub with user ID", e)
                false
            }
        }
    }
    
    private suspend fun downloadFromGitHubWithUserId(userId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val userFileName = "backup_$userId.json"
                val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$userFileName"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "token ${getGithubToken()}")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = gson.fromJson(responseBody, Map::class.java)
                    val content = jsonObject["content"] as? String
                    if (content != null) {
                        String(Base64.getDecoder().decode(content.replace("\n", "")))
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e("BackupManager", "Error downloading from GitHub with user ID", e)
                null
            }
        }
    }
}