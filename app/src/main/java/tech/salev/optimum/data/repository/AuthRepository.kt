package tech.salev.optimum.data.repository

import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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
        private const val DEFAULT_WEB_CLIENT_ID = "859090591444-gks1ocsevkb8kdcltbeoe24gi5lbo3pd.apps.googleusercontent.com"
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
        val serverClientId = webClientId ?: DEFAULT_WEB_CLIENT_ID
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            processCredentialResult(result.credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGoogleSignInIntent(context: Context, webClientId: String? = null): Intent {
        val serverClientId = webClientId ?: DEFAULT_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    suspend fun processLegacySignInResult(intentData: Intent?): Result<UserProfile> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intentData)
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.email != null) {
                account.idToken?.let { token ->
                    try {
                        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)
                        com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential).await()
                    } catch (fe: Exception) {
                        // Firebase auth log or fallback
                    }
                }

                val profile = UserProfile(
                    id = account.id ?: account.email!!,
                    email = account.email!!,
                    displayName = account.displayName ?: account.email!!.substringBefore("@"),
                    photoUrl = account.photoUrl?.toString(),
                    isLoggedIn = true,
                    lastSyncTime = null,
                    isSyncing = false,
                    syncMessage = "Google hesabı bağlandı: ${account.email}"
                )
                saveUserProfile(profile)
                Result.success(profile)
            } else {
                Result.failure(Exception("Google hesabına erişilemedi."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processCredentialResult(credential: androidx.credentials.Credential): Result<UserProfile> {
        return if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val realEmail = googleIdTokenCredential.id
            val realName = googleIdTokenCredential.displayName ?: realEmail.substringBefore("@")
            val realPhoto = googleIdTokenCredential.profilePictureUri?.toString()

            val idToken = googleIdTokenCredential.idToken
            if (idToken.isNotEmpty()) {
                try {
                    val firebaseCred = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
                } catch (fe: Exception) {
                    // Log or handle Firebase Auth exception
                }
            }

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
    }
}
