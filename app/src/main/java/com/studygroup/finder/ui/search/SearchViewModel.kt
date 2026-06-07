package com.studygroup.finder.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.repository.StudyGroupRepository
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
    private val studyGroupRepository: StudyGroupRepository
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

    /**
     * Debounced query that drives the search results.
     * Emits after 300 ms of inactivity.
     */
    private val debouncedQuery = _searchQuery
        .debounce(300L)
        .onEach { _isLoading.value = true }

    /**
     * Live search results — filtered by the debounced query text
     * (matches name OR subject, case-insensitive) and the selected category.
     */
    val searchResults: StateFlow<List<StudyGroup>> = combine(
        debouncedQuery.flatMapLatest { studyGroupRepository.getAllGroupsFlow() },
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
        .catch {
            _isLoading.value = false
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
}
