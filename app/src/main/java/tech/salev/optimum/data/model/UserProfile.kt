package tech.salev.optimum.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val lastSyncTime: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String = "Senkronizasyon bekleniyor"
)
