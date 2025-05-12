# Epic 10: Engaging HomeScreen Experience - Trending Photos

**Goal:** To provide users with an immediate and visually engaging experience upon app launch by displaying a dynamic, infinitely scrollable, and refreshable staggered grid of trending/curated photos on the HomeScreen, encouraging passive discovery and setting an inspiring tone for the application. This directly supports Goal 7 of the project.

## Story List

### Story 10.1: Implement HomeScreen with LazyVerticalStaggeredGrid for Trending Photos

-   **User Story / Goal:** As a user, when I open the app, I want to see a visually appealing grid of trending photos on the HomeScreen, so I can immediately start discovering interesting images.
-   **Detailed Requirements:**
    * Create `HomeScreen.kt` in the `ui.home` package. This screen will be the start destination in the navigation graph.
    * Create `HomeScreenViewModel.kt` in `ui.home`, injected by Hilt. It will use `ImageRepository` to fetch curated photos (Pexels API `/v1/curated`).
    * `HomeScreenViewModel` to expose UI state (e.g., `StateFlow<HomeScreenUiState>` containing `isLoading: Boolean`, `photos: List<Photo>`, `errorMessage: String?`).
    * `HomeScreen` will display photos using Jetpack Compose's `LazyVerticalStaggeredGrid` from `androidx.compose.foundation.lazy.staggeredgrid`.
    * Each grid item will be an `ImageItem` Composable (reused or adapted from search results), loading an appropriate image size (e.g., `Photo.src.large` or `Photo.src.medium`) using Coil.
-   **Acceptance Criteria (ACs):**
    * AC1: `HomeScreen` displays a grid of photos fetched by `HomeScreenViewModel` from `ImageRepository` (using `/v1/curated` endpoint).
    * AC2: The grid is implemented using `LazyVerticalStaggeredGrid`.
    * AC3: `HomeScreenViewModel` correctly manages the initial data fetching and exposes UI state.
    * AC4: Images are loaded into grid items using Coil, displaying actual photo content with varying aspect ratios reflected in the staggered layout.

---

### Story 10.2: Implement Infinite Scrolling for Trending Photos on HomeScreen

-   **User Story / Goal:** As a user, when I scroll to the bottom of the trending photos on the HomeScreen, I want more photos to load automatically, so I can continuously discover new content.
-   **Detailed Requirements:**
    * In `HomeScreen`, use `rememberLazyStaggeredGridState()` and monitor its `layoutInfo.visibleItemsInfo` to detect when the last items are visible.
    * When near the end, and if not already loading more and more pages are available, call a `loadNextPage()` function in `HomeScreenViewModel`.
    * `HomeScreenViewModel` will manage `currentPage` for the `/v1/curated` endpoint and call `ImageRepository.getCuratedPhotos(page = nextPage, ...)`.
    * New photos are appended to the existing list in the ViewModel's state.
    * Display a small loading indicator item at the bottom of the `LazyVerticalStaggeredGrid` when `isLoadingNextPage` state in ViewModel is true.
    * Handle pagination errors from the repository gracefully (e.g., show a small error item with a retry option at the end of the list).
-   **Acceptance Criteria (ACs):**
    * AC1: When scrolling near the end of `LazyVerticalStaggeredGrid` on `HomeScreen`, `HomeScreenViewModel.loadNextPage()` is triggered.
    * AC2: A loading indicator item is visible at the end of the grid while `HomeScreenViewModel.isLoadingNextPage` is true.
    * AC3: New photos are appended to the grid upon successful loading of the next page.
    * AC4: Infinite scrolling is smooth and does not cause significant UI jank.
    * AC5: If loading the next page fails, an inline error message with a retry option is shown at the end of the list.

---

### Story 10.3: Implement Dynamic Column Count for HomeScreen Grid

-   **User Story / Goal:** As a user, I want the HomeScreen's photo grid to adapt its number of columns based on my device's screen size and orientation, so it always looks well-organized and utilizes space effectively.
-   **Detailed Requirements:**
    * `HomeScreen` will consume `WindowSizeClass` (provided from `MainActivity` or a root Composable, as established in Epic 7).
    * Dynamically set the `columns` parameter of `LazyVerticalStaggeredGrid.Fixed(count)` based on `windowSizeClass.widthSizeClass` (and potentially `LocalConfiguration.current.orientation`).
        * Example: `Compact` width: 2 columns, `Medium`: 3 columns, `Expanded`: 4 columns.
    * Ensure item spans and content within `ImageItem` adapt well to changing column counts.
-   **Acceptance Criteria (ACs):**
    * AC1: The number of columns in the `LazyVerticalStaggeredGrid` on `HomeScreen` adjusts dynamically based on `WindowSizeClass.widthSizeClass`.
    * AC2: Column count also considers device orientation if specific rules are defined (e.g., landscape typically allows more columns than portrait for the same width class).
    * AC3: The staggered layout remains visually appealing, and items are well-proportioned with different column counts.
    * AC4: State (scroll position, loaded items) is reasonably preserved during column count changes due to configuration updates.

---

### Story 10.4: Implement User-Initiated Refresh on HomeScreen

-   **User Story / Goal:** As a user, I want a way to manually refresh the list of trending photos on the HomeScreen, so I can see the very latest curated content if I choose.
-   **Detailed Requirements:**
    * Add a "Refresh" `IconButton` to the `HomeScreen`, likely in a `TopAppBar` or as a distinct UI element.
    * Tapping this button calls an `onRefresh()` function in `HomeScreenViewModel`.
    * `HomeScreenViewModel.onRefresh()` will:
        * Set an `isRefreshing` state to true (distinct from pagination loading).
        * Call `ImageRepository.getCuratedPhotos(page = 1, ...)` to fetch the first page, effectively resetting the list.
        * Update the photos list and reset pagination state.
        * Set `isRefreshing` to false on completion.
    * The refresh button should be disabled or show a progress indicator while `isRefreshing` is true.
    * Provide feedback via Snackbar (e.g., "Photos updated" or "Failed to refresh").
-   **Acceptance Criteria (ACs):**
    * AC1: A "Refresh" `IconButton` is present and functional on the `HomeScreen`.
    * AC2: Tapping the refresh button triggers `HomeScreenViewModel` to re-fetch page 1 of curated photos.
    * AC3: Visual feedback (e.g., progress indicator on/near button, or main list shows loading) is displayed while `isRefreshing` is true.
    * AC4: The grid is updated with new photos, replacing the old list.
    * AC5: A Snackbar message confirms success or failure of the refresh operation.

---

### Story 10.5: Enhance HomeScreen UI/UX (Placeholders, Shimmer, Spacing, Corners)

-   **User Story / Goal:** As a user, I want the HomeScreen grid to look highly polished and modern, with smooth loading indicators and visually appealing item presentation.
-   **Detailed Requirements:**
    * Define `Arrangement.spacedBy(...)` for `LazyVerticalStaggeredGrid`'s `verticalItemSpacing` and `horizontalArrangement` to ensure consistent spacing.
    * Apply `Modifier.clip(RoundedCornerShape(X.dp))` (e.g., 8.dp or 12.dp) to each `ImageItem` in the grid.
    * `AsyncImage` within `ImageItem` should use a color placeholder (e.g., from `Photo.avgColor` if fetched quickly enough, or a default grey).
    * Implement or integrate a shimmer effect library (e.g., `com.valentinilk.shimmer` with a Compose wrapper, or a Compose-native alternative if available and simple). The shimmer effect should overlay the placeholder areas of `ImageItem`s while their images are loading.
    * Add `contentPadding` to the `LazyVerticalStaggeredGrid`.
-   **Acceptance Criteria (ACs):**
    * AC1: `LazyVerticalStaggeredGrid` items have consistent vertical and horizontal spacing.
    * AC2: `ImageItem`s displayed in the grid have rounded corners.
    * AC3: A shimmer effect is displayed over `ImageItem` placeholders while their respective images are loading.
    * AC4: The `LazyVerticalStaggeredGrid` has appropriate padding from the screen edges.
    * AC5: The overall loading experience for images in the grid feels polished and modern.

---

### Story 10.6: Robust State Handling for HomeScreen (Initial Load, Empty, Error)

-   **User Story / Goal:** As a user, I want the HomeScreen to clearly communicate its status, whether it's loading initial photos, encounters an error, or if (hypothetically) no trending photos are available.
-   **Detailed Requirements:**
    * `HomeScreenViewModel.uiState` will include states for initial loading, success (with photo list), empty, and error.
    * **Initial Load:** When `HomeScreenUiState.isLoadingInitial` is true, `HomeScreen` displays a centered `CircularProgressIndicator` or a full-screen shimmer over a skeleton grid.
    * **Empty State:** If `ImageRepository` returns an empty list for the initial curated photos fetch (and not in error state), `HomeScreenUiState.isEmpty` becomes true. `HomeScreen` displays a user-friendly message (e.g., "No trending photos to show right now. Try refreshing!").
    * **Error State:** If the initial fetch from `ImageRepository` results in an error (e.g., network), `HomeScreenUiState.errorMessage` is populated. `HomeScreen` displays a centered `ErrorView` Composable (reused from Epic 5) with the message and a "Retry" button that calls a retry function in `HomeScreenViewModel`.
    * Use `AnimatedVisibility` or `Crossfade` to smoothly transition between these states based on `HomeScreenViewModel.uiState`.
-   **Acceptance Criteria (ACs):**
    * AC1: A full-screen loading indicator (or skeleton shimmer) is shown when `HomeScreenViewModel.uiState.isLoadingInitial` is true.
    * AC2: If the initial photo list is empty (and no error), a user-friendly "empty state" message is displayed.
    * AC3: If the initial data fetch fails, a full-screen `ErrorView` with a "Retry" button is displayed.
    * AC4: Transitions between loading, content, empty, and error states on `HomeScreen` are smooth (e.g., using `Crossfade` or `AnimatedVisibility`).

---

### Story 10.7: Navigate from HomeScreen Image to Detail View

-   **User Story / Goal:** As a user, when I tap on an image in the HomeScreen's trending photos grid, I want to be taken to the detailed view of that image.
-   **Detailed Requirements:**
    * Make each `ImageItem` in the `LazyVerticalStaggeredGrid` on `HomeScreen` clickable using `Modifier.clickable`.
    * On tap, trigger navigation to the `ImageDetailScreen`.
    * Pass the selected `Photo` object (or its ID, consistent with how search results navigate to detail) to `ImageDetailScreen` via navigation arguments.
    * Shared element transition (Story 9.1) should be attempted for this navigation path if feasible, providing visual continuity from the staggered grid item to the detail view image. This might require coordinating `LookaheadLayout` or similar mechanisms.
-   **Acceptance Criteria (ACs):**
    * AC1: Tapping an image in the `LazyVerticalStaggeredGrid` on `HomeScreen` navigates to `ImageDetailScreen`.
    * AC2: The correct `Photo` data for the tapped image is passed to and displayed on `ImageDetailScreen`.
    * AC3: Navigation is smooth. If shared element transition is implemented for this path, it works correctly and performantly.

---

## Change Log

| Change        | Date         | Version | Description                                                          | Author             |
| ------------- | ------------ | ------- | -------------------------------------------------------------------- | ------------------ |
| Initial Draft | 2025-05-12   | 0.1     | First draft of Engaging HomeScreen Experience (Trending Photos) epic. | Product Manager AI |
| Architecture Refinement | 2025-05-12 | 0.2 | Added technical details, ViewModel interactions, state handling, and refined ACs based on architecture. | Architect AI |