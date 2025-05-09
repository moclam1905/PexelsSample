
# Epic 3: Image Results Display & Pagination

**Goal:** To display the image search results fetched from the Pexels API in a visually appealing list or grid, implement image loading for thumbnails, and enable smooth pagination to load more results as the user scrolls. This epic also covers handling the "no results found" state.

## Story List

### Story 3.1: Search Results List/Grid UI Structure

-   **User Story / Goal:** As a user, I want to see the images matching my search query displayed in a scrollable list or grid, so that I can quickly browse through them.
-   **Detailed Requirements:**
    * Create a Composable screen or section (e.g., `SearchResultsScreen`) to display the image results. This screen would be navigated to or become visible after a search is successfully initiated in Epic 2.
    * Use a `LazyVerticalGrid` or `LazyColumn` Composable to efficiently display a potentially large number of image items. A grid layout is preferred for visual appeal.
    * The `SearchViewModel` (from Epic 2) should expose the list of `Photo` objects (from Story 1.4's data models) as an observable state.
    * The UI should observe this list and render an `ImageItem` (from Story 3.2) for each photo.
-   **Acceptance Criteria (ACs):**
    * AC1: A `SearchResultsScreen` (or equivalent Composable area) is implemented.
    * AC2: A `LazyVerticalGrid` (or `LazyColumn`) is used to display image items.
    * AC3: The UI correctly observes and displays items from the ViewModel's list of photos.
    * AC4: The list/grid is scrollable if the number of items exceeds the screen height.

---

### Story 3.2: Image Item Composable & Image Loading

-   **User Story / Goal:** As a user, I want to see a thumbnail for each image in the search results, so that I can get a preview of the image content.
-   **Detailed Requirements:**
    * Create a reusable `ImageItem` Composable that takes a `Photo` object as input.
    * The `ImageItem` should display the image using an image loading library (e.g., Coil for Compose).
        * Add the chosen image loading library dependency.
        * Load the image from one of the URLs in the `Photo.src` object (e.g., `Photo.src.medium` or `Photo.src.portrait` depending on desired aspect ratio/size).
    * The `ImageItem` should display a placeholder (e.g., a solid color based on `Photo.avg_color`, or a generic placeholder drawable) while the image is loading.
    * The `ImageItem` should handle image loading failures gracefully (e.g., display an error icon or a default placeholder).
    * Each `ImageItem` should be a reasonable size for a grid/list thumbnail.
-   **Acceptance Criteria (ACs):**
    * AC1: An `ImageItem` Composable is created.
    * AC2: The Composable uses an image loading library (e.g., Coil) to load and display the image from the provided `Photo` data.
    * AC3: A placeholder is shown during image loading.
    * AC4: An error state (e.g., different placeholder or icon) is shown if image loading fails for an item.
    * AC5: Images are displayed with reasonable quality and performance in the grid/list.

---

### Story 3.3: ViewModel Logic for Handling Results & Pagination State

-   **User Story / Goal:** As a Developer, I want the `SearchViewModel` to manage the list of search results, current page, and total results, so that pagination can be implemented effectively.
-   **Detailed Requirements:**
    * Extend `SearchViewModel` (from Epic 2) to:
        * Store the list of successfully fetched `Photo` objects (e.g., in a `MutableStateFlow<List<Photo>>`).
        * Keep track of the current page number for pagination (e.g., `currentPage: Int`).
        * Store the total number of available results if provided by the API (e.g., `totalResults: Int`).
        * Store whether more results are available to load (e.g., `canLoadMore: Boolean`).
    * When a search API call (from Story 2.3) is successful:
        * If it's the first page, replace the existing list with the new results.
        * If it's a subsequent page (for pagination), append the new results to the existing list.
        * Update `currentPage`, `totalResults`, and `canLoadMore` accordingly based on the API response.
-   **Acceptance Criteria (ACs):**
    * AC1: `SearchViewModel` maintains and updates a list of `Photo` objects from API responses.
    * AC2: `SearchViewModel` correctly tracks the current page number.
    * AC3: `SearchViewModel` correctly determines if more pages can be loaded based on API response (e.g., `next_page` URL in Pexels API response or by comparing loaded items vs. `total_results`).
    * AC4: On a new search, the existing results list is cleared and populated with new data. For pagination, new results are appended.

---

### Story 3.4: "Load More" Functionality (Pagination Trigger)

-   **User Story / Goal:** As a user, when I scroll to the bottom of the search results, I want the application to automatically load more images, so that I can continue Browse without manual intervention.
-   **Detailed Requirements:**
    * In the `SearchResultsScreen` (hosting the `LazyVerticalGrid`/`LazyColumn`), detect when the user has scrolled near the end of the currently loaded items.
    * When this condition is met and `SearchViewModel.canLoadMore` is true:
        * Trigger a function in the `SearchViewModel` to fetch the next page of results (e.g., `loadNextPage()`).
        * The `loadNextPage()` function should increment the page number and call the `PexelsApiService.searchPhotos()` method with the new page.
    * The ViewModel should manage a loading state specifically for pagination (e.g., `isLoadingMore: StateFlow<Boolean>`).
    * Display a small loading indicator at the bottom of the list/grid when more items are being loaded.
-   **Acceptance Criteria (ACs):**
    * AC1: The app detects when the user scrolls near the end of the list/grid.
    * AC2: If more results can be loaded, `SearchViewModel.loadNextPage()` is called.
    * AC3: The ViewModel requests the next page of data from the API.
    * AC4: A loading indicator is shown at the bottom of the list/grid during pagination loading.
    * AC5: New items are appended to the list/grid and become visible after successful loading.
    * AC6: Pagination requests stop if `canLoadMore` becomes false.

---

### Story 3.5: Handle Empty Search Results State

-   **User Story / Goal:** As a user, if my search query yields no results, I want to see a clear message indicating this, so that I understand why no images are displayed.
-   **Detailed Requirements:**
    * The `SearchViewModel` should expose a state indicating whether the last search yielded zero results (e.g., `isResultsEmpty: StateFlow<Boolean>`). This should be true if the API call was successful but the list of photos is empty.
    * The `SearchResultsScreen` (or relevant UI part) should observe this state.
    * If `isResultsEmpty` is true (and not in a loading state), hide the results list/grid and display a user-friendly message (e.g., "No images found for '[search query]'. Try a different search.").
-   **Acceptance Criteria (ACs):**
    * AC1: `SearchViewModel` correctly identifies and exposes an "empty results" state.
    * AC2: When search results are empty, the UI hides the (empty) list/grid.
    * AC3: A clear, user-friendly message is displayed to the user indicating no results were found.
    * AC4: The "no results" message is not shown during initial loading or if there are results.

---

## Change Log

| Change        | Date       | Version | Description                                          | Author         |
| ------------- | ---------- | ------- | ---------------------------------------------------- | -------------- |
| Initial Draft | 2025-05-08 | 0.1     | First draft of image results display and pagination epic | Product Manager AI |

