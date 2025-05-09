# Epic 1 file

# Epic 1: Core Application Setup & API Integration Foundation

**Goal:** To establish the foundational elements of the PexelsSample Android application, including project structure, build configurations, core library integrations (networking, database), secure API key management, basic navigation, and initial data models for Pexels API interaction. This epic will ensure a runnable application shell with the necessary groundwork for subsequent feature development.

## Story List

### Story 1.1: Android Project Initialization and Structure

-   **User Story / Goal:** As a Developer, I want a new Android Studio project initialized with Kotlin and Jetpack Compose, so that I have a clean starting point adhering to modern Android development standards.
-   **Detailed Requirements:**
    * Initialize a new Android project using Android Studio.
    * Configure the project to use Kotlin as the primary language.
    * Set up Jetpack Compose for UI development.
    * Define basic package structure for separation of concerns (e.g., `ui`, `data`, `domain`, `di`, `utils`).
    * Include necessary Gradle plugins and configurations for Kotlin, Compose, and application building.
    * Ensure the project builds successfully and runs a basic "Hello World" Composable on an emulator/device.
    * Establish basic version control (e.g., `.gitignore` file).
-   **Acceptance Criteria (ACs):**
    * AC1: A new Android project is created and configured for Kotlin and Jetpack Compose.
    * AC2: The project includes a defined package structure for `ui`, `data`, `domain`.
    * AC3: The app successfully builds and displays a simple Composable screen.
    * AC4: A `.gitignore` file appropriate for Android projects is present.

---

### Story 1.2: Secure API Key Management

-   **User Story / Goal:** As a Developer, I want to securely store and access the Pexels API key, so that it is not exposed in version control or easily accessible in the compiled application.
-   **Detailed Requirements:**
    * Store the Pexels API key in a way that it's not hardcoded directly in source files committed to version control (e.g., using `gradle.properties` and `BuildConfig` fields).
    * Provide a mechanism for the application to access this API key at runtime for API requests.
    * Ensure the file containing the API key (e.g., local `gradle.properties`) is listed in `.gitignore`.
-   **Acceptance Criteria (ACs):**
    * AC1: The Pexels API key is stored outside of version-controlled code files.
    * AC2: The application can retrieve the API key at runtime.
    * AC3: The method used prevents the key from being easily discoverable in the APK (e.g., not a plain string in `BuildConfig` if more obfuscation is simple to add, though `BuildConfig` from `gradle.properties` is a common first step).
    * AC4: The file holding the actual key is included in `.gitignore`.

---

### Story 1.3: Networking Layer Setup (Pexels API)

-   **User Story / Goal:** As a Developer, I want a networking layer configured with Retrofit (or a similar library like Ktor) and an OkHttp client, so that the application can make HTTP requests to the Pexels API.
-   **Detailed Requirements:**
    * Add Retrofit (or Ktor) and OkHttp dependencies to the project.
    * Configure a Retrofit instance with the Pexels API base URL (`https://api.pexels.com/v1/`).
    * Configure an OkHttp client, including an interceptor to add the Pexels API key (from Story 1.2) to the `Authorization` header of requests.
    * Define a basic Pexels API service interface (e.g., `PexelsApiService`) with a placeholder endpoint (e.g., a suspend function for `/v1/curated?per_page=1`).
    * Set up JSON parsing (e.g., using `kotlinx.serialization` or `Moshi`) for API responses.
-   **Acceptance Criteria (ACs):**
    * AC1: Retrofit (or Ktor) and OkHttp dependencies are added and configured.
    * AC2: An OkHttp interceptor correctly adds the API key to request headers.
    * AC3: A basic `PexelsApiService` interface is defined.
    * AC4: The setup can make a simple, authenticated API call to a Pexels endpoint (e.g., `/v1/curated`) and receive a success or failure response without crashing. (Actual data parsing can be verified in Story 1.4).

---

### Story 1.4: Core Data Models for Pexels API

-   **User Story / Goal:** As a Developer, I want Kotlin data classes defined to represent the structure of Pexels API responses (e.g., Photo, Src), so that API responses can be deserialized into usable objects.
-   **Detailed Requirements:**
    * Based on the Pexels API documentation (e.g., for search results, photo details), define Kotlin data classes.
        * `Photo` (id, width, height, url, photographer, photographer_url, photographer_id, avg_color, src, liked, alt)
        * `PhotoSrc` (original, large2x, large, medium, small, portrait, landscape, tiny)
        * Potentially a wrapper class for the search/curated photos response list (e.g., `PexelsApiResponse` containing a list of `Photo` objects, page, per_page, total_results, next_page).
    * Annotate data classes for JSON deserialization using the chosen library (`kotlinx.serialization` or `Moshi`).
-   **Acceptance Criteria (ACs):**
    * AC1: Kotlin data classes for `Photo`, `PhotoSrc`, and a response wrapper are created.
    * AC2: Data classes correctly map to the Pexels API JSON structure.
    * AC3: A sample JSON response from Pexels can be successfully deserialized into these data classes using the configured JSON library.

---

### Story 1.5: Basic Navigation Structure with Jetpack Compose

-   **User Story / Goal:** As a Developer, I want a basic navigation graph set up using Jetpack Navigation Component for Compose, so that the app can navigate between different screens (even if they are placeholders initially).
-   **Detailed Requirements:**
    * Add Jetpack Navigation Compose dependency.
    * Define a `NavHost` and a `NavController`.
    * Create at least two placeholder Composable screens:
        * An initial screen (e.g., `HomeScreen` which might later show curated photos or be the search entry).
        * A placeholder for the search results screen (e.g., `SearchResultsScreen`).
    * Implement navigation from the initial screen to the placeholder search results screen (e.g., on a button click).
-   **Acceptance Criteria (ACs):**
    * AC1: Jetpack Navigation Compose is integrated into the project.
    * AC2: A `NavHost` controller is set up with at least two Composable destinations.
    * AC3: User can navigate from the initial screen to the second placeholder screen.

---

### Story 1.6: Room Database Initialization

-   **User Story / Goal:** As a Developer, I want Room database initialized in the project, so that local data persistence for caching and future features like bookmarks is available.
-   **Detailed Requirements:**
    * Add Room dependencies (runtime, compiler, KTX).
    * Define a simple placeholder Room Entity (e.g., `PlaceholderEntity` or a basic `CachedImageEntity`).
    * Define a corresponding DAO (Data Access Object) for the entity with no methods initially or a simple insert/get.
    * Create a Room `Database` class.
    * Instantiate the database in the Application class or via DI.
-   **Acceptance Criteria (ACs):**
    * AC1: Room dependencies are added and correctly configured.
    * AC2: A placeholder Room Entity and DAO are defined.
    * AC3: A Room Database class is created and can be instantiated without errors.
    * AC4: The app compiles and runs with the basic Room setup.

---

### Story 1.7: Dependency Injection Setup (Hilt)

-   **User Story / Goal:** As a Developer, I want Hilt setup for dependency injection, so that dependencies can be managed effectively and code remains decoupled and testable.
-   **Detailed Requirements:**
    * Add Hilt dependencies and configure the Hilt Gradle plugin.
    * Annotate the Application class with `@HiltAndroidApp`.
    * Create basic Hilt modules for providing application-level dependencies (e.g., `Context`, Retrofit instance, Database instance).
    * Ensure ViewModels (if any prototyped yet, or plan for their injection) can be injected using Hilt.
-   **Acceptance Criteria (ACs):**
    * AC1: Hilt is correctly configured in the project.
    * AC2: The Application class is annotated with `@HiltAndroidApp`.
    * AC3: Basic dependencies (like `Context` or a test service) can be injected into an `@AndroidEntryPoint` annotated Activity/Fragment or a Hilt ViewModel.
    * AC4: The app compiles and runs with Hilt setup.

---

## Change Log

| Change        | Date       | Version | Description                  | Author         |
| ------------- | ---------- | ------- | ---------------------------- | -------------- |
| Initial Draft | 2025-05-08 | 0.1     | First draft of foundational epic stories | Product Manager AI |


---
# Epic 2 file


# Epic 2: Image Search Implementation

**Goal:** To enable users to input search keywords and initiate an image search request to the Pexels API. This epic focuses on creating the search input UI, managing its state, and triggering the API call, preparing for results display in a subsequent epic.

## Story List

### Story 2.1: Search Input Screen/UI

-   **User Story / Goal:** As a user, I want a clear and simple interface with a text field and a search button/icon, so that I can easily type my search keywords and initiate an image search.
-   **Detailed Requirements:**
    * Create a dedicated Composable screen or a prominent section on an existing screen for image search.
    * Include a `TextField` Composable for users to input their search query.
        * The `TextField` should have a placeholder text (e.g., "Search for images...").
        * It should allow users to see the text they are typing.
    * Include a `Button` or `IconButton` (e.g., with a search icon) to trigger the search.
    * The search screen/UI should be accessible via the navigation setup in Epic 1 (e.g., as the `HomeScreen` or navigable from it).
    * Basic styling should ensure the input field and button are clearly visible and usable.
-   **Acceptance Criteria (ACs):**
    * AC1: A Composable UI is present with a `TextField` for search input and a `Button` or `IconButton` to initiate search.
    * AC2: The `TextField` displays a placeholder text when empty.
    * AC3: Users can type text into the `TextField`.
    * AC4: The search button is visible and clickable.

---

### Story 2.2: Search ViewModel and State Management

-   **User Story / Goal:** As a Developer, I want a ViewModel to manage the state of the search query and handle the logic for initiating a search, so that the UI remains responsive and state is handled correctly.
-   **Detailed Requirements:**
    * Create a Hilt ViewModel (e.g., `SearchViewModel`) associated with the search input UI.
    * The ViewModel should hold the current search query string as observable state (e.g., using `StateFlow` or `MutableState`).
    * The ViewModel should expose a function to be called when the user initiates a search (e.g., `onSearchClicked()`).
    * The `TextField` in the UI should be two-way bound to the search query state in the ViewModel.
    * The search initiation function in the ViewModel should validate that the search query is not empty before proceeding.
-   **Acceptance Criteria (ACs):**
    * AC1: A `SearchViewModel` is created and injectable using Hilt.
    * AC2: The ViewModel manages the search query string as an observable state.
    * AC3: The UI's `TextField` updates the ViewModel's query state, and changes in ViewModel's query state update the `TextField`.
    * AC4: The ViewModel has a function to trigger search logic, which checks for a non-empty query.

---

### Story 2.3: Trigger Pexels Search API Call

-   **User Story / Goal:** As a Developer, I want the `SearchViewModel` to use the `PexelsApiService` to execute an image search request when a user initiates a search, so that images matching the query can be fetched.
-   **Detailed Requirements:**
    * Inject the `PexelsApiService` (created in Epic 1) into the `SearchViewModel`.
    * When the search is initiated (and the query is valid), the ViewModel should call the appropriate method on `PexelsApiService` (e.g., a new method like `searchPhotos(query: String, page: Int, perPage: Int)`).
        * This will likely require adding a `searchPhotos` suspend function to the `PexelsApiService` interface, targeting `GET /v1/search`.
    * The ViewModel should handle the API call asynchronously (e.g., using Kotlin Coroutines launched in `viewModelScope`).
    * For this story, the primary focus is on successfully making the call. The ViewModel should receive either the deserialized `PexelsApiResponse` (from Story 1.4) or an error.
    * Initial pagination parameters can be hardcoded (e.g., `page = 1`, `perPage = 20`).
-   **Acceptance Criteria (ACs):**
    * AC1: `PexelsApiService` is updated with a method to perform a keyword search.
    * AC2: `SearchViewModel` successfully calls the `searchPhotos` method on `PexelsApiService` with the user's query.
    * AC3: The API call is made asynchronously.
    * AC4: The ViewModel can distinguish between a successful API response (containing data or an empty list) and an API call failure (e.g., network error, API error). (Detailed error *display* is for Epic 5).

---

### Story 2.4: Basic Search State Indication (Loading)

-   **User Story / Goal:** As a user, I want to see a basic loading indicator after I initiate a search, so that I know the application is processing my request.
-   **Detailed Requirements:**
    * The `SearchViewModel` should expose an observable boolean state representing loading status (e.g., `isLoading: StateFlow<Boolean>`).
    * This state should be set to `true` before the API call is made and `false` after the call completes (either successfully or with an error).
    * The search UI should observe this loading state.
    * When `isLoading` is `true`, the UI should display a simple loading indicator (e.g., a `CircularProgressIndicator` Composable).
    * The search input field and/or search button might be disabled while loading.
-   **Acceptance Criteria (ACs):**
    * AC1: `SearchViewModel` exposes an observable loading state.
    * AC2: The loading state is `true` during the API call and `false` otherwise.
    * AC3: The UI displays a visual loading indicator when the loading state is `true`.
    * AC4: The search input or button is disabled during the loading state to prevent multiple submissions.

---

## Change Log

| Change        | Date       | Version | Description                                     | Author         |
| ------------- | ---------- | ------- | ----------------------------------------------- | -------------- |
| Initial Draft | 2025-05-08 | 0.1     | First draft of image search implementation epic | Product Manager AI |

---
# Epic 3 file


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


---
# Epic 4 file


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


---
# Epic 5 file


# Epic 5: Comprehensive State Handling & UI Polish

**Goal:** To ensure the application gracefully handles and clearly communicates all relevant states (loading, content, empty, error) to the user across all features, and to refine the overall UI for a clean, modern, and visually appealing user experience.

## Story List

### Story 5.1: Consistent Error Display Component & Logic

-   **User Story / Goal:** As a user, when something goes wrong (e.g., network error, API error), I want to see a clear, understandable message explaining the problem and, if possible, an option to retry.
-   **Detailed Requirements:**
    * Design and implement a reusable Composable function (e.g., `ErrorView(message: String, onRetry: (() -> Unit)?)`) that can be used to display error states consistently across different screens.
    * The `ErrorView` should display:
        * A user-friendly error message.
        * Optionally, a "Retry" button if an `onRetry` action is provided.
    * Extend ViewModels (`SearchViewModel`, and any potential `ImageDetailViewModel`) to manage and expose specific error states (e.g., distinct states for network errors vs. API errors vs. unknown errors).
    * Integrate this `ErrorView` into screens where data is loaded (Search Results, Image Detail).
-   **Acceptance Criteria (ACs):**
    * AC1: A reusable `ErrorView` Composable is created and can display a message and an optional retry button.
    * AC2: ViewModels expose distinct error states that can be consumed by the UI.
    * AC3: The `ErrorView` is displayed on relevant screens when a corresponding error state occurs in the ViewModel.
    * AC4: The "Retry" button, when present and clicked, triggers the provided retry action in the ViewModel.

---

### Story 5.2: Handle Network Connectivity Errors

-   **User Story / Goal:** As a user, if I try to use the app without an internet connection or if the connection is unstable, I want to be clearly informed about the network issue.
-   **Detailed Requirements:**
    * Implement logic (e.g., in Repositories or Use Cases interacting with the `PexelsApiService`) to specifically detect network connectivity errors (e.g., `UnknownHostException`, `ConnectException`).
    * When a network error is detected, the ViewModel should update its state to reflect a "Network Error".
    * The UI, using the `ErrorView` (from Story 5.1), should display a specific message like "No internet connection. Please check your connection and try again."
    * The "Retry" action should be available for network errors.
-   **Acceptance Criteria (ACs):**
    * AC1: The application can detect and distinguish network connectivity errors.
    * AC2: A specific "Network Error" state is managed by ViewModels.
    * AC3: A user-friendly message regarding network issues is displayed via the `ErrorView`.
    * AC4: A retry mechanism is offered for network errors.

---

### Story 5.3: Handle Pexels API Specific Errors

-   **User Story / Goal:** As a user, if the Pexels API returns an error (e.g., rate limit exceeded, invalid API key, server issue), I want to be informed appropriately.
-   **Detailed Requirements:**
    * Enhance the networking layer/repositories to parse and identify specific HTTP error codes from the Pexels API (e.g., 401/403 for auth issues, 429 for rate limits, 5xx for server errors).
    * ViewModels should update their state to reflect these specific API errors.
    * Display user-friendly messages via the `ErrorView`:
        * For rate limits: "Too many requests. Please try again later." (Retry might be disabled or have a delay).
        * For auth issues (e.g., invalid key, which shouldn't happen with secure key management but good to consider): "There was an issue accessing the service. Please try again later." (Internal error essentially).
        * For general Pexels server errors: "Pexels.com seems to be having issues. Please try again later."
    * Offer a "Retry" option where appropriate (e.g., for transient server errors, but perhaps not for a misconfigured API key).
-   **Acceptance Criteria (ACs):**
    * AC1: The application can identify and distinguish different Pexels API error responses.
    * AC2: Specific API error states are managed by ViewModels.
    * AC3: User-friendly messages corresponding to different API errors are displayed.
    * AC4: Retry mechanism is offered where appropriate for API errors.

---

### Story 5.4: UI Polish and Visual Consistency Review

-   **User Story / Goal:** As a user, I want the application to have a clean, modern, and visually appealing interface that is consistent across all screens.
-   **Detailed Requirements:**
    * Review all existing screens and UI components (Search Input, Results Grid/Items, Detail View, Error/Loading/Empty states).
    * Ensure consistent use of spacing, padding, and margins.
    * Standardize typography (font sizes, weights, styles) for headings, body text, and captions.
    * Ensure a harmonious color palette is applied consistently.
    * Refine iconography for clarity and visual appeal.
    * Check for general alignment and visual hierarchy on each screen.
    * Ensure touch targets are adequately sized.
    * Address any elements that feel cluttered or unrefined.
    * The overall aesthetic should align with "modern Android design" and the "clean, intuitive" vision from the project brief.
-   **Acceptance Criteria (ACs):**
    * AC1: UI elements use consistent spacing and padding throughout the app.
    * AC2: Typography is standardized and applied consistently.
    * AC3: Color usage is consistent and contributes to a clean aesthetic.
    * AC4: Icons are clear, well-chosen, and consistently styled.
    * AC5: Overall UI feels polished, modern, and visually appealing as per the project vision.

---

### Story 5.5: Comprehensive Loading State Refinement

-   **User Story / Goal:** As a user, I want to see clear and non-intrusive loading indicators whenever the app is fetching data, so I understand that activity is in progress.
-   **Detailed Requirements:**
    * Review all data loading operations:
        * Initial search (Story 2.4).
        * Pagination/loading more results (Story 3.4).
        * Loading full image in detail view (Story 4.3).
    * Ensure `CircularProgressIndicator` or other suitable loading indicators are displayed appropriately.
    * For full-screen loads (e.g., initial search), the indicator should ideally be centered.
    * For list pagination, the indicator should be at the end of the list.
    * For in-place content loading (like an image in the detail view), the indicator should overlay the content area.
    * Ensure loading indicators are promptly removed once data is loaded or an error occurs.
    * Ensure content is appropriately hidden or shown when loading states change (e.g., don't show "No results" while initial data is still loading).
-   **Acceptance Criteria (ACs):**
    * AC1: Clear loading indicators are present for all primary data loading operations.
    * AC2: Loading indicators are appropriately styled and positioned for their context.
    * AC3: Loading indicators are promptly shown and hidden in sync with data fetching lifecycles.
    * AC4: The UI correctly transitions between loading, content, empty, and error states without showing conflicting information.

---

## Change Log

| Change        | Date       | Version | Description                                         | Author         |
| ------------- | ---------- | ------- | --------------------------------------------------- | -------------- |
| Initial Draft | 2025-05-08 | 0.1     | First draft of comprehensive state handling & UI polish epic | Product Manager AI |

# END EPIC FILES