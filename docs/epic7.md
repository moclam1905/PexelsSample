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