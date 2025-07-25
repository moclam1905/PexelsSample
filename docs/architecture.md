
# PexelsSample Architecture Document

## 1\. Technical Summary

PexelsSample is a native Android application built using Kotlin `2.0.21` and Jetpack Compose (BOM `2024.09.00`), designed to provide users with a seamless experience for searching and Browse high-quality images via the Pexels API. The architecture follows the Model-View-ViewModel (MVVM) pattern to ensure a clear separation of concerns, enhance testability, and promote a scalable codebase. Key Jetpack libraries such as ViewModel, Navigation Component, Room (for local caching of photo metadata), and Hilt (for dependency injection) are utilized. The application focuses on efficient image loading with Coil, robust error handling, and clear communication of loading and empty states, aiming for a production-quality standard as outlined in the PRD. Target SDK is `35` and Min SDK is `26`. [3, 5, 10, 27, 54, 55, 59, 61]

## 2\. High-Level Overview

The application architecture is centered around the MVVM pattern. The UI (View) is built with Jetpack Compose Composables, observing data and state exposed by ViewModels. ViewModels interact with Repositories to fetch and manage data from the Pexels API and a local Room database cache. User interactions trigger actions in the ViewModel, which updates its state and potentially fetches new data via the Repository. The Repository layer abstracts data source logic. Kotlin Coroutines and Flow are used for asynchronous operations and reactive data streams. Hilt manages dependencies.

The primary user interaction flow involves:
1.  **User launches the app and is presented with the `HomeScreen` displaying trending/curated photos.**
2.  **User can scroll infinitely to load more trending photos or trigger a manual refresh.**
3.  User can tap a trending photo to navigate to the `ImageDetailScreen`.
4.  User can initiate a search (e.g., via a search icon/bar on `HomeScreen` or by navigating to a dedicated search screen).
5.  If searching, the Composable UI (View) sends this action to the `SearchViewModel`.
6.  The `SearchViewModel` requests data from the `ImageRepository`.
7.  The `ImageRepository` fetches image data from the Pexels API (Search endpoint).
8.  The `SearchViewModel` updates its state (e.g., list of photos, loading/error status) exposed via `StateFlow`.
9.  The UI recomposes to display the search results.
10. User can tap an image from search results to navigate to a detail view.

<!-- end list -->

```mermaid
graph TD
    UserInteraction["User Interaction (e.g., Search, Scroll, Tap)"] --> View["View (Jetpack Compose UI - Activity/Composables)"];
    View -- "Observes State / Sends Actions" --> ViewModel["ViewModel (Jetpack ViewModel, Hilt Injected)"];
    ViewModel -- "Requests Data / Executes Logic (UseCases)" --> Repository["Repository (Hilt Injected)"];
    Repository -- "Fetches/Saves Data" --> PexelsAPI["Pexels API (Retrofit + OkHttp)"];
    Repository -- "Caches/Retrieves Data" --> RoomDB["Room Database (Local Cache)"];
    PexelsAPI -- "Provides Image Data" --> Repository;
    RoomDB -- "Provides Cached Data" --> Repository;
    Repository -- "Returns Data/State (Flows)" --> ViewModel;
    ViewModel -- "Exposes State (StateFlow)" --> View;

    subgraph "Presentation Layer (UI - Jetpack Compose)"
        View
        ViewModel
    end

    subgraph "Domain Layer (Optional UseCases, Core Models)"
        %% UseCases can be added here if logic in ViewModel becomes too complex
        DomainModels["Domain Models (Photo, PhotoSrc)"]
    end

    subgraph "Data Layer"
        Repository
        PexelsAPI
        RoomDB
        DataMappers["DTO/Entity Mappers"]
    end

    ViewModel --> DomainModels;
    Repository --> DomainModels;
    Repository --> DataMappers;
    DataMappers --> PexelsAPI;
    DataMappers --> RoomDB;

    UserInteraction-.->|"Interacts with"|View;
    View-.->|"Delegates to"|ViewModel;
    ViewModel-.->|"Uses"|Repository;
    Repository-.->|"Accesses"|PexelsAPI;
    Repository-.->|"Accesses"|RoomDB;
```

## 3\. Component View

The application is structured into Presentation (UI and ViewModel), Domain (core models and optional use cases), and Data (repositories, data sources, mappers) layers.

```mermaid
graph TD
    subgraph "User Interface (View - ui package)"
        MainActivity["MainActivity (Hosts NavGraph)"]
        NavGraph["AppNavigation"]
        Screen_Home["HomeScreen (Displays trending photos in a LazyVerticalStaggeredGrid, handles infinite scroll, refresh, and navigation to search/detail. Uses WindowSizeClass for adaptive columns.)"]
        Screen_SearchResults["SearchResultsScreen"]
        Screen_ImageDetail["ImageDetailScreen"]
        AdaptiveLayoutHost["AdaptiveLayoutHost (e.g., in HomeScreen or Root, uses WindowSizeClass)"]
        Composable_ImageItem["ImageItem (Reusable)"]
        Composable_SearchBar["SearchBar (Reusable)"]
        Composable_ErrorView["ErrorView (Reusable)"]
        Composable_LoadingIndicator["LoadingIndicator (Reusable)"]
    end

    subgraph "Presentation Logic (ViewModel - ui package)"
        VM_Home["HomeScreenViewModel (Manages fetching, pagination, and state for trending/curated photos on HomeScreen. Interacts with ImageRepository.)"]
        VM_Search["SearchViewModel (Manages state for image search functionality, search results, pagination, pull-to-refresh for search, and search history. Interacts with ImageRepository and SearchHistoryRepository.)"]
        VM_ImageDetail["ImageDetailViewModel (Manages image data for ImageDetailScreen, and potentially zoom/pan state if complex)"]
    end

    subgraph "Domain Layer (domain package)"
        Model_Photo["Photo (Domain Model)"]
        Model_PhotoSrc["PhotoSrc (Domain Model)"]
        Interface_ImageRepo["ImageRepository (Interface)"]
        UseCase_Search["SearchPhotosUseCase (Optional)"]
        Interface_SearchHistoryRepo["SearchHistoryRepository (Interface - Optional, or direct use of concrete repo)"]
        UseCase_GetHistory["GetRecentSearchesUseCase (Optional)"]
        UseCase_SaveSearch["SaveSearchTermUseCase (Optional)"]
    end

    subgraph "Data Layer (data package)"
        Repo_ImageImpl["ImageRepositoryImpl"]
        subgraph "Remote Data Source (data.remote)"
            API_Pexels["PexelsApiService (Retrofit)"]
            DTOs["API DTOs (PexelsPhotoDto, etc.)"]
        end
        subgraph "Local Data Source (data.local)"
            DB_Room["PexelsRoomDatabase"]
            DAO_Photo["PhotoDao"]
            CacheEntities["Cache Entities (CachedPhotoEntity)"]
        end
        Mappers["Data Mappers (data.mappers)"]
        Util_ImageLoader["ImageLoader (Coil - used in UI)"]
        Util_NetworkHandler["NetworkConnectivityHandler (core.utils)"]
        Repo_ImageImpl["ImageRepositoryImpl"]
        API_Pexels["PexelsApiService (Retrofit)"]
        DB_Room["PexelsRoomDatabase (Image Cache)"]
    
        DataStore_SearchHistory["Search History DataStore (Jetpack DataStore)"]
        Repo_SearchHistory["SearchHistoryRepository (Hilt Injected, uses DataStore)"]
    end

    subgraph "Cross-Cutting Concerns"
        DI_Hilt["Hilt"]
        CoroutinesFlow["Kotlin Coroutines & Flow"]
        Navigation["Jetpack Navigation Compose (Handles navigation OR a selected item ID for two-pane)"]
        AppTheme["Application Theme"]
        Util_WindowSizeClass["WindowSizeClass Utility (Provides screen size info)"]
    end

    %% HomeScreen Flow
    MainActivity --> NavGraph;
    NavGraph -- "Default Route" --> Screen_Home;
    Screen_Home --> VM_Home;
    VM_Home -- "Gets Curated Photos" --> Interface_ImageRepo;
    Screen_Home -- "Tap Image" --> NavGraph; % Navigates to ImageDetailScreen
    Screen_Home -- "Initiate Search Action" --> NavGraph; % Navigates to SearchScreen or expands search UI

    %% Connections
    AdaptiveLayoutHost -- uses --> Util_WindowSizeClass;
    AdaptiveLayoutHost -- "If Compact/Medium" --> Screen_SearchResults;
    Screen_SearchResults -- "Tap (Compact/Medium)" --> Navigation; 
    Navigation -- "Navigates with PhotoID" --> Screen_ImageDetail;

    AdaptiveLayoutHost -- "If Expanded" ----> |Shows ListPane| Screen_SearchResults;
    AdaptiveLayoutHost -- "If Expanded" ----> |Shows DetailPane| Screen_ImageDetail;

    Screen_SearchResults --> VM_Search;
    Screen_ImageDetail --> VM_ImageDetail;
    Screen_SearchResults -- "Pull-to-Refresh Action" --> VM_Search;
    Composable_SearchBar -- "Focus/Input" --> VM_Search; % For showing history
    VM_Search -- "Fetches/Saves History" --> Repo_SearchHistory; % Or via UseCases
    Repo_SearchHistory -- "Accesses" --> DataStore_SearchHistory;

    VM_Search -- "Refreshes Image Data" --> Repo_ImageImpl; % Existing connection
    VM_Search -- "Provides Photo List" --> Screen_SearchResults;
    VM_Search -- "Provides SelectedPhoto (for Expanded)" --> Screen_ImageDetail; 
    VM_ImageDetail -- "Receives Photo/ID" --> |Fetches/Displays| Model_Photo; 

    VM_Search -- uses --> Interface_ImageRepo; 
    UseCase_Search -- uses --> Interface_ImageRepo;
    Repo_ImageImpl -- implements --> Interface_ImageRepo;
    Repo_ImageImpl --> API_Pexels;
    Repo_ImageImpl --> DAO_Photo;
    Repo_ImageImpl --> Mappers;
    Mappers --> DTOs;
    Mappers --> CacheEntities;
    Mappers --> Model_Photo; 

    DI_Hilt -.-> VM_Search;
    DI_Hilt -.-> Repo_ImageImpl;
    DI_Hilt -.-> API_Pexels;
    DI_Hilt -.-> DB_Room;
    %% etc.
```

  - **`app/src/main/java/com/nguyenmoclam/pexelssample/`**: Root package.
      - **`core/`**: Navigation, general utilities (`NetworkConnectivityHandler`).
      - **`data/`**: Data sources, repositories implementations, DTOs, entities, mappers.
      - **`di/`**: Hilt dependency injection modules.
      - **`domain/`**: Core business logic, domain models, repository interfaces, use cases.
      - **`ui/`**: Composable screens, ViewModels, UI-specific models/state, reusable UI components, theme.
      - (See `docs/project-structure.md` for the full layout).

## 4\. Key Architectural Decisions & Patterns

  - **MVVM (Model-View-ViewModel):** Core pattern for UI, state, and data separation. [59]
  - **Clean Architecture Principles:** Layering into UI, Domain, and Data.
  - **Single Activity Architecture:** Using `MainActivity` with Jetpack Navigation Compose.
  - **Repository Pattern:** `ImageRepository` as the single source of truth for image data.
  - **Dependency Injection with Hilt:** For managing dependencies. (Story 1.7)
  - **Kotlin Coroutines + Flow:** For asynchronous operations and reactive data streams. [72]
  - **Offline Caching (Metadata):** Room for API response metadata; Coil for image files. [55, 68, 69]
  - **Secure API Key Management:** Via `gradle.properties` and `BuildConfig`. (Story 1.2) [25, 56, 71]
  - **Modular UI with Reusable Composables.**
  -   **ViewModel for Complex UI State:** For screens with intricate UI state that needs to survive configuration changes robustly (e.g., zoom/pan state in `ImageDetailScreen`), a dedicated ViewModel leveraging `SavedStateHandle` is preferred over relying solely on `rememberSaveable` within the Composable if state becomes too complex.
  -   **Local Storage with Jetpack DataStore for Search History:** Jetpack DataStore (Preferences or Proto) will be used to store the user's recent search terms locally, providing an asynchronous API for data persistence. [87]
    -   *Justification:* Lightweight and suitable for simple key-value or typed object persistence, recommended for this use case over Room unless complex querying is needed.
  -   **Rich UI Animations & Transitions with Jetpack Compose:** The application will utilize Jetpack Compose's built-in animation APIs (`animate*AsState`, `AnimatedVisibility`, `LookaheadLayout` for shared elements, etc.) to create purposeful and performant UI transitions and feedback, enhancing the user experience. [38, 143, 149]
        -   *Justification:* Improves perceived quality and user engagement. Shared element transitions, in particular, provide visual continuity.
  -   **Engaging Initial Experience with Staggered Grid:** The HomeScreen will use `LazyVerticalStaggeredGrid` to display trending/curated photos, providing a visually dynamic and engaging entry point to the application. [127, 171]
        -   *Justification:* Enhances user engagement and content discovery upon app launch.
  - (See `docs/coding-standards.md` for more on patterns and error handling).

## 5\. Infrastructure and Deployment Overview

  - **Hosting/Cloud Provider:** Not applicable (client-side application). [53]
  - **Deployment:** Standard Android Application Bundle (AAB) or APK generated via Android Studio / Gradle.
  - **CI/CD:** Recommended for production (e.g., GitHub Actions), but not an MVP requirement. Would involve building, running tests, and potentially distributing builds. [62]
  - **Environments:**
      - Local Development: Android Studio, Emulators, Physical Devices.
      - Testing: JUnit for unit tests (JVM), AndroidX Test + Compose Test for instrumented/UI tests.
      - (See `docs/environment-vars.md` for build-time configuration).

## 6. Key Reference Documents

-   `PexelsSample Product Requirements Document (PRD).docx` (or `prd.md` if converted)
-   `epic1.md`
-   `epic2.md`
-   `epic3.md`
-   `epic4.md`
-   `epic5.md`
-   `epic6.md`
-   `epic7.md`
-   `epic8.md`
-   `epic9.md`
-   `epic10.md`
-   `research-homescreen-trending-photos.md`
-   `deep-research-bonus-features.md`
-   `tech-stack.md`
-   `project-structure.md`
-   `coding-standards.md`
-   `api-reference.md`
-   `data-models.md`
-   `environment-vars.md`
-   `testing-strategy.md`
-   `ui-ux-spec.md` (To be created if detailed UI mockups are designed)

## 7\. Change Log

| Change        | Date       | Version | Description                                                                     | Author     |
| :------------ |:-----------|:--------|:--------------------------------------------------------------------------------| :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial architecture document based on PRD, epics, and collaborative decisions. | Architect AI |
| Bonus Features | 2025-05-10 | 1.1     | Bonus Features                                                                  | Architect AI |
| Trending Photos on HomeScreen | 2025-05-12 | 1.2     | New Features                                                                    | Architect AI |
