
# PexelsSample Project Structure

This document outlines the recommended directory and package structure for the PexelsSample Android application. The structure is designed to promote separation of concerns, modularity, and ease of navigation.

## Project Root Directory Structure

```plaintext
PexelsSample/
├── app/                          # Main application module
│   ├── build/                    # Build output (git-ignored)
│   ├── libs/                     # Local libraries (if any, usually empty with Gradle)
│   ├── src/
│   │   ├──androidTest/            # Instrumented tests (UI tests, integration tests on device)
│   │   │  └── java/
│   │   │      └── com/nguyenmoclam/pexelssample/
│   │   │          └── (test classes mirroring main structure)
│   │   ├──main/                   # Main application source set
│   │   │  ├── java/
│   │   │  │   └── com/nguyenmoclam/pexelssample/
│   │   │  │       ├── PexelsSampleApp.kt    # Application class (Hilt entry point)
│   │   │  │       ├── MainActivity.kt       # Single Activity hosting Compose UI
│   │   │  │       │
│   │   │  │       ├── core/                 # Core components (not fitting elsewhere or widely shared)
│   │   │  │       │   ├── navigation/       # Jetpack Navigation Compose setup (NavGraph, Routes)
│   │   │  │       │   └── utils/            # General utility classes/functions (e.g., NetworkConnectivityHandler)
│   │   │  │       │
│   │   │  │       ├── data/                 # Data layer: Repositories, API services, Database
│   │   │  │       │   ├── local/            # Local data source (Room)
│   │   │  │       │   │   ├── PexelsRoomDatabase.kt
│   │   │  │       │   │   ├── dao/
│   │   │  │       │   │   │   └── PhotoDao.kt
│   │   │  │       │   │   ├── datastore/      # New package for DataStore
│   │   │  │       │   │   │   └── SearchHistoryDataStoreManager.kt (or similar)
│   │   │  │       │   │   └── model/        # Room entities (if distinct from domain models)
│   │   │  │       │   │       └── CachedImageEntity.kt (example)
│   │   │  │       │   ├── remote/           # Remote data source (Pexels API)
│   │   │  │       │   │   ├── PexelsApiService.kt
│   │   │  │       │   │   └── model/        # API response DTOs (Data Transfer Objects)
│   │   │  │       │   │       ├── PexelsPhotoDto.kt
│   │   │  │       │   │       ├── PexelsPhotoSrcDto.kt
│   │   │  │       │   │       └── PexelsSearchResponseDto.kt
│   │   │  │       │   ├── repository/       # Repository implementations
│   │   │  │       │   │   └── ImageRepositoryImpl.kt
│   │   │  │       │   │   └── SearchHistoryRepositoryImpl.kt # New repository
│   │   │  │       │   └──mappers/          # Mappers to convert DTOs/Entities to Domain Models
│   │   │  │       │       └── PhotoMappers.kt
│   │   │  │       │
│   │   │  │       ├── di/                   # Dependency Injection modules (Hilt)
│   │   │  │       │   ├── AppModule.kt
│   │   │  │       │   ├── NetworkModule.kt
│   │   │  │       │   └── DatabaseModule.kt
│   │   │  │       │   ├── DataStoreModule.kt  # New module for DataStore
│   │   │  │       │   └── RepositoryModule.kt # Update to include SearchHistoryRepository
│   │   │  │       │
│   │   │  │       ├── domain/               # Domain layer: Core business logic, Use Cases, Domain Models
│   │   │  │       │   ├── model/            # Domain models (e.g., Photo, PhotoSrc - clean representation)
│   │   │  │       │   │   ├── Photo.kt
│   │   │  │       │   │   └── PhotoSrc.kt
│   │   │  │       │   ├── repository/       # Repository interfaces (defined in domain)
│   │   │  │       │   │   └── ImageRepository.kt
│   │   │  │       │   │   └── SearchHistoryRepository.kt # New interface
│   │   │  │       │   └── usecase/          # Use cases (optional, for more complex logic)
│   │   │  │       │       ├── SearchPhotosUseCase.kt
│   │   │  │       │       └── GetCuratedPhotosUseCase.kt
│   │   │  │       │       ├── GetRecentSearchesUseCase.kt # New
│   │   │  │       │       ├── SaveSearchTermUseCase.kt    # New
│   │   │  │       │       └── ClearSearchHistoryUseCase.kt # New
│   │   │  │       │
│   │   │  │       └── ui/                   # Presentation layer: Composables, ViewModels, UI-related state
│   │   │  │           ├── common/           # Reusable Composables shared across features
│   │   │  │           │   ├── ErrorView.kt
│   │   │  │           │   ├── LoadingIndicator.kt
│   │   │  │           │   └── ImageItem.kt
│   │   │  │           ├── detail/           # Image Detail feature
│   │   │  │           │   ├── ImageDetailScreen.kt
│   │   │  │           │   └── ImageDetailViewModel.kt (if needed)
│   │   │  │           ├── home/             # Home/Search feature (can be one or split)
│   │   │  │           │   ├── HomeScreen.kt
│   │   │  │           │   └── SearchViewModel.kt
│   │   │  │           ├── results/          # Search Results feature (if distinct from home)
│   │   │  │           │   ├── SearchResultsScreen.kt
│   │   │  │           │   └── (SearchViewModel might be shared or a dedicated one)
│   │   │  │           └── theme/            # Jetpack Compose Theme (Colors, Typography, Shapes)
│   │   │  │               ├── Color.kt
│   │   │  │               ├── Theme.kt
│   │   │  │               └── Typography.kt
│   │   │  │
│   │   │  └── AndroidManifest.xml
│   │   │  └── res/                   # Android resources (drawables, mipmap, values, etc.)
│   │   │      ├── drawable/
│   │   │      ├── mipmap-xxxhdpi/ (etc. for app icons)
│   │   │      └── values/
│   │   │          ├── colors.xml
│   │   │          ├── strings.xml
│   │   │          └── themes.xml
│   │   │
│   │   └──test/                   # Unit tests (local JVM tests)
│   │      └── java/
│   │          └── com/nguyenmoclam/pexelssample/
│   │              └── (test classes mirroring main structure, e.g., data/repository, domain/usecase, ui/viewmodel)
│   │
│   ├── .gitignore
│   └── build.gradle.kts            # App module Gradle build script
│
├── gradle/                       # Gradle wrapper files
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .gitignore                    # Root .gitignore
├── build.gradle.kts              # Root project Gradle build script
├── gradle.properties             # Project-wide Gradle settings (API key placeholder here)
├── libs.versions.toml            # Version catalog for dependencies
└── settings.gradle.kts           # Gradle settings (included modules)
```

## Key Package Descriptions (within `app/src/main/java/com/nguyenmoclam/pexelssample/`)

* **`PexelsSampleApp.kt`**: The `Application` class, annotated with `@HiltAndroidApp` to initialize Hilt.
* **`MainActivity.kt`**: The single entry point `Activity` for the application, responsible for setting up the Jetpack Compose content.
* **`core/`**: Contains fundamental code that doesn't fit neatly into other layers or is broadly shared without being specific to a feature.
    * `core/navigation/`: Defines NavHost, NavController setup, screen routes, and navigation argument passing logic.
    * `core/utils/`: General utility classes, e.g., `NetworkConnectivityHandler`, helper functions.
* **`data/`**: Implements the data layer, handling data retrieval and storage.
    * `data/local/`: Room database setup (`PexelsRoomDatabase`), DAOs (`PhotoDao`), and local entity models if they differ from domain models.
    * `data/remote/`: Retrofit service interface (`PexelsApiService`) and API-specific DTOs.
    * `data/repository/`: Concrete implementations of repository interfaces defined in the `domain` layer (e.g., `ImageRepositoryImpl`).
    * `data/mappers/`: Contains extension functions or classes to map DTOs (from `remote`) and Entities (from `local`) to domain models (in `domain/model`), and vice-versa if needed. This keeps data layer models separate from domain models.
* **`di/`**: Hilt dependency injection modules (e.g., `AppModule`, `NetworkModule`, `DatabaseModule`) for providing instances of Repositories, API services, Database, etc.
* **`domain/`**: The core business logic of the application, independent of Android framework or specific data sources.
    * `domain/model/`: Defines the clean data models representing the core entities of the application (e.g., `Photo`, `PhotoSrc`). These are plain Kotlin data classes.
    * `domain/repository/`: Defines interfaces for repositories (e.g., `ImageRepository`), specifying the contract for data operations that ViewModels/UseCases will use.
    * `domain/usecase/`: (Optional but recommended for encapsulating complex business logic) Contains individual classes, each representing a single business operation or user story (e.g., `SearchPhotosUseCase`, `GetPhotoDetailsUseCase`). Use cases orchestrate calls to repositories.
* **`ui/`**: Contains all UI-related components (Jetpack Compose) and ViewModels.
    * `ui/common/`: Reusable Composable functions that can be used across multiple feature screens (e.g., `ImageItem`, `ErrorView`, `LoadingIndicator`).
        * `ui/common/adaptive/`: Reusable Composables specifically for adaptive layout structures (e.g., MasterDetailLayout).
    * `ui/theme/`: Jetpack Compose theme definitions (colors, typography, shapes).
    * `ui/{feature_name}/`: Sub-packages for each distinct feature or screen (e.g., `ui/home/`, `ui/detail/`, `ui/results/`). Each feature package typically contains:
        * `{FeatureName}Screen.kt`: The main Composable function for the screen.
        * `{FeatureName}ViewModel.kt`: The ViewModel associated with the screen.
        * Other feature-specific Composables.

## Notes

* This structure emphasizes a separation between data, domain, and UI layers, which is crucial for MVVM and Clean Architecture principles.
* The use of a `domain` layer is highly recommended to keep business logic pure and independent of platform specifics.
* Mappers in the `data` layer are important for converting between network/database models and the clean domain models, preventing the domain layer from being polluted by data source specifics.
* Package names should be lowercase. Class names should use PascalCase. File names for Kotlin classes generally match the class name.
* Test packages (`androidTest` and `test`) should mirror the structure of the `main` source set for clarity.

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft based on MVVM and Clean Arch principles. | Architect AI |
| Bonus Features | 2025-05-10 | 1.1     | Bonus Features | Architect AI |

