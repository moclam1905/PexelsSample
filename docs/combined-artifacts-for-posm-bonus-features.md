# Epic 6 file

# Epic 6: Advanced Image Interaction - Pinch-to-Zoom

**Goal:** To enhance the image detail view by allowing users to intuitively zoom in/out and pan images for a more detailed inspection, leveraging native Android capabilities or well-vetted third-party libraries to ensure a smooth, performant, and user-friendly experience. This directly contributes to Goal 6 of the project.

## Story List

### Story 6.1: Implement Core Pinch-to-Zoom and Pan Functionality

-   **User Story / Goal:** As a user, when viewing an image in the detail screen, I want to use pinch gestures to zoom in and out, and drag gestures to pan the zoomed image, so I can inspect image details more closely.
-   **Detailed Requirements:**
    * Integrate gesture detection within the `ImageDetailScreen` Composable (likely around the `AsyncImage` Composable displaying the full image).
    * Utilize `Modifier.pointerInput` with `detectTransformGestures` for handling pinch (scale) and drag (pan) events.
    * Maintain state for current `scale`, `offsetX`, and `offsetY` using `remember` and `mutableStateOf`.
    * Apply transformations to the `AsyncImage` Composable using `Modifier.graphicsLayer { scaleX = currentScale; scaleY = currentScale; translationX = currentOffsetX; translationY = currentOffsetY; }`.
    * Ensure that panning is only effective when the `currentScale` makes the image content larger than the view bounds.
    * The initial implementation will focus on these Compose-native capabilities.
-   **Acceptance Criteria (ACs):**
    * AC1: Users can zoom into an image in the detail view using a two-finger pinch-out gesture, updating the `scale` state.
    * AC2: Users can zoom out of an image in the detail view using a two-finger pinch-in gesture, updating the `scale` state.
    * AC3: When an image is zoomed in (`scale` > fit-to-view scale), users can pan the image horizontally and vertically by dragging, updating `offsetX` and `offsetY` states.
    * AC4: Zooming and panning actions feel smooth and responsive on target devices/emulators.
    * AC5: Panning is restricted if the scaled image's dimension is smaller than or equal to the view bounds in that axis.
    * AC6: The gesture detection mechanism is primarily based on Jetpack Compose's `pointerInput` and `detectTransformGestures`.

---

### Story 6.2: Implement Zoom Level Constraints and Boundary Checks

-   **User Story / Goal:** As a user, when zooming and panning an image, I want the interaction to be constrained within sensible limits, so I don't zoom too far in/out or pan the image completely off-screen.
-   **Detailed Requirements:**
    * Define minimum zoom level: This will typically be the scale at which the image "fits to view" (either by width or height, maintaining aspect ratio, depending on image and screen dimensions). Calculate this initial scale when the image dimensions and layout size are known.
    * Define maximum zoom level: E.g., 3.0f to 5.0f times the content's original pixel density, or a cap based on the "fit-to-view" scale.
    * In the gesture handling logic, clamp the `currentScale` state between the calculated `minScale` and `maxScale`.
    * Implement boundary checks for panning: When `offsetX` or `offsetY` are updated, calculate the visible edges of the scaled image. Prevent these offsets from allowing the image edges to move beyond the view bounds, unless the scaled image dimension is smaller than the view dimension along that axis.
    * The calculations need to account for the image's aspect ratio and the Composable's layout size.
-   **Acceptance Criteria (ACs):**
    * AC1: The image cannot be zoomed out smaller than its calculated "fit-to-view" scale.
    * AC2: The image cannot be zoomed in beyond the predefined maximum magnification factor (e.g., `maxScale`).
    * AC3: When panning a zoomed-in image, the `offsetX` and `offsetY` are clamped so that image edges do not pan beyond the view container's edges, unless the image is smaller than the container in that dimension.
    * AC4: Zoom and pan constraints work correctly for images of various aspect ratios loaded into `ImageDetailScreen`.
    * AC5: Zoom and pan state variables (`scale`, `offsetX`, `offsetY`) correctly reflect applied constraints.

---

### Story 6.3: Implement Double-Tap to Zoom Functionality

-   **User Story / Goal:** As a user, I want to quickly zoom into an image or reset its zoom level by double-tapping it, providing a convenient alternative to pinch gestures.
-   **Detailed Requirements:**
    * Implement double-tap gesture detection on the image Composable in `ImageDetailScreen` using `Modifier.pointerInput` with `detectTapGestures(onDoubleTap = ...)`.
    * Define an intermediate zoom level (e.g., `intermediateScale = 2.0f * fitToViewScale`, or a scale that makes the image fit by width).
    * On double-tap:
        * If current `scale` is close to "fit-to-view" scale, animate to `intermediateScale` (centered on the tap point if possible, otherwise center of the image).
        * If current `scale` is greater than "fit-to-view" scale, animate back to "fit-to-view" scale and reset pan offsets.
    * Use Jetpack Compose animation APIs (e.g., `Animatable` or `animate*AsState`) for smooth transitions of `scale`, `offsetX`, and `offsetY` during double-tap zoom actions.
-   **Acceptance Criteria (ACs):**
    * AC1: Double-tapping a "fit-to-view" image animates its scale to a predefined intermediate level.
    * AC2: Double-tapping an already zoomed image (either by pinch or previous double-tap) animates its scale back to "fit-to-view" and resets pan.
    * AC3: The zoom transition (scale and pan if applicable) triggered by a double-tap is animated smoothly.
    * AC4: The tap point for zoom-in is considered for a more focused zoom, if feasible.

---

### Story 6.4: Ensure Performance and Handle High-Resolution Images for Zoom

-   **User Story / Goal:** As a developer, I want the pinch-to-zoom feature to perform smoothly without jank, even with high-resolution images, and manage memory efficiently.
-   **Detailed Requirements:**
    * Load `Photo.src.large2x` or `Photo.src.original` into the `AsyncImage` Composable within `ImageDetailScreen`.
    * Test pinch-to-zoom, pan, and double-tap performance with various image sizes sourced from Pexels API, focusing on responsiveness and frame rate.
    * Profile using Android Studio's tools if jank or lag is observed during transformations or animations.
    * Ensure matrix transformations in `Modifier.graphicsLayer` and bounds calculations are performed efficiently.
    * If significant performance issues or `OutOfMemoryError` exceptions occur with typical "original" images *after* this initial direct transformation approach, a separate technical spike/story will be created to investigate and potentially integrate a subsampling image view solution compatible with Compose.
-   **Acceptance Criteria (ACs):**
    * AC1: Pinching, panning, and double-tap zoom animations are smooth (target 60fps) on target devices/emulators when using `Photo.src.large2x`.
    * AC2: The application handles `Photo.src.large2x` (and typical `Photo.src.original` from Pexels) images in the zoom view without `OutOfMemoryError` exceptions under normal conditions.
    * AC3: There is no noticeable lag or jank during zoom/pan interactions with these images.

---

### Story 6.5: Preserve Zoom State Across Configuration Changes

-   **User Story / Goal:** As a user, if I zoom into an image and then rotate my device or the app undergoes a configuration change, I want the image's zoom level and pan position to be preserved.
-   **Detailed Requirements:**
    * The zoom state consists of `scale: Float`, `offsetX: Float`, `offsetY: Float`.
    * Use `rememberSaveable` within the `ImageDetailScreen` Composable to store and restore these state variables.
    * If the state includes non-standard types or complex logic, a custom `Saver` object compatible with `rememberSaveable` might be required.
    * Alternatively, if an `ImageDetailViewModel` is introduced for other reasons or if state logic becomes significantly complex, this state can be held in the ViewModel and saved/restored using its `SavedStateHandle`. This would be a preferred approach for more robust state management. The decision will be made based on complexity encountered.
-   **Acceptance Criteria (ACs):**
    * AC1: After a device rotation (or other configuration change) while viewing a zoomed/panned image, the `ImageDetailScreen` correctly restores the previous `scale`.
    * AC2: After a configuration change, the `ImageDetailScreen` correctly restores the previous `offsetX` and `offsetY`.
    * AC3: State restoration is seamless and does not cause visual glitches or reset the image view.
    * AC4: The chosen solution (either `rememberSaveable` in Composable or ViewModel with `SavedStateHandle`) effectively preserves the state through process death if applicable.

---

## Change Log

| Change        | Date       | Version | Description                                         | Author             |
| ------------- | ---------- | ------- | --------------------------------------------------- | ------------------ |
| Initial Draft | 2025-05-10 | 0.1     | First draft of Pinch-to-Zoom epic for bonus features. | Product Manager AI |
| Architecture Refinement | 2025-05-10 | 0.2     | Added technical details and architectural considerations to stories. | Architect AI |


# Epic 7 file

# Epic 7: Adaptive User Interface for Diverse Screens

**Goal:** To ensure the PexelsSample application provides an optimal viewing experience across a wide range of Android devices by implementing responsive and adaptive layouts that adjust gracefully to different screen sizes, orientations, and window configurations (e.g., multi-window mode), leveraging Jetpack WindowSizeClasses and Compose's adaptive capabilities. This supports Goal 6.

## Story List

### Story 7.1: Integrate Jetpack Window Size Classes for Layout Decisions

-   **User Story / Goal:** As a Developer, I want to integrate Jetpack Window Size Classes into the application, so that I have a robust foundation for making high-level decisions about layout structure based on available screen space.
-   **Detailed Requirements:**
    * Add the `androidx.compose.material3:material3-window-size-class` dependency if not already included by the Compose BOM (confirm version compatibility with BOM `2024.09.00`).
    * In `MainActivity`, use `calculateWindowSizeClass(this)` and make the resulting `WindowSizeClass` object available to the root Composable hierarchy (e.g., by passing it down or using a `CompositionLocal`).
    * Ensure `WindowSizeClass` is recalculated and propagated correctly on configuration changes (e.g., rotation, multi-window resizing).
-   **Acceptance Criteria (ACs):**
    * AC1: `WindowSizeClass` (containing `widthSizeClass` and `heightSizeClass`) is correctly calculated in `MainActivity`.
    * AC2: The `WindowSizeClass` is accessible by top-level screen Composables.
    * AC3: The `WindowSizeClass` updates accurately when the app's window size or orientation changes, triggering appropriate recompositions.

---

### Story 7.2: Adapt Search Results Grid to Screen Width

-   **User Story / Goal:** As a user, I want the grid of image search results to utilize screen space effectively by showing more columns on wider screens and fewer columns on narrower screens.
-   **Detailed Requirements:**
    * In `SearchResultsScreen`, consume the `WindowSizeClass`.
    * Use a `when` expression based on `windowSizeClass.widthSizeClass` to determine the number of columns for the `LazyVerticalGrid`:
        * `WindowWidthSizeClass.Compact`: e.g., 2 columns.
        * `WindowWidthSizeClass.Medium`: e.g., 3 columns.
        * `WindowWidthSizeClass.Expanded`: e.g., 4 or 5 columns.
    * Ensure `ImageItem` Composables are designed to look good within these varying column counts (e.g., aspect ratio handling).
    * Test on phone (portrait/landscape), foldable (inner/outer displays), and tablet emulators.
-   **Acceptance Criteria (ACs):**
    * AC1: `SearchResultsScreen` displays 2 columns for `Compact` width.
    * AC2: `SearchResultsScreen` displays 3 columns for `Medium` width.
    * AC3: `SearchResultsScreen` displays 4 (or 5) columns for `Expanded` width.
    * AC4: The grid items maintain appropriate spacing and visual appeal across different column counts.
    * AC5: Performance of the `LazyVerticalGrid` remains smooth with dynamic column counts.

---

### Story 7.3: Implement Adaptive Layout for Image Detail View (Optional Two-Pane)

-   **User Story / Goal:** As a user with a larger screen (e.g., a tablet in landscape), I want to potentially see the image detail view alongside the search results list, so I can browse and view details more efficiently without full screen navigation for each image.
-   **Detailed Requirements:**
    * In a Composable that hosts both search results and potentially details (e.g., `HomeScreen` or a new container screen), use `WindowSizeClass` (e.g., `windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded`).
    * If `Expanded` width:
        * Display `SearchResultsScreen` (or its list part) in one pane and `ImageDetailScreen` (or a new `ImageDetailPane` Composable) in another pane (e.g., using `Row` with weights).
        * `SearchViewModel` will need to hold the state of the currently selected `Photo` to be shown in the detail pane.
        * Tapping an item in the list pane updates the detail pane. Navigation to a separate full screen for detail is suppressed.
    * If `Compact` or `Medium` width:
        * Use the existing full-screen navigation to `ImageDetailScreen` when an item is tapped.
    * Pinch-to-zoom (Epic 6) must function correctly within the detail pane.
    * State of selected item in list and zoom state in detail pane must be handled across configuration changes.
-   **Acceptance Criteria (ACs):**
    * AC1: When `windowSizeClass.widthSizeClass` is `Expanded`, a two-pane layout is active, showing the image list and image detail side-by-side.
    * AC2: Selecting an image in the list pane updates the content of the detail pane in the two-pane layout.
    * AC3: When `windowSizeClass.widthSizeClass` is `Compact` or `Medium`, tapping an image navigates to a full-screen `ImageDetailScreen`.
    * AC4: Pinch-to-zoom functionality (from Epic 6) works correctly within the detail pane of the two-pane layout.
    * AC5: Navigation and back stack behavior are logical in both single-pane and two-pane modes.

---

### Story 7.4: Responsive Sizing for UI Elements (Text, Spacing, Touch Targets)

-   **User Story / Goal:** As a user, regardless of my screen size, I want text to be readable, UI elements to be well-spaced, and touch targets to be appropriately sized.
-   **Detailed Requirements:**
    * Review key Composables (`SearchBar`, `ImageItem`, `Text` elements in `ImageDetailScreen`, Buttons).
    * Define typography and spacing guidelines based on `WindowSizeClass` or by using adaptive modifiers like `Modifier.weight()` or `BoxWithConstraints` for more granular control if needed.
    * Consider using `LocalDensity.current.fontScale` to respect user's system font size settings.
    * Ensure all interactive elements maintain a minimum touch target size (e.g., 48dp x 48dp) across all configurations.
    * Test for readability and usability on Compact, Medium, and Expanded width classes.
-   **Acceptance Criteria (ACs):**
    * AC1: Text in major UI elements (titles, descriptions, buttons) remains legible across different `WindowSizeClass` categories.
    * AC2: Spacing (margins, paddings) around key UI elements adapts to avoid clutter on small screens and excessive emptiness on large screens.
    * AC3: All interactive elements (buttons, clickable image items) meet minimum touch target size requirements (e.g., 48dp) on all screen configurations.
    * AC4: The overall visual hierarchy and balance are maintained across screen sizes.

---

### Story 7.5: Graceful Handling of Orientation Changes

-   **User Story / Goal:** As a user, when I rotate my device, I expect the app to adapt its layout smoothly to the new orientation, preserving my current context and any ongoing interactions (like a zoomed image state).
-   **Detailed Requirements:**
    * Verify all screens (`HomeScreen`, `SearchResultsScreen`, `ImageDetailScreen`) respond correctly to portrait/landscape orientation changes.
    * Layouts should adapt based on the new `WindowSizeClass` calculated after orientation change (e.g., grid columns, single/two-pane switch).
    * Ensure all relevant state is preserved using `rememberSaveable` or ViewModel's `SavedStateHandle`:
        * Current search query in `SearchViewModel`.
        * Loaded search results and pagination state (`currentPage`) in `SearchViewModel`.
        * Scroll position of `LazyVerticalGrid` in `SearchResultsScreen`.
        * Zoom/pan state of image in `ImageDetailScreen` (as per Story 6.5).
        * Selected item for two-pane layout (if applicable).
-   **Acceptance Criteria (ACs):**
    * AC1: All screens correctly re-render and remain usable after changing device orientation from portrait to landscape and vice-versa.
    * AC2: Layouts automatically adapt based on the `WindowSizeClass` changes resulting from orientation change (e.g., column counts adjust, two-pane layout activates/deactivates).
    * AC3: Critical application state (search query, results, pagination info, scroll positions, zoom/pan state) is correctly preserved and restored across orientation changes.

---

## Change Log

| Change        | Date       | Version | Description                                                | Author             |
| ------------- | ---------- | ------- | ---------------------------------------------------------- | ------------------ |
| Initial Draft | 2025-05-10 | 0.1     | First draft of Adaptive User Interface epic for bonus features. | Product Manager AI |
| Architecture Refinement | 2025-05-10 | 0.2     | Added technical details and architectural considerations to stories, including `WindowSizeClass` integration. | Architect AI |


# Epic 8 file

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

# Epic 9 file

# Epic 9: Rich UI Animations & Transitions

**Goal:** To elevate the overall user experience of the PexelsSample application by implementing purposeful, smooth, and aesthetically pleasing UI animations and transitions. These enhancements will make interactions feel more intuitive, engaging, and modern, aligning with Goal 6 and the vision of a high-quality application.

## Story List

### Story 9.1: Implement Shared Element Transition for Image Detail View

-   **User Story / Goal:** As a user, when I tap on an image in the results grid, I want the image to smoothly animate and transition from its position in the grid to its place in the image detail view, creating a seamless and visually connected experience.
-   **Detailed Requirements:**
    * Investigate and implement shared element transition primarily using Jetpack Compose's `LookaheadLayout` if suitable for coordinating the layout and animation of the tapped image from `SearchResultsScreen` to `ImageDetailScreen`.
    * The transition should animate properties like size, position, and potentially aspect ratio correction.
    * Ensure state (e.g., scroll position of the grid) is managed correctly so the reverse transition targets the correct grid item.
    * Pass necessary information (e.g., image ID, initial bounds if helpful for `LookaheadLayout`) as part of the navigation action or through a shared ViewModel if `LookaheadLayout` is used across different navigation destinations.
    * Ensure the reverse transition (from detail back to grid) is also smooth, visually consistent, and correctly handles the image animating back to its (potentially off-screen) grid cell.
    * If `LookaheadLayout` proves too complex or has limitations for this cross-screen scenario, explore alternative advanced Compose animation techniques before considering View system interop.
-   **Acceptance Criteria (ACs):**
    * AC1: When an image in the results grid is tapped, it animates (scales and moves) smoothly from its grid position to its final position/size in the `ImageDetailScreen`.
    * AC2: When navigating back from `ImageDetailScreen`, the image animates smoothly back to its corresponding position and size in the results grid, even if the grid item was previously off-screen.
    * AC3: The transition is performant (targets 60fps) and visually polished, without jarring jumps.
    * AC4: The transition correctly handles images of various aspect ratios.
    * AC5: The chosen implementation is primarily Compose-based, utilizing `LookaheadLayout` or comparable advanced animation/layout APIs.

---

### Story 9.2: Animate Image Loading and Appearance in Results Grid

-   **User Story / Goal:** As a user, when images load into the search results grid, I want them to appear smoothly with a subtle animation (e.g., fade-in), rather than abruptly popping in, enhancing the visual flow.
-   **Detailed Requirements:**
    * For each `ImageItem` in the `LazyVerticalGrid` of `SearchResultsScreen`:
        * When using Coil's `AsyncImage`, leverage its built-in transition capabilities (e.g., `AsyncImage(..., transition = Transition.Crossfade(durationMillis = 300))` or a custom `Transition.Factory`).
        * Alternatively, use `AnimatedVisibility` around the `AsyncImage` that becomes visible when the image successfully loads, applying a fade-in transition.
    * The animation duration should be brief (e.g., 200-300ms).
-   **Acceptance Criteria (ACs):**
    * AC1: Images displayed via `AsyncImage` in the results grid fade in smoothly as they are successfully loaded.
    * AC2: The fade-in animation is subtle (e.g., 200-300ms duration) and does not feel distracting.
    * AC3: Placeholders are shown correctly before the image loads and the fade-in animation starts.
    * AC4: Performance of the grid remains smooth during loading and animation of multiple items.

---

### Story 9.3: Animate Appearance of Empty/Error/Loading States

-   **User Story / Goal:** As a user, when the app transitions to an empty state (no search results), an error state, or a full-screen loading state, I want these state indicators to appear with a subtle animation, making the transition less jarring.
-   **Detailed Requirements:**
    * Wrap the Composables responsible for displaying full-screen/central states (e.g., `LoadingIndicator` when initial search is loading, `ErrorView` for API/network errors, "No Results" message) with `AnimatedVisibility`.
    * Apply a standard entrance animation (e.g., `fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 10 })`) and exit animation (`fadeOut` + `slideOutVertically`).
    * Ensure animations are consistent for similar state transitions.
-   **Acceptance Criteria (ACs):**
    * AC1: The full-screen loading indicator animates in smoothly when it becomes visible.
    * AC2: The `ErrorView` Composable animates in smoothly when it becomes visible.
    * AC3: The "No Results" message Composable animates in smoothly when it becomes visible.
    * AC4: The animations are brief (e.g., ~300ms) and use standard easing curves for a polished feel.
    * AC5: Exit animations are also applied when these states are hidden.

---

### Story 9.4: Implement General Screen Transitions (Optional & Subtle)

-   **User Story / Goal:** As a user, I want general navigation between different major sections or screens of the app to feel smooth and polished with subtle transitions, if not already covered by more specific animations like shared elements.
-   **Detailed Requirements:**
    * In the `NavHost` setup (`core/navigation/NavGraph.kt`), define `enterTransition`, `exitTransition`, `popEnterTransition`, and `popExitTransition` for navigation actions between main screens (e.g., from Home/Search to Results if they are separate, or from Results to non-shared-element Detail views if applicable).
    * Use simple, standard transitions like `fadeIn`, `fadeOut`, `slideInHorizontally`, `slideOutHorizontally` with appropriate `tween` specifications.
    * These transitions should be globally consistent for standard navigation operations.
    * Shared element transitions (Story 9.1) will take precedence for specific image navigation.
-   **Acceptance Criteria (ACs):**
    * AC1: Standard navigation between screens (not covered by shared element transitions) uses defined, subtle animations (e.g., fade or slight slide).
    * AC2: These screen transitions are performant and consistent across the application.
    * AC3: Transitions do not make the app feel slow or heavy.

---

### Story 9.5: Enhance Interactive Feedback with Minor Animations

-   **User Story / Goal:** As a user, I want interactive elements like buttons or list items to provide subtle animated feedback on touch, enhancing the sense of responsiveness and polish.
-   **Detailed Requirements:**
    * Ensure standard Material Design touch ripples are enabled and consistently visible on all clickable Composables (Buttons, Card, `Modifier.clickable`). This is often default behavior with Material components but should be verified.
    * Optionally, for `ImageItem` in the grid, consider adding a slight scale-down effect on press using `Modifier.pointerInput` and `animateFloatAsState` or `InteractionSource`.
        * Example: `val scale by interactionSource.collectIsPressedAsState().let { if (it.value) 0.95f else 1f }` then apply with `Modifier.scale(scale)`.
    * Any such custom interaction animations must be very subtle, performant, and not interfere with the primary action (e.g., navigation).
-   **Acceptance Criteria (ACs):**
    * AC1: Material touch ripples are present and provide clear feedback on standard clickable elements.
    * AC2: If implemented for `ImageItem` or other specific elements, a subtle scale/visual animation occurs on press, enhancing interactivity.
    * AC3: All interactive feedback animations are highly performant and do not make UI elements feel sluggish.

---

## Change Log

| Change        | Date       | Version | Description                                                    | Author             |
| ------------- | ---------- | ------- | -------------------------------------------------------------- | ------------------ |
| Initial Draft | 2025-05-10 | 0.1     | First draft of Rich UI Animations & Transitions epic. | Product Manager AI |
| Architecture Refinement | 2025-05-10 | 0.2     | Added technical details for Compose animations, shared elements, and architectural considerations. | Architect AI |