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