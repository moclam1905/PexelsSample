
# PexelsSample Coding Standards and Patterns

## Architectural / Design Patterns Adopted

-   **MVVM (Model-View-ViewModel):** The core architectural pattern for separating UI (Composables), UI logic and state (ViewModels), and data operations (Repositories).
    -   *Rationale/Reference:* Chosen for its strong support in Android Jetpack, clear separation of concerns, and testability. Detailed in `docs/architecture.md`. [59]
-   **Repository Pattern:** Used to abstract data sources (`ImageRepository` interface in `domain`, implementation in `data`).
    -   *Rationale/Reference:* Decouples ViewModels from data source specifics, enhances testability. Detailed in `docs/architecture.md`.
-   **Dependency Injection (Hilt):** Used for managing and providing dependencies throughout the application.
    -   *Rationale/Reference:* Reduces boilerplate, improves modularity and testability. (Story 1.7) Detailed in `docs/architecture.md`.
-   **Clean Architecture Principles (Layering):** The project structure separates concerns into `data`, `domain`, and `ui` layers.
    -   *Rationale/Reference:* Promotes maintainability, testability, and independence of business logic from framework details. Detailed in `docs/project-structure.md`.
-   **Single Activity Architecture:** The app uses one `MainActivity` hosting all Composable screens.
    -   *Rationale/Reference:* Modern Android practice, simplifies navigation with Jetpack Compose. Detailed in `docs/architecture.md`.
-   **Use Cases (Interactors - Optional but Recommended):** For encapsulating specific pieces of business logic within the `domain` layer.
    -   *Rationale/Reference:* Makes business logic more explicit, reusable, and easier to test. Improves clarity for complex operations.

## Coding Standards

-   **Primary Language:** Kotlin `2.0.21` (as per `libs.versions.toml`). [54, 58]
-   **UI Toolkit:** Jetpack Compose (BOM `2024.09.00` as per `libs.versions.toml`). [54, 60]
-   **Style Guide & Linter:**
    -   Kotlin Coding Conventions: Adhere to the official Kotlin style guide ([https://kotlinlang.org/docs/coding-conventions.html](https://kotlinlang.org/docs/coding-conventions.html)).
    -   Android Studio Formatter: Use the default Android Studio Kotlin formatter.
    -   Linters: Utilize Android Lint (via Gradle) and potentially add `ktlint` for stricter style enforcement if desired by the development team. Configuration for `ktlint` would be in `build.gradle.kts`.
-   **Naming Conventions:**
    -   Packages: `lowercase_separated_by_dots` (e.g., `com.nguyenmoclam.pexelssample.ui.home`).
    -   Classes & Interfaces: `PascalCase` (e.g., `SearchViewModel`, `ImageRepository`).
    -   Composable Functions: `PascalCase` (e.g., `HomeScreen`, `ImageItem`).
    -   Functions (non-Composable) & Methods: `camelCase` (e.g., `searchPhotos`, `loadNextPage`).
    -   Variables (including `val` and `var`): `camelCase` (e.g., `photoList`, `isLoading`).
    -   Constants (`const val`, top-level or object `val`): `UPPER_SNAKE_CASE` (e.g., `API_BASE_URL`, `DEFAULT_PAGE_SIZE`).
    -   Layout Files (XML, if any, e.g., for `themes.xml`, `colors.xml`): `snake_case.xml`.
    -   Drawable Resources: `snake_case.xml` or `snake_case.png`.
    -   Hilt Modules: `{Purpose}Module` (e.g., `NetworkModule`, `DatabaseModule`).
    -   Test Methods: `` `should {behavior} when {condition}` `` (using backticks for readability) or `verbNoun` (e.g., `testSearchSuccess`, `search_returnsEmpty_whenNoResults`).
-   **File Structure:** Adhere to the layout defined in `docs/project-structure.md`.
    -   Kotlin files should generally contain a single public class/interface/object that matches the filename. Extension functions or related private top-level declarations can be included if closely related.
-   **Asynchronous Operations:**
    -   Exclusively use Kotlin Coroutines for all asynchronous tasks (networking, database I/O, complex computations). [72]
    -   Launch coroutines from `viewModelScope` in ViewModels.
    -   Use `Dispatchers.IO` for I/O-bound work and `Dispatchers.Default` for CPU-bound work within Repositories or UseCases.
    -   Utilize `suspend` functions for asynchronous operations.
    -   Prefer Kotlin `Flow` (especially `StateFlow` and `SharedFlow`) for exposing reactive streams of data from Repositories to ViewModels, and from ViewModels to Composables.
-   **Type Safety & Nullability:**
    -   Leverage Kotlin's type system to ensure null safety. Avoid platform types where possible.
    -   Explicitly handle nullable types using safe calls (`?.`), Elvis operator (`?:`), or non-null assertions (`!!`) only when nullability is impossible and documented.
    -   Strive for non-nullable types in domain models and internal logic where practical.
-   **Comments & Documentation:**
    -   Use KDoc for documenting public classes, functions, and properties, especially in the `domain` layer and for reusable UI components or utilities.
    -   Explain complex logic, non-obvious decisions, or workarounds with inline comments.
    -   Keep comments concise and focused on *why* something is done, rather than *what* is being done (which the code should make clear).
    -   Ensure READMEs are updated as necessary.
-   **Dependency Management:**
    -   Use the `libs.versions.toml` (Version Catalog) for managing all dependency versions.
    -   Add new dependencies only after team discussion or architect approval, considering their impact on app size, build time, and maintenance.
    -   Regularly review and update dependencies to their latest stable versions, addressing breaking changes.
-   **Immutability:**
    -   Prefer immutable data structures (`val`, `List`, `Map`, immutable data classes) wherever possible, especially for state exposed by ViewModels and for domain models. This helps in predictable state management with Compose.
    -   Use `toMutableList()` or similar if a mutable copy is needed for local manipulation.
-   **Jetpack Compose Best Practices:**
    -   Keep Composables small, reusable, and focused on a single responsibility.
    -   Hoist state to the lowest common ancestor Composable that needs it (State Hoisting).
    -   Pass only necessary data to Composables. Avoid passing entire ViewModels unless for specific Hilt `@HiltViewModel` injection points.
    -   Use `remember` for storing state that survives recomposition. Use `rememberSaveable` for state that needs to survive configuration changes or process death if not handled by ViewModel.
    -   Optimize performance by minimizing unnecessary recompositions (e.g., ensure stability of Composable parameters, use lambdas for deferred execution).
    -   Preview Composables using `@Preview` annotation during development.
    -   Refer to official Jetpack Compose guidelines: [https://developer.android.com/jetpack/compose/mental-model](https://developer.android.com/jetpack/compose/mental-model)

## Error Handling Strategy (General Approach)

-   **Exceptions for Exceptional Situations:** Use Kotlin exceptions to signal errors.
-   **Sealed Classes for Typed Errors:** Define sealed classes/interfaces to represent specific error types that the UI or calling layer might need to handle differently (e.g., `NetworkError`, `ApiError`, `NotFoundError`). This is often part of a `Resource` or `Result` wrapper.
    ```kotlin
    // Example Result Wrapper
    sealed class ResultWrapper<out T> {
        data class Success<out T>(val value: T) : ResultWrapper<T>()
        data class GenericError(val code: Int? = null, val message: String? = null) : ResultWrapper<Nothing>()
        object NetworkError : ResultWrapper<Nothing>()
    }
    ```
-   **ViewModel State:** ViewModels should catch exceptions from Repositories/UseCases and map them to specific UI error states (e.g., a `String` for a message, a boolean flag, or an instance of the error sealed class). These states are then observed by the UI.
-   **UI Feedback:**
    -   Display user-friendly error messages using a common `ErrorView` Composable. (Story 5.1)
    -   Provide "Retry" options where appropriate.
-   **Logging:**
    -   Library: Use Android's built-in `android.util.Log` for development logging. Consider a more structured logging library (e.g., Timber) if advanced features like tree planting for release/debug builds are needed, but `Log` is sufficient for MVP.
    -   Levels: Use `Log.d` for debug, `Log.i` for info, `Log.w` for warnings, `Log.e` for errors (especially caught exceptions).
    -   Context: Include class and method names where practical (e.g., using `Log.TAG`). Log relevant data for diagnosing issues but avoid logging sensitive PII.
-   **Specific Handling Patterns:**
    -   **Network & API Calls:** Repositories should wrap API calls in `try-catch` blocks to handle `IOExceptions` (network issues) or `HttpExceptions` (API errors from Retrofit). Map these to the `ResultWrapper` or specific error types. Implement retry logic with backoff for transient errors if Pexels API guidelines suggest it.
    -   **Input Validation:** Perform input validation in ViewModels (e.g., for search query length) before initiating actions.
    -   **Graceful Degradation:** If an API call fails, the app should clearly communicate the error rather than crashing. Cached data might be shown if available and appropriate.

## Security Best Practices

-   **API Key Management:** The Pexels API key MUST NOT be hardcoded in source files or committed to version control. Store it in the local `gradle.properties` file (added to `.gitignore`) and expose it via `BuildConfig` fields. (Story 1.2) [25, 56, 71]
    ```gradle
    // In local gradle.properties (DO NOT COMMIT THIS FILE if it contains the actual key)
    PEXELS_API_KEY="YOUR_ACTUAL_API_KEY"

    // In app/build.gradle.kts
    android {
        // ...
        defaultConfig {
            // ...
            buildConfigField("String", "PEXELS_API_KEY", "\"${System.getenv("PEXELS_API_KEY") ?: project.findProperty("PEXELS_API_KEY") ?: ""}\"")
        }
    }
    ```
    *(The CI/CD environment would need to provide `PEXELS_API_KEY` as an environment variable).*
-   **Input Sanitization/Validation:** While not a primary concern for consuming a trusted API like Pexels for search terms, ensure any user input used in API requests is reasonably validated (e.g., length, basic format).
-   **Network Security:** Use HTTPS for all Pexels API communications (Retrofit default with valid URLs).
-   **Dependency Security:** Regularly update dependencies to patch known vulnerabilities. Tools like GitHub's Dependabot can automate this.
-   **Data Storage:** No highly sensitive user data is stored locally in this MVP. If future features add sensitive data, encryption (e.g., using Jetpack Security / Tink) for Room or SharedPreferences would be necessary.
-   **Permissions:** Request only necessary Android permissions. For this app, `android.permission.INTERNET` is the primary one.

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft covering key coding and pattern guidelines. | Architect AI |

