package com.studygroup.finder.ui.search

import com.studygroup.finder.core.utils.NetworkUtils
import com.studygroup.finder.data.model.StudyGroup
import com.studygroup.finder.data.repository.StudyGroupRepository
import com.studygroup.finder.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val studyGroupRepository: StudyGroupRepository = mockk()
    private val networkUtils: NetworkUtils = mockk()

    private lateinit var viewModel: SearchViewModel

    private val testGroups = listOf(
        StudyGroup(
            groupId = "1",
            name = "Engineering Study Group",
            subject = "Engineering",
            description = "Let's build engines",
            createdBy = "creator1",
            members = emptyList()
        ),
        StudyGroup(
            groupId = "2",
            name = "Science Lab Group",
            subject = "Science",
            description = "Chemistry and Biology experiments",
            createdBy = "creator2",
            members = emptyList()
        ),
        StudyGroup(
            groupId = "3",
            name = "Algebra Practice",
            subject = "Mathematics",
            description = "Algebra practice sessions",
            createdBy = "creator3",
            members = emptyList()
        )
    )

    @Before
    fun setUp() {
        every { networkUtils.isNetworkAvailable() } returns true
        every { studyGroupRepository.getAllGroupsFlow() } returns flowOf(testGroups)
        viewModel = SearchViewModel(studyGroupRepository, networkUtils)
    }

    @Test
    fun initial_state_is_loading_and_empty() = runTest {
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(SearchCategory.ALL, viewModel.selectedCategory.value)
    }

    @Test
    fun query_changed_filters_results_by_name() = runTest {
        // Collect in background to keep StateFlow active
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.searchResults.collect()
        }

        // When user types query
        viewModel.onSearchQueryChanged("Science")
        
        // Wait for debounce delay (300ms)
        testScheduler.advanceTimeBy(350)

        // Then
        val results = viewModel.searchResults.value
        assertEquals(1, results.size)
        assertEquals("2", results[0].groupId)

        collectJob.cancel()
    }

    @Test
    fun category_selected_filters_results_by_category() = runTest {
        // Collect in background to keep StateFlow active
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.searchResults.collect()
        }

        // When category selected
        viewModel.onCategorySelected(SearchCategory.MATHEMATICS)
        
        // Wait for debounce delay
        testScheduler.advanceTimeBy(350)

        // Then
        val results = viewModel.searchResults.value
        assertEquals(1, results.size)
        assertEquals("Algebra Practice", results[0].name)

        collectJob.cancel()
    }
}
