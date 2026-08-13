package tech.salev.optimum.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.UserProfile
import tech.salev.optimum.data.repository.AuthRepository
import tech.salev.optimum.data.repository.SyncRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val _syncStateMessage = MutableStateFlow<String?>(null)
    val syncStateMessage: StateFlow<String?> = _syncStateMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun signInWithGoogle(webClientId: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStateMessage.value = "Google hesabına bağlanılıyor..."
            val result = authRepository.signInWithGoogle(context, webClientId)
            result.onSuccess { profile ->
                _syncStateMessage.value = "Hoş geldin, ${profile.displayName}!"
                // Trigger auto sync after login
                syncCloudData()
            }.onFailure { err ->
                _syncStateMessage.value = "Giriş hatası: ${err.localizedMessage}"
            }
            _isSyncing.value = false
        }
    }

    fun syncCloudData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStateMessage.value = "Veriler bulut veritabanına eşitleniyor..."
            val result = syncRepository.performCloudSync()
            result.onSuccess { msg ->
                _syncStateMessage.value = msg
            }.onFailure { err ->
                _syncStateMessage.value = "Senkronizasyon hatası: ${err.localizedMessage}"
            }
            _isSyncing.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _syncStateMessage.value = "Çıkış yapıldı."
        }
    }
}
