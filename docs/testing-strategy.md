
# PexelsSample Testing Strategy

## Overall Philosophy & Goals

The testing strategy for PexelsSample aims to ensure the application is robust, reliable, and maintainable. We will follow the "Testing Pyramid" principle, emphasizing a large base of fast unit tests, a moderate number of integration tests, and a smaller set of UI/end-to-end tests for critical user flows. Automation is key, with tests integrated into the CI/CD pipeline to provide rapid feedback.

-   **Goal 1:** Achieve high code coverage (>70-80%) for ViewModels, Repositories, and UseCases via unit tests.
-   **Goal 2:** Prevent regressions in core functionality (search, pagination, detail view, error handling) through a combination of integration and UI tests.
-   **Goal 3:** Enable confident refactoring and continuous development by having a solid test suite.
-   **Goal 4:** Ensure the application handles API interactions, including error states and edge cases, correctly.
-   **Goal 5:** Validate UI behavior and state changes in Jetpack Compose. [41]

## Testing Levels

### 1. Unit Tests

-   **Scope:** Test individual functions, methods, classes, or Composables in isolation. Focus on business logic within ViewModels, transformations in Mappers, logic in Repositories (with mocked dependencies), and individual utility functions or simple Composables.
-   **Tools:**
    -   JUnit 4 (`junit:junit`): Core testing framework. [39]
    -   MockK (`io.mockk:mockk`): For creating mocks and verifying interactions with dependencies (e.g., mocking Repository in ViewModel tests, mocking API service/DAO in Repository tests).
    -   Turbine (`app.cash.turbine:turbine`): For testing Kotlin Flows, especially `StateFlow` and `SharedFlow` emissions from ViewModels.
    -   Kotlin Coroutines Test (`org.jetbrains.kotlinx:kotlinx-coroutines-test`): For testing suspend functions and managing CoroutineDispatchers in tests.
-   **Location:** `app/src/test/java/com/nguyenmoclam/pexelssample/` (mirroring the main source set structure).
-   **Key Areas:**
    -   **ViewModels (`SearchViewModel`, etc.):** Test state changes in response to events/actions, interactions with Repository/UseCases, Flow emissions, validation logic.
    -   **Repositories (`ImageRepositoryImpl`):** Test logic for fetching data from API vs. cache, data transformations (with mocked API service and DAO).
    -   **UseCases (if implemented):** Test specific business logic rules.
    -   **Mappers:** Test accurate conversion between DTOs, Domain Models, and Cache Entities.
    -   **Utility functions.**
-   **Expectations:**
    -   Fast execution, run on every commit locally and in CI.
    -   No Android framework dependencies (true unit tests running on JVM).
    -   Cover all significant logic paths and edge cases within a unit.

### 2. Integration Tests

-   **Scope:** Verify the interaction and collaboration between multiple components of the application. This includes testing how different layers (ViewModel-Repository, Repository-API, Repository-Database) work together.
-   **Tools:**
    -   AndroidX Test JUnit (`androidx.test.ext:junit`): For running tests on an Android device or emulator.
    -   Hilt Android Testing (`com.google.dagger:hilt-android-testing`): For testing Hilt-injected components in an instrumented environment. Allows replacing or injecting test doubles for certain dependencies.
    -   OkHttp MockWebServer (`com.squareup.okhttp3:mockwebserver`): To mock Pexels API responses and test the Retrofit service and Repository's network interaction logic, including error handling and parsing. [40]
    -   Room (In-memory database): For testing DAO and Repository interactions with the database without affecting the actual device database.
    -   Espresso (`androidx.test.espresso:espresso-core`): Can be used for very specific integration scenarios if needed, though primarily focus is on non-UI integration.
    -   Robolectric (Optional): Could be considered if faster execution of Android-dependent integration tests (without an emulator) is desired, but adds complexity. For MVP, emulator-based tests are preferred.
-   **Location:** `app/src/androidTest/java/com/nguyenmoclam/pexelssample/` (mirroring main structure).
-   **Key Areas:**
    -   **Repository with Mocked API:** Testing the full Repository logic including API call, parsing (with MockWebServer), and interaction with a real (in-memory) Room DAO.
    -   **DAO (Room):** Testing database CRUD operations against an in-memory Room database.
    -   **ViewModel with Mocked Repository (Instrumented):** If testing interactions that require Android context not easily available in JVM unit tests.
    -   **PexelsApiService with MockWebServer:** Ensuring requests are formatted correctly and responses are parsed as expected.
-   **Expectations:**
    -   Slower than unit tests as they may run on an emulator/device.
    -   Focus on the contracts and interactions between components.
    -   Run in CI, perhaps less frequently than unit tests if build time is a concern (e.g., on PR merges).

### 3. UI Tests (Jetpack Compose)

-   **Scope:** Test user flows and UI behavior from the user's perspective. Interact with Jetpack Compose UI elements and verify that the UI reacts correctly to state changes from ViewModels.
-   **Tools:**
    -   Jetpack Compose UI Test JUnit4 (`androidx.compose.ui:ui-test-junit4`). [41, 65]
    -   Hilt Android Testing (`com.google.dagger:hilt-android-testing`): To provide test ViewModels or mock dependencies for UI tests.
    -   AndroidX Test Rules (`androidx.test:rules`) and Runners (`androidx.test:runner`).
-   **Location:** `app/src/androidTest/java/com/nguyenmoclam/pexelssample/ui/`
-   **Key User Flows to Test:**
    -   Searching for an image and verifying results are displayed.
    -   Pagination: Scrolling to load more results.
    -   Navigating to the image detail screen and verifying image/metadata.
    -   Display of loading states.
    -   Display of error states (network error, API error, no results) and retry functionality.
    -   Empty states (e.g., no search results).
-   **Expectations:**
    -   Most expensive tests, run on emulators/devices.
    -   Focus on critical user paths and UI correctness.
    -   Ensure Composables correctly observe and react to ViewModel state changes.
    -   Use semantic properties and test tags for robust node finding.
    -   Run in CI, typically before releases or on a nightly schedule if very time-consuming.

## Test Data Management

-   **Unit Tests:** Use hardcoded sample data, or generate data within the test itself. MockK can return specific objects.
-   **Integration Tests (API):** MockWebServer will serve predefined JSON responses from files (e.g., in `app/src/androidTest/assets/api_responses/`) or constructed in code.
-   **Integration Tests (DB):** For Room DAO tests, insert predefined data into the in-memory database before each test and clear it afterwards.
-   **UI Tests:** May require mocking repository responses to provide consistent data for UI validation, or use MockWebServer for API interactions.

## CI/CD Integration

-   **Workflow:**
    1.  **On Push/Pull Request (to develop/main branches):** Run all unit tests. If they pass, optionally run integration tests.
    2.  **On Merge (to develop/main) / Nightly Builds:** Run all unit tests, integration tests, and UI tests.
-   **Failure Policy:** A failing test at any level should fail the CI/CD build pipeline, preventing merges or deployments until fixed.
-   **Reporting:** Test results and code coverage reports (e.g., via JaCoCo integrated with SonarQube or Codecov) should be generated and made accessible.

## Specialized Testing Types (Future Considerations, Post-MVP)

-   **Performance Testing:** (Not MVP) If UI jank or slow loading becomes an issue, use Jetpack Compose performance profiling tools (e.g., Layout Inspector, Recomposition counts) and Android Studio profilers.
-   **Accessibility Testing:** (Basic considerations for MVP as per PRD [29]) Use Compose tooling and manual checks for basic accessibility (contrast, touch targets). More formal accessibility testing with tools like Google's Accessibility Scanner can be added post-MVP.
-   **Visual Regression Testing:** (Not MVP) Tools like Paparazzi (from Cash App) or other screenshot testing libraries could be used to catch unintended UI changes.

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft outlining the testing strategy for PexelsSample. | Architect AI |

