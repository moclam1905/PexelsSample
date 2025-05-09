

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

