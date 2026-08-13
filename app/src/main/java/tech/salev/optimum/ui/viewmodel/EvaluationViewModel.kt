package tech.salev.optimum.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.salev.optimum.data.model.DailyEvaluation
import tech.salev.optimum.data.repository.OptimumRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EvaluationViewModel @Inject constructor(
    private val repository: OptimumRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }
    private fun handleError(t: Throwable) {
        _errorMessage.value = "Bir hata oluştu: ${t.localizedMessage}"
    }

    private val _currentDate = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    fun setSelectedDate(date: String) { _currentDate.value = date }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentEvaluation: StateFlow<DailyEvaluation?> = _currentDate
        .flatMapLatest { date -> repository.getEvaluationForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allEvaluations: StateFlow<ImmutableList<DailyEvaluation>> = repository.getAllEvaluations()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun saveEvaluation(rating: Int, journalNote: String, date: String? = null, mood: Int = 0) {
        viewModelScope.launch {
            runCatching {
                repository.saveEvaluation(
                    DailyEvaluation(date = date ?: _currentDate.value,
                                   rating = rating, mood = mood, journalNote = journalNote)
                )
            }.onFailure { handleError(it) }
        }
    }

    fun deleteEvaluation(evaluation: DailyEvaluation) {
        viewModelScope.launch {
            runCatching {
                repository.deleteEvaluation(evaluation)
            }.onFailure { handleError(it) }
        }
    }
}
