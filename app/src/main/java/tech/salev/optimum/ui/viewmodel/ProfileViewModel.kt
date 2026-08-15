package tech.salev.optimum.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.credentials.exceptions.GetCredentialCancellationException
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
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = authRepository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val _syncStateMessage = MutableStateFlow<String?>(null)
    val syncStateMessage: StateFlow<String?> = _syncStateMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun signInWithGoogle(
        activityContext: Context,
        onFallbackNeeded: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStateMessage.value = "Google hesabına bağlanılıyor..."

            val result = authRepository.signInWithGoogle(activityContext)
            result.onSuccess { profile ->
                _syncStateMessage.value = "Hoş geldin, ${profile.displayName}!"
                syncCloudData()
                _isSyncing.value = false
            }.onFailure { err ->
                if (err is GetCredentialCancellationException) {
                    _syncStateMessage.value = "Giriş işlemi iptal edildi."
                    _isSyncing.value = false
                    return@launch
                }

                // Fallback to GoogleSignInClient Intent if CredentialManager fails or needs account chooser
                try {
                    val intent = authRepository.getGoogleSignInIntent(activityContext)
                    onFallbackNeeded(intent)
                } catch (fallbackErr: Exception) {
                    _syncStateMessage.value = "Giriş hatası: ${err.localizedMessage ?: "Bilinmeyen hata"}"
                    _isSyncing.value = false
                }
            }
        }
    }

    fun handleLegacySignInResult(intentData: Intent?) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStateMessage.value = "Google hesabı işleniyor..."
            val result = authRepository.processLegacySignInResult(intentData)
            result.onSuccess { profile ->
                _syncStateMessage.value = "Hoş geldin, ${profile.displayName}!"
                syncCloudData()
            }.onFailure { err ->
                _syncStateMessage.value = err.localizedMessage ?: "Giriş işlemi başarısız oldu."
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

    fun cancelSignIn() {
        _isSyncing.value = false
        if (_syncStateMessage.value == "Google hesabına bağlanılıyor..." || _syncStateMessage.value == "Google hesabı işleniyor...") {
            _syncStateMessage.value = "Giriş işlemi iptal edildi."
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _syncStateMessage.value = "Çıkış yapıldı."
        }
    }
}
