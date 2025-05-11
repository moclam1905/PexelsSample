# Epic 8: Enhanced Search Experience - Refresh & History

**Goal:** To significantly improve the image discovery process by allowing users to easily refresh current search results or load new content via a pull-to-refresh gesture, and to provide quick access to their recent search queries through a search history feature, making the overall search experience more convenient, efficient, and user-centric. This supports Goal 6.

## Story List

### Story 8.1: Implement Pull-to-Refresh Gesture on Results Screen

-   **User Story / Goal:** As a user, when viewing image search results, I want to be able to swipe down from the top of the list to refresh the content, so I can see the latest images for my query or new curated photos.
-   **Detailed Requirements:**
    * Integrate a pull-to-refresh mechanism into `SearchResultsScreen`.
    * Utilize a Compose library component for pull-to-refresh (e.g., from `androidx.compose.material.pullrefresh` or `androidx.compose.material3` if an M3 equivalent is used, along with `rememberPullRefreshState` or similar).
    * The `SearchViewModel` will expose an `isRefreshing: StateFlow<Boolean>` and a `onRefresh: () -> Unit` function.
    * The Composable will observe `isRefreshing` and call `onRefresh` when the gesture is triggered.
-   **Acceptance Criteria (ACs):**
    * AC1: Users can trigger a refresh action by swiping down from the top of the `LazyVerticalGrid` in `SearchResultsScreen`.
    * AC2: A pull-to-refresh Composable wrapper (e.g., `PullRefreshIndicatorTransform`, `PullToRefreshContainer`) is used around the `LazyVerticalGrid`.
    * AC3: The gesture triggers the `onRefresh` lambda connected to the `SearchViewModel`.
    * AC4: A visual refresh indicator (e.g., `PullRefreshIndicator`) is displayed during the gesture and while `isRefreshing` is true.

---

### Story 8.2: Define and Implement Refresh Logic

-   **User Story / Goal:** As a developer, I want to define and implement the specific data fetching logic that occurs when a pull-to-refresh is triggered, so that the user receives relevant updated content.
-   **Detailed Requirements:**
    * In `SearchViewModel`, the `onRefresh` function will:
        * Set `isRefreshing` state to `true`.
        * Determine context: If a current search query exists, re-fetch page 1 for that query. If no query (e.g., curated view), re-fetch page 1 of curated photos.
        * Call the appropriate `ImageRepository` method (e.g., `searchPhotos(query, page=1, perPage=...)` or `getCuratedPhotos(page=1, perPage=...)`).
        * Upon completion (success or error), update the photos list (replacing existing items for the first page) and set `isRefreshing` to `false`.
    * Ensure that multiple refresh requests are handled gracefully (e.g., ignore new request if one is already in progress).
-   **Acceptance Criteria (ACs):**
    * AC1: When pull-to-refresh is triggered with an active search query, `SearchViewModel` requests page 1 of that search from `ImageRepository`.
    * AC2: If no active search query (e.g., initial curated view), pull-to-refresh requests page 1 of curated photos from `ImageRepository`.
    * AC3: `SearchViewModel.isRefreshing` state is true during the data fetch operation and false upon completion.
    * AC4: The main photo list in `SearchViewModel` is cleared and repopulated with the new page 1 results upon successful refresh.

---

### Story 8.3: Visual Feedback and State Handling for Pull-to-Refresh

-   **User Story / Goal:** As a user, when I perform a pull-to-refresh, I want to see a clear visual indicator that the app is working and be informed if the refresh is successful or fails.
-   **Detailed Requirements:**
    * The `PullRefreshIndicator` (or equivalent) should be displayed and animated correctly based on the `isRefreshing` state from `SearchViewModel` and the pull gesture state.
    * If refresh fails (e.g., network error from `ImageRepository`), `SearchViewModel` should update its error state. The UI should then display this error (e.g., using a Snackbar/Toast or the existing `ErrorView` Composable), and `isRefreshing` should be set to `false`.
    * Ensure the refresh indicator is properly hidden when the operation concludes.
-   **Acceptance Criteria (ACs):**
    * AC1: The `PullRefreshIndicator` is visible and animating during the pull gesture and while `SearchViewModel.isRefreshing` is true.
    * AC2: The indicator is hidden when `SearchViewModel.isRefreshing` is false.
    * AC3: New data is displayed in the `LazyVerticalGrid` upon successful refresh.
    * AC4: In case of a refresh error, an appropriate error message is shown to the user (e.g., Snackbar or Toast), and the refresh indicator stops.

---

### Story 8.4: Implement Local Storage for Search History (DataStore)

-   **User Story / Goal:** As a developer, I want to implement local data persistence for storing a user's recent search terms using Jetpack DataStore, so that they can be retrieved and displayed later.
-   **Detailed Requirements:**
    * Add `androidx.datastore:datastore-preferences:1.1.1` dependency.
    * Create a `SearchHistoryDataStoreManager` (or similar name) in `data/local/datastore/` to encapsulate DataStore interactions.
    * Define a `Preferences.Key` for storing search history (e.g., as a JSON string representing a `List<String>`).
    * Implement `SearchHistoryRepositoryImpl` in `data/repository/`:
        * `suspend fun getRecentSearches(limit: Int): Flow<List<String>>`
        * `suspend fun addSearchTerm(term: String)`
        * `suspend fun deleteSearchTerm(term: String)`
        * `suspend fun clearSearchHistory()`
    * These repository methods will interact with `SearchHistoryDataStoreManager`.
    * Provide `SearchHistoryRepository` via Hilt (interface `domain.repository.SearchHistoryRepository` bound to implementation).
    * (Optional) Create UseCases in `domain/usecase/` (e.g., `GetRecentSearchesUseCase`, `SaveSearchTermUseCase`) that use `SearchHistoryRepository`.
-   **Acceptance Criteria (ACs):**
    * AC1: Jetpack DataStore (Preferences) is configured for storing search history.
    * AC2: `SearchHistoryRepository` can save a list of search terms to DataStore.
    * AC3: `SearchHistoryRepository` can retrieve a list of search terms from DataStore.
    * AC4: `SearchHistoryRepository` methods for deleting a specific term and clearing all history are implemented and interact correctly with DataStore.
    * AC5: All DataStore operations are performed asynchronously.

---

### Story 8.5: Display Recent Searches UI

-   **User Story / Goal:** As a user, when I focus on the search bar and it's empty, I want to see a list of my recent searches, so I can quickly select one without retyping.
-   **Detailed Requirements:**
    * In `SearchViewModel`, add state for `recentSearches: List<String>` and `showRecentSearches: Boolean`.
    * When the `SearchBar` Composable (in `HomeScreen` or wherever it's used) is focused and its input text is empty, `SearchViewModel` should fetch recent searches from `SearchHistoryRepository` (or via `GetRecentSearchesUseCase`) and update `recentSearches` and `showRecentSearches`.
    * The UI (likely a `LazyColumn` displayed conditionally below/near the search bar) should render the `recentSearches` list.
    * Limit the number of displayed searches (e.g., 5-10).
    * Order terms with the most recently added/searched at the top.
    * If history is empty, `showRecentSearches` should be false or an empty state message for history shown.
-   **Acceptance Criteria (ACs):**
    * AC1: When the search TextField is focused and empty, a list of up to N (e.g., 5) recent search terms is displayed.
    * AC2: Recent search terms are ordered with the most recent at the top.
    * AC3: If no search history exists, the recent searches UI is not shown, or an appropriate empty message is displayed.
    * AC4: The recent searches UI is hidden when the search bar loses focus or contains text.
    * AC5: `SearchViewModel` correctly fetches and exposes recent searches state.

---

### Story 8.6: Interaction with Search History Items

-   **User Story / Goal:** As a user, I want to be able to tap on a recent search term to quickly re-run that search, and I want options to manage my search history.
-   **Detailed Requirements:**
    * Make each item in the recent searches list clickable.
    * On tap, `SearchViewModel` should:
        * Update its current search query state with the selected term.
        * Trigger a new search for that term (calling `ImageRepository.searchPhotos(...)`).
        * Hide the recent searches UI.
    * Implement a UI mechanism (e.g., an 'X' icon next to each term in the list) to delete an individual search term. This action should call `SearchHistoryRepository.deleteSearchTerm(...)` via the ViewModel.
    * Provide a "Clear All" button/option if the recent searches UI is visible. This action should call `SearchHistoryRepository.clearSearchHistory()` via the ViewModel.
    * UI should update reactively after deletion.
-   **Acceptance Criteria (ACs):**
    * AC1: Tapping a recent search term populates the search input field with that term and initiates a new search.
    * AC2: Users can delete an individual search term from the list, and it's removed from DataStore and the UI.
    * AC3: Users can clear the entire search history, which is reflected in DataStore and the UI.
    * AC4: The recent searches UI is hidden after a history item is tapped to start a search.

---

### Story 8.7: Manage Search History Data Integrity

-   **User Story / Goal:** As a developer, I want to ensure search history data is managed correctly, avoiding duplicates and respecting user privacy.
-   **Detailed Requirements:**
    * In `SearchHistoryRepositoryImpl` (or `SaveSearchTermUseCase`):
        * Before saving a term, normalize it (e.g., convert to lowercase, trim leading/trailing whitespace).
        * Retrieve the current list. If the normalized term exists, remove the old instance. Add the new (or existing moved) normalized term to the beginning of the list (most recent).
        * If the list exceeds the maximum size (e.g., 10 items), remove the oldest item(s) from the end.
        * Save the updated list back to DataStore.
    * Ensure search history is only stored locally on the device.
-   **Acceptance Criteria (ACs):**
    * AC1: When a search is performed, the normalized search term is added to the beginning of the recent searches list in DataStore.
    * AC2: If a normalized search term already exists in history, it is moved to the beginning of the list instead of creating a duplicate entry.
    * AC3: The search history stored in DataStore is limited to a predefined maximum number of entries (e.g., 10), with the oldest entries being removed if the limit is exceeded.
    * AC4: Search terms are normalized (e.g., lowercase, trimmed) before being saved and compared.

---

## Change Log

| Change        | Date       | Version | Description                                                            | Author             |
| ------------- | ---------- | ------- | ---------------------------------------------------------------------- | ------------------ |
| Initial Draft | 2025-05-10 | 0.1     | First draft of Enhanced Search Experience (Refresh & History) epic. | Product Manager AI |
| Architecture Refinement | 2025-05-10 | 0.2     | Added technical details, DataStore integration, and architectural considerations. | Architect AI |