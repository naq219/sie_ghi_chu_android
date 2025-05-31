package quangan.sreminder.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Cài đặt ứng dụng"
    }
    val text: LiveData<String> = _text
    
    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId
    
    private val _isBackupInProgress = MutableLiveData<Boolean>(false)
    val isBackupInProgress: LiveData<Boolean> = _isBackupInProgress
    
    private val _isRestoreInProgress = MutableLiveData<Boolean>(false)
    val isRestoreInProgress: LiveData<Boolean> = _isRestoreInProgress
    
    fun setUserId(userId: String) {
        _userId.value = userId
    }
    
    fun setBackupInProgress(inProgress: Boolean) {
        _isBackupInProgress.value = inProgress
    }
    
    fun setRestoreInProgress(inProgress: Boolean) {
        _isRestoreInProgress.value = inProgress
    }
}