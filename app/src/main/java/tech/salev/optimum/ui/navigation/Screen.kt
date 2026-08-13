package tech.salev.optimum.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed class Screen {
    abstract val title: String
    abstract val icon: ImageVector

    @Serializable
    data object Onboarding : Screen() {
        override val title = "Hoş Geldin"
        override val icon = Icons.Default.GridView
    }
    @Serializable
    data object Home : Screen() {
        override val title = "Çizelge"
        override val icon = Icons.Default.GridView
    }
    @Serializable
    data object Categories : Screen() {
        override val title = "Kategoriler"
        override val icon = Icons.Default.Category
    }
    @Serializable
    data object Analytics : Screen() {
        override val title = "Analiz"
        override val icon = Icons.Default.Analytics
    }
    @Serializable
    data object Profile : Screen() {
        override val title = "Profil & Bulut"
        override val icon = Icons.Default.Person
    }
    @Serializable
    data object Settings : Screen() {
        override val title = "Ayarlar"
        override val icon = Icons.Default.Settings
    }
}
