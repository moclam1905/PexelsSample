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