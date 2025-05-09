
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

