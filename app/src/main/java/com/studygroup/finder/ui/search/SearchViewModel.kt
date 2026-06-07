package com.studygroup.finder.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.core.utils.ErrorHandler
import com.studygroup.finder.core.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Available category filters for the search screen.
 */
enum class SearchCategory(val label: String) {
    ALL("All"),
    ENGINEERING("Engineering"),
    SCIENCE("Science"),
    MATHEMATICS("Mathematics"),
    LITERATURE("Literature"),
    TECHNOLOGY("Technology")
}

/**
 * ViewModel for the Search screen.
 *
 * Debounces the user's search query by 300 ms, fetches all groups from
 * Firestore via [StudyGroupRepository], and applies local filtering by
 * name/subject (case-insensitive) and selected category.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val studyGroupRepository: StudyGroupRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    // ── Search query ───────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── Selected category filter ───────────────────
    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    // ── Loading indicator ──────────────────────────
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Error state ────────────────────────────────
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Debounced query that drives the search results.
     * Emits after 300 ms of inactivity.
     */
    private val debouncedQuery = _searchQuery
        .debounce(300L)
        .onEach { 
            _isLoading.value = true
            _errorMessage.value = null
        }

    /**
     * Live search results — filtered by the debounced query text
     * (matches name OR subject, case-insensitive) and the selected category.
     */
    val searchResults: StateFlow<List<StudyGroup>> = combine(
        debouncedQuery.flatMapLatest { query ->
            if (!networkUtils.isNetworkAvailable()) {
                _errorMessage.value = "No internet connection. Please check your network settings and try again."
            }
            studyGroupRepository.getAllGroupsFlow()
        },
        debouncedQuery,
        _selectedCategory
    ) { allGroups, query, category ->
        val filtered = allGroups.filter { group ->
            val matchesQuery = query.isBlank() ||
                group.name.contains(query, ignoreCase = true) ||
                group.subject.contains(query, ignoreCase = true)

            val matchesCategory = category == SearchCategory.ALL ||
                group.subject.equals(category.label, ignoreCase = true)

            matchesQuery && matchesCategory
        }
        _isLoading.value = false
        filtered
    }
        .catch { throwable ->
            _isLoading.value = false
            _errorMessage.value = ErrorHandler.getErrorMessage(throwable, networkUtils)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Public actions ─────────────────────────────

    /**
     * Called each time the user types into the search field.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _isLoading.value = true
    }

    /**
     * Called when the user taps a category filter chip.
     */
    fun onCategorySelected(category: SearchCategory) {
        _selectedCategory.value = category
        _isLoading.value = true
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refresh() {
        _isLoading.value = true
        _errorMessage.value = null
        val current = _searchQuery.value
        _searchQuery.value = ""
        _searchQuery.value = current
    }
}
