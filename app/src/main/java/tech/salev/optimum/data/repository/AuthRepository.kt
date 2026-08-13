package tech.salev.optimum.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.salev.optimum.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("user_is_logged_in")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_display_name")
        private val KEY_USER_PHOTO = stringPreferencesKey("user_photo_url")
        private val KEY_LAST_SYNC = stringPreferencesKey("user_last_sync")
    }

    val userProfileFlow: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            id = prefs[KEY_USER_ID] ?: "",
            email = prefs[KEY_USER_EMAIL] ?: "",
            displayName = prefs[KEY_USER_NAME] ?: "",
            photoUrl = prefs[KEY_USER_PHOTO],
            isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false,
            lastSyncTime = prefs[KEY_LAST_SYNC],
            isSyncing = false,
            syncMessage = if (prefs[KEY_IS_LOGGED_IN] == true) "Bulut senkronizasyonu aktif" else "Giriş yapılmadı"
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = profile.isLoggedIn
            prefs[KEY_USER_ID] = profile.id
            prefs[KEY_USER_EMAIL] = profile.email
            prefs[KEY_USER_NAME] = profile.displayName
            if (profile.photoUrl != null) {
                prefs[KEY_USER_PHOTO] = profile.photoUrl
            } else {
                prefs.remove(KEY_USER_PHOTO)
            }
            if (profile.lastSyncTime != null) {
                prefs[KEY_LAST_SYNC] = profile.lastSyncTime
            }
        }
    }

    suspend fun updateLastSyncTime() {
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr"))
        val nowStr = sdf.format(Date())
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = nowStr
        }
    }

    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_PHOTO)
            prefs.remove(KEY_LAST_SYNC)
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String? = null): Result<UserProfile> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId ?: "859090591444-gks1ocsevkb8kdcltbeoe24gi5lbo3pd.apps.googleusercontent.com")
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val realEmail = googleIdTokenCredential.id
                val realName = googleIdTokenCredential.displayName ?: realEmail.substringBefore("@")
                val realPhoto = googleIdTokenCredential.profilePictureUri?.toString()

                val profile = UserProfile(
                    id = googleIdTokenCredential.id,
                    email = realEmail,
                    displayName = realName,
                    photoUrl = realPhoto,
                    isLoggedIn = true,
                    lastSyncTime = null,
                    isSyncing = false,
                    syncMessage = "Google hesabı bağlandı: $realEmail"
                )
                saveUserProfile(profile)
                Result.success(profile)
            } else {
                Result.failure(IllegalStateException("Google hesabı doğrulanamadı. Lütfen tekrar deneyin."))
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
