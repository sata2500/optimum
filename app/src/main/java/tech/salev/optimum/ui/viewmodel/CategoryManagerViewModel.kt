package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.ActivityItem
import tech.salev.optimum.data.model.Category
import tech.salev.optimum.data.repository.OptimumRepository
import javax.inject.Inject

@HiltViewModel
class CategoryManagerViewModel @Inject constructor(
    private val repository: OptimumRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }
    private fun handleError(t: Throwable) {
        _errorMessage.value = "Bir hata oluştu: ${t.localizedMessage}"
    }

    val categories: StateFlow<ImmutableList<Category>> = repository.allCategories
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val categoriesWithActivities: StateFlow<ImmutableList<tech.salev.optimum.data.model.CategoryWithActivities>> = repository.allCategoriesWithActivities
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    val activities: StateFlow<ImmutableList<ActivityItem>> = repository.allActivities
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    // ── Category CRUD ────────────────────────────────────────────────────────

    fun addCategory(name: String, code: String, colorHex: String, isProductive: Boolean, iconName: String = "Category") {
        viewModelScope.launch {
            runCatching {
                repository.insertCategory(
                    Category(name = name, code = code, colorHex = colorHex,
                             isProductive = isProductive, iconName = iconName)
                )
            }.onFailure { handleError(it) }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            runCatching { repository.updateCategory(category) }.onFailure { handleError(it) }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            runCatching { repository.deleteCategory(category) }.onFailure { handleError(it) }
        }
    }

    fun reorderCategories(reorderedList: List<Category>) {
        viewModelScope.launch {
            runCatching {
                reorderedList.forEachIndexed { index, category ->
                    if (category.displayOrder != index) {
                        repository.updateCategory(category.copy(displayOrder = index))
                    }
                }
            }.onFailure { handleError(it) }
        }
    }

    // ── Activity CRUD ────────────────────────────────────────────────────────

    fun addActivity(categoryId: Long, name: String, description: String = "", shortCode: String = "", colorHex: String = "#FFD700") {
        viewModelScope.launch {
            runCatching {
                val nextNumber = repository.getMaxActivityNumberForCategory(categoryId)
                val finalShortCode = if (shortCode.isNotBlank()) shortCode else name.trim().take(1).lowercase()
                repository.insertActivity(
                    ActivityItem(categoryId = categoryId, name = name,
                                 activityNumber = nextNumber, description = description,
                                 shortCode = finalShortCode, colorHex = colorHex)
                )
            }.onFailure { handleError(it) }
        }
    }

    fun updateActivity(activity: ActivityItem) {
        viewModelScope.launch {
            runCatching { repository.updateActivity(activity) }.onFailure { handleError(it) }
        }
    }

    fun deleteActivity(activity: ActivityItem) {
        viewModelScope.launch {
            runCatching { repository.deleteActivity(activity) }.onFailure { handleError(it) }
        }
    }

    fun reorderActivities(reorderedList: List<ActivityItem>) {
        viewModelScope.launch {
            runCatching {
                reorderedList.forEachIndexed { index, activity ->
                    if (activity.displayOrder != index) {
                        repository.updateActivity(activity.copy(displayOrder = index))
                    }
                }
            }.onFailure { handleError(it) }
        }
    }
}
