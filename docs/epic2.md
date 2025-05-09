
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

