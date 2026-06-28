package app.btccompass.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.btccompass.android.domain.model.ScoreSnapshot
import app.btccompass.android.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val snapshot: ScoreSnapshot) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: ScoreRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Drive UI from the cache flow; successful network fetches write through and update UI.
        viewModelScope.launch {
            repository.observeCachedScore()
                .filterNotNull()
                .collect { snapshot -> _uiState.value = HomeUiState.Success(snapshot) }
        }
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            // Show loading spinner only when there is nothing cached yet.
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }
            try {
                repository.getScore() // write-through to cache → cache Flow updates UI
            } catch (e: Exception) {
                // Keep cached data visible on error; only show error banner when cache is empty.
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }
}
