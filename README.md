# 📱 PexelsSample

<div align="center">
  
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK%2026+-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)

</div>

A native Android application built using Kotlin and Jetpack Compose, designed to provide users with a seamless experience for searching and browsing high-quality images via the Pexels API.

---

## 🏗️ Architecture and Technical Choices

### 🔄 MVVM Architecture with Clean Architecture Principles

<img src="https://developer.android.com/static/topic/libraries/architecture/images/final-architecture.png" width="600" alt="MVVM Architecture Diagram">

The application follows the Model-View-ViewModel (MVVM) pattern with Clean Architecture principles, organized into three main layers:

- **UI Layer (Presentation)**: Jetpack Compose UI components and ViewModels that manage UI state
- **Domain Layer**: Core business logic, repository interfaces, domain models, and use cases
- **Data Layer**: Repository implementations, API services, and local database cache

For detailed information on the architecture, see [Architecture Documentation](docs/architecture.md).

### 📂 Project Structure
The project follows a structured organization that reflects the Clean Architecture approach:

```
PexelsSample/
├── app/
│   ├── src/
│   │   ├── androidTest/  # Instrumented tests
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/nguyenmoclam/pexelssample/
│   │   │   │       ├── PexelsSampleApp.kt    # Application class (Hilt entry point)
│   │   │   │       ├── MainActivity.kt       # Single Activity hosting Compose UI
│   │   │   │       ├── core/                 # Navigation, utils
│   │   │   │       ├── data/                 # Repositories, API, DB
│   │   │   │       │   ├── local/            # Room database, DataStore
│   │   │   │       │   ├── remote/           # Pexels API
│   │   │   │       │   ├── repository/       # Repository implementations
│   │   │   │       │   └── mappers/          # Entity/DTO to domain model mappers
│   │   │   │       ├── di/                   # Dependency injection modules
│   │   │   │       ├── domain/               # Business logic
│   │   │   │       │   ├── model/            # Domain models
│   │   │   │       │   ├── repository/       # Repository interfaces
│   │   │   │       │   └── usecase/          # Business logic use cases
│   │   │   │       └── ui/                   # Composables, ViewModels
│   │   │   │           ├── common/           # Shared Composables
│   │   │   │           ├── detail/           # Image detail screen
│   │   │   │           ├── home/             # Home screen
│   │   │   │           └── theme/            # App theme
│   │   │   └── res/                          # Resources
│   │   └── test/                             # Unit tests
```

For more details, see [Project Structure Documentation](docs/project-structure.md).

### 🛠️ Tech Stack

<table>
  <tr>
    <th>Category</th>
    <th>Technologies</th>
  </tr>
  <tr>
    <td>📱 UI</td>
    <td>
      <a href="https://developer.android.com/jetpack/compose">Jetpack Compose</a> with Material 3<br>
      Responsive layouts using WindowSizeClass
    </td>
  </tr>
  <tr>
    <td>🧩 Architecture</td>
    <td>
      MVVM<br>
      <a href="https://developer.android.com/topic/libraries/architecture/viewmodel">ViewModel</a><br>
      <a href="https://developer.android.com/guide/navigation/navigation-getting-started">Navigation Compose</a><br>
      <a href="https://developer.android.com/training/data-storage/room">Room</a> for local caching<br>
      <a href="https://developer.android.com/topic/libraries/architecture/datastore">DataStore</a> for preferences
    </td>
  </tr>
  <tr>
    <td>💉 Dependency Injection</td>
    <td><a href="https://developer.android.com/training/dependency-injection/hilt-android">Hilt</a></td>
  </tr>
  <tr>
    <td>🌐 Networking</td>
    <td>
      <a href="https://square.github.io/retrofit/">Retrofit</a> with OkHttp<br>
      Kotlinx Serialization
    </td>
  </tr>
  <tr>
    <td>🖼️ Image Loading</td>
    <td><a href="https://coil-kt.github.io/coil/">Coil</a></td>
  </tr>
  <tr>
    <td>🧵 Concurrency</td>
    <td>Kotlin Coroutines and Flow</td>
  </tr>
  <tr>
    <td>🧪 Testing</td>
    <td>JUnit4, MockK, Espresso, Compose UI Test</td>
  </tr>
</table>

For comprehensive tech stack details, see [Tech Stack Documentation](docs/tech-stack.md).

### 🧰 Implementation Patterns

- **Repository Pattern**: Provides a clean API for data access to the rest of the application
- **Use Case Pattern**: Encapsulates business logic in single-responsibility classes
- **Immutable UI State**: ViewModels expose immutable UI state via StateFlow
- **Unidirectional Data Flow**: UI events flow to ViewModel, state updates flow back to UI
- **Error Handling**: Structured approach using sealed classes for typed errors
- **Dependency Injection**: Hilt for providing dependencies throughout the application
- **Adaptive Layouts**: Responsive UI that adapts to different screen sizes using WindowSizeClass

For more information, see [Coding Standards Documentation](docs/coding-standards.md).

### ✨ Key Features

<div align="center">
  <table>
    <tr>
      <td align="center"><b>🔍 Search</b></td>
      <td align="center"><b>🖼️ Browse</b></td>
      <td align="center"><b>🔎 Detail View</b></td>
      <td align="center"><b>📱 Responsive</b></td>
    </tr>
    <tr>
      <td>Search for high-quality photos via Pexels API</td>
      <td>Browse curated collections</td>
      <td>View photo details with pinch-to-zoom</td>
      <td>UI adapts to different screen sizes</td>
    </tr>
    <tr>
      <td align="center"><b>⚡ Performance</b></td>
      <td align="center"><b>🔄 Refresh</b></td>
      <td align="center"><b>🕒 History</b></td>
      <td align="center"><b>📶 Offline</b></td>
    </tr>
    <tr>
      <td>Local caching for improved speed</td>
      <td>Pull-to-refresh for updating content</td>
      <td>Recent search history</td>
      <td>Basic offline capabilities</td>
    </tr>
  </table>
</div>

---

## 📊 Data Models

The application uses clear domain models to represent core entities:

- **Photo**: Represents a photo from the Pexels API with properties like id, width, height, photographer name, etc.
- **PhotoSrc**: Contains URLs for different sizes/resolutions of a photo

See [Data Models Documentation](docs/data-models.md) for detailed information.

## 🌐 API Integration

The application integrates with the Pexels API to fetch high-quality photos:

- Search for photos by query term
- Browse curated collections
- View photo details

API details available in [API Reference Documentation](docs/api-reference.md).

## ⚖️ Trade-offs Due to Time Constraints

- **Limited Offline Support**: The application caches image metadata but has limited full offline experience.
- **Basic Error Handling**: Implementation focuses on core error cases without extensive recovery strategies.
- **Simplified Image Processing**: Complex image manipulations are limited in the initial implementation.
- **Core Testing Coverage**: Testing focuses on critical components rather than comprehensive coverage.

## 🧪 Testing Strategy

The project employs a comprehensive testing approach:

<div align="center">
  <img src="https://developer.android.com/static/images/training/testing/testing-pyramid.png" width="400" alt="Testing Pyramid">
</div>

- **Unit Tests**: For ViewModels, UseCases, Repositories, and utility classes
- **Integration Tests**: For data flow between components
- **UI Tests**: For Composable functions and screen interactions

See [Testing Strategy Documentation](docs/testing-strategy.md) for details.

---

## 🚀 Running the Application

### Prerequisites

- Android Studio (latest stable version recommended)
- JDK 17 or later
- An Android device or emulator (minimum SDK 26 - Android 8.0 Oreo)
- A valid Pexels API key

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/moclam1905/PexelsSample.git
   ```

2. Set up your Pexels API key:
   - Create or edit `gradle.properties` in the project root directory
   - Add the following line:
     ```properties
     PEXELS_API_KEY="your_actual_api_key"
     ```
   - For more details on environment configuration, see [Environment Variables Documentation](docs/environment-vars.md)

3. Open the project in Android Studio.

4. Sync the project with Gradle files.

### Build and Run

- To build the project:
  ```bash
  ./gradlew assembleDebug
  ```
- To install and run on a connected device or emulator:
  ```bash
  ./gradlew installDebug
  ```
- Or simply use the Run button in Android Studio.

---

## 📑 Implementation Epics

The project was implemented according to the following epics:

1. [📋 Epic 1: Project Setup and Configuration](docs/epic1.md)
2. [🌐 Epic 2: Core Networking and API Integration](docs/epic2.md)
3. [🔍 Epic 3: Photo Search and Display](docs/epic3.md)
4. [🔎 Epic 4: Photo Detail View](docs/epic4.md)
5. [✨ Epic 5: User Experience Enhancements](docs/epic5.md)
6. [⚡ Epic 6: Performance Optimizations](docs/epic6.md)
7. [🧪 Epic 7: Testing and Quality Assurance](docs/epic7.md)
8. [📱 Epic 8: Adaptive Layouts](docs/epic8.md)
9. [🕒 Epic 9: Search History](docs/epic9.md)

## 📚 Documentation

For more detailed information, refer to the following documentation:

<div align="center">
  
| Technical Documentation | Feature Documentation |
|------------------------|----------------------|
| [🏗️ Architecture Overview](docs/architecture.md) | [📋 Epic 1: Project Setup](docs/epic1.md) |
| [📂 Project Structure](docs/project-structure.md) | [🌐 Epic 2: API Integration](docs/epic2.md) |
| [🛠️ Tech Stack](docs/tech-stack.md) | [🔍 Epic 3: Photo Search](docs/epic3.md) |
| [📏 Coding Standards](docs/coding-standards.md) | [🔎 Epic 4: Photo Detail](docs/epic4.md) |
| [🌐 API Reference](docs/api-reference.md) | [✨ Epic 5: UX Enhancements](docs/epic5.md) |
| [📊 Data Models](docs/data-models.md) | [⚡ Epic 6: Performance](docs/epic6.md) |
| [🔐 Environment Variables](docs/environment-vars.md) | [🧪 Epic 7: Testing & QA](docs/epic7.md) |
| [🧪 Testing Strategy](docs/testing-strategy.md) | [📱 Epic 8: Adaptive Layouts](docs/epic8.md) |
|  | [🕒 Epic 9: Search History](docs/epic9.md) |

</div>

---

<div align="center">
  <p>Made with ❤️ by NguyenMocLam</p>
</div> 