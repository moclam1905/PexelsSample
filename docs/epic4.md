
# Epic 4: Image Detail View

**Goal:** To allow users to tap on an image in the search results and view it in a dedicated, larger detail screen, displaying the image itself and relevant metadata.

## Story List

### Story 4.1: Navigation to Image Detail Screen

-   **User Story / Goal:** As a user, when I tap on an image thumbnail in the search results, I want to be taken to a new screen showing a larger version of that image and its details.
-   **Detailed Requirements:**
    * Modify the `ImageItem` Composable (from Story 3.2) in the search results grid/list to be clickable.
    * On click, navigate to a new `ImageDetailScreen` Composable.
    * Pass the necessary data for the selected `Photo` to the `ImageDetailScreen`. This can be done by passing the `Photo` object (if small enough and Parcelable/Serializable) or key identifiers (like photo ID, then re-fetch or find from a cache, though passing the object is simpler for MVP if already fetched). Given the `Photo` object is already available from the list, passing it directly (or its relevant fields like image URLs and metadata) via navigation arguments is preferred.
    * Ensure the navigation setup (from Story 1.5) is extended to include the `ImageDetailScreen` route and argument handling.
-   **Acceptance Criteria (ACs):**
    * AC1: Tapping an `ImageItem` in the search results navigates the user to the `ImageDetailScreen`.
    * AC2: The relevant data for the selected `Photo` (e.g., image URLs, photographer name, alt text) is successfully passed to and available in the `ImageDetailScreen`.
    * AC3: The navigation stack is correctly managed (i.e., pressing back from detail screen returns to results).

---

### Story 4.2: Image Detail Screen UI Layout

-   **User Story / Goal:** As a developer, I want to create the basic UI layout for the `ImageDetailScreen`, so that it can display a large image and associated metadata clearly.
-   **Detailed Requirements:**
    * Create a new Composable function for `ImageDetailScreen(photoData: Photo)`.
    * The layout should prominently feature an area for displaying the large image.
    * Include sections or `Text` Composables to display metadata such as:
        * Photographer's name (`Photo.photographer`).
        * Alt text or description if available (`Photo.alt`).
    * Include a clear way to navigate back (e.g., a back arrow icon in an app bar, or relying on system back).
    * The screen should be scrollable if the content (image + metadata) exceeds the screen height, although for MVP the image might be constrained to fit.
-   **Acceptance Criteria (ACs):**
    * AC1: An `ImageDetailScreen` Composable is created and accepts photo data.
    * AC2: The layout includes a prominent area for the main image.
    * AC3: The layout includes designated areas/Text Composables for photographer name and alt text.
    * AC4: A standard back navigation mechanism is present and functional.

---

### Story 4.3: Display Full-Resolution Image

-   **User Story / Goal:** As a user, on the image detail screen, I want to see a larger, higher-quality version of the selected image, so that I can appreciate its details.
-   **Detailed Requirements:**
    * Use the image loading library (e.g., Coil) to load and display the image in the designated area on `ImageDetailScreen`.
    * Load a larger version of the image from `Photo.src` (e.g., `Photo.src.large2x` or `Photo.src.original`). Choose a size appropriate for a detail view that balances quality and loading time.
    * Display a placeholder while the full image is loading.
    * Handle image loading errors gracefully (e.g., show an error message or placeholder).
    * The image should be scaled appropriately to fit the allocated space, maintaining its aspect ratio.
-   **Acceptance Criteria (ACs):**
    * AC1: The image loading library is used to display the image on `ImageDetailScreen`.
    * AC2: A larger, higher-resolution version of the image (compared to the thumbnail) is loaded.
    * AC3: Placeholders are shown during loading, and error states are handled for the image.
    * AC4: The image maintains its aspect ratio and is displayed clearly.

---

### Story 4.4: Display Image Metadata

-   **User Story / Goal:** As a user, on the image detail screen, I want to see information about the image, such as the photographer's name, so that I can learn more about its origin.
-   **Detailed Requirements:**
    * Display the photographer's name (`Photo.photographer`) using a `Text` Composable.
    * Display the image's alternative text (`Photo.alt`) if available and non-empty, using a `Text` Composable. This can serve as a description.
    * (Optional for MVP, but good if simple) Make the photographer's name clickable to open their Pexels profile URL (`Photo.photographer_url`) in a web browser (using an Intent).
    * (Optional for MVP) Display the original image dimensions (`Photo.width`, `Photo.height`) if desired.
-   **Acceptance Criteria (ACs):**
    * AC1: The photographer's name is displayed on the screen.
    * AC2: The image's alt text (if available) is displayed.
    * AC3: If implemented, tapping the photographer's name opens their Pexels profile URL.
    * AC4: Displayed metadata is clearly legible.

---

## Change Log

| Change        | Date       | Version | Description                                   | Author         |
| ------------- | ---------- | ------- | --------------------------------------------- | -------------- |
| Initial Draft | 2025-05-08 | 0.1     | First draft of image detail view implementation epic | Product Manager AI |
