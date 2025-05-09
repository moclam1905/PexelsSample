
# PexelsSample Environment Variables

This document outlines how build-time configurations, particularly secrets like the Pexels API key, are managed for the PexelsSample application.

## Configuration Loading Mechanism

The primary mechanism for injecting sensitive information like the Pexels API key at build time involves using Gradle properties.

  - **Local Development:**
      - A `gradle.properties` file located in the root project directory (or the user's global `.gradle` directory) can be used to store the API key. This file **must be added to `.gitignore`** to prevent the actual key from being committed to version control.
      - The `app/build.gradle.kts` file will read this property and make it available to the application code via the `BuildConfig` class.
  - **CI/CD (Continuous Integration/Continuous Deployment):**
      - For automated builds (e.g., on GitHub Actions, GitLab CI, Jenkins), the Pexels API key should be configured as a secure environment variable or secret within the CI/CD system.
      - The Gradle build script will be configured to read this environment variable during CI/CD builds if the `gradle.properties` entry is not found or if explicitly prioritized.

## Required Variables

| Variable Name      | Description                      | Example / Placeholder Value in `.env.nguyenmoclam` or Build Script | Required? (Yes/No) | Sensitive? (Yes/No) | How Provided to Build                                                                                                |
| :----------------- | :------------------------------- | :---------------------------------------------------------- | :----------------- | :------------------ | :------------------------------------------------------------------------------------------------------------------- |
| `PEXELS_API_KEY`   | API Key for the Pexels API.      | `"YOUR_PEXELS_API_KEY_PLACEHOLDER"`                         | Yes                | Yes                 | Local: `gradle.properties` (root project). \<br/\> CI/CD: Secure environment variable (e.g., `PEXELS_API_KEY`). [25, 56, 71] |
| `APP_VERSION_NAME` | Application's user-visible version | `1.0.0`                                                     | Yes                | No                  | Defined in `app/build.gradle.kts`.                                                                                   |
| `APP_VERSION_CODE` | Application's internal version code| `1`                                                         | Yes                | No                  | Defined in `app/build.gradle.kts`.                                                                                   |

## `BuildConfig` Field Generation

The Pexels API key will be exposed to the application code via a field in the generated `BuildConfig` class. This is configured in `app/build.gradle.kts`:

```kotlin
// In app/build.gradle.kts

android {
    // ... other configurations ...

    defaultConfig {
        // ... other defaultConfig ...

        // Read PEXELS_API_KEY from environment variable first (for CI/CD),
        // then from project's gradle.properties (for local development).
        val pexelsApiKey = System.getenv("PEXELS_API_KEY")
            ?: project.findProperty("PEXELS_API_KEY") as? String
            ?: "" // Fallback to empty string if not found (build will likely fail or app won't work)

        // Expose PEXELS_API_KEY as a BuildConfig field
        // The actual key will be available in code as: BuildConfig.PEXELS_API_KEY
        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsApiKey\"")
    }
    // ...
}
```

## `.gitignore` consideration for `gradle.properties`

Ensure that if `gradle.properties` in the project root is used to store the actual API key for local development, it is listed in the project's root `.gitignore` file:

```
# In /.gitignore
# ... other rules ...

# Local Gradle properties file (if it contains secrets like API keys)
gradle.properties
```

It's also common to have a `gradle.properties.nguyenmoclam` file committed to the repository with placeholder values to guide developers.

## Notes

  - **Secrets Management:** The Pexels API key is sensitive and is the primary secret this application handles. The method described (local `gradle.properties` + `BuildConfig` + CI/CD environment variables) is a standard approach for Android development.
  - **No `.env` files:** Unlike some backend or web frontend projects, Android projects don't typically use `.env` files at runtime in the same way. Build-time injection via `BuildConfig` is preferred for such configurations.
  - **Validation:** The application should ideally check at startup (e.g., in the `Repository` or `ViewModel` when making the first API call) if the `BuildConfig.PEXELS_API_KEY` is empty or a placeholder, and if so, fail gracefully or inform the user/developer, rather than letting API calls fail cryptically.

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft outlining API key management via Gradle properties and BuildConfig. | Architect AI |

