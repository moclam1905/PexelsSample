

# PexelsSample API Reference

This document details the external APIs consumed by the PexelsSample application. Currently, the sole external API is the Pexels API.

## External APIs Consumed

### Pexels API

-   **Purpose:** To search for and retrieve high-quality photos for display within the application. All image content is sourced from Pexels. [30]
-   **Base URL:** `https://api.pexels.com/v1/`
-   **Authentication:** API Key sent in the `Authorization` HTTP header.
    -   Header Name: `Authorization`
    -   Value: `{YOUR_PEXELS_API_KEY}` (The actual key will be injected via `BuildConfig` from `gradle.properties`, as defined in `docs/coding-standards.md` and `docs/environment-vars.md`). [27, 37, 56]
-   **Link to Official Docs:** [https://www.pexels.com/api/documentation/](https://www.pexels.com/api/documentation/)
-   **Rate Limits:**
    -   The Pexels API has a rate limit of 200 requests per hour and 20,000 requests per month.
    -   The application should be designed to respect these limits through efficient API usage, caching, and by avoiding unnecessary calls. [37, 68]
    -   The API returns `429 Too Many Requests` HTTP status code if rate limits are exceeded. This should be handled gracefully. (Story 5.3)

#### Key Endpoints Used:

1.  **Search for Photos**
    -   **Endpoint:** `GET /search`
    -   **Description:** Searches for photos based on a query string. (Goal 1, Epic 2) [6]
    -   **Request Parameters (Query):**
        -   `query` (String, required): The search query (e.g., "Nature", "People").
        -   `page` (Int, optional, default: `1`): The page number of the results to retrieve.
        -   `per_page` (Int, optional, default: `15`, max: `80`): The number of results per page. We will likely use a value around `20-30` for efficient pagination.
        -   `orientation` (String, optional): Desired photo orientation. `landscape`, `portrait`, or `square`.
        -   `size` (String, optional): Minimum photo size. `large` (24MP), `medium` (12MP), or `small` (4MP).
        -   `locale` (String, optional): The locale of the search query.
    -   **Example Request:**
        `GET https://api.pexels.com/v1/search?query=Nature&per_page=20&page=1`
        Header: `Authorization: YOUR_PEXELS_API_KEY`
    -   **Success Response (`200 OK`):** JSON object containing a list of photo objects and pagination details.
        -   Schema: See `PexelsSearchResponseDto` and related DTOs defined in `docs/data-models.md` (to be created).
        -   Key fields in response: `photos` (array of photo objects), `page`, `per_page`, `total_results`, `next_page` (URL for the next page of results).
    -   **Error Responses:**
        -   `400 Bad Request`: Invalid request parameters.
        -   `401 Unauthorized`: Invalid API key. (Should be handled by secure key management, but good to be aware of).
        -   `429 Too Many Requests`: Rate limit exceeded.
        -   `500 Internal Server Error`: Pexels API server issue.

2.  **Get Curated Photos**
    -   **Endpoint:** `GET /curated`
    -   **Description:** Retrieves a list of curated, high-quality photos from Pexels. This can be used for an initial view or a "discover" feature if no search query is active. [36]
    -   **Request Parameters (Query):**
        -   `page` (Int, optional, default: `1`): The page number of the results to retrieve.
        -   `per_page` (Int, optional, default: `15`, max: `80`): The number of results per page.
    -   **Example Request:**
        `GET https://api.pexels.com/v1/curated?per_page=20&page=1`
        Header: `Authorization: YOUR_PEXELS_API_KEY`
    -   **Success Response (`200 OK`):** JSON object, same structure as the search response (`PexelsSearchResponseDto`).
        -   Schema: See `PexelsSearchResponseDto` in `docs/data-models.md`.
    -   **Error Responses:** Similar to the `/search` endpoint.

*(Note: The Pexels API offers other endpoints like getting a specific photo by ID, featured collections, etc. These are not required for the MVP but could be considered for future enhancements.)*

## Internal APIs Provided

Not applicable for this project, as PexelsSample is a client-side application and does not expose its own APIs for external consumption.

## Cloud Service SDK Usage

Not applicable for this project, as it's a client-side application consuming a third-party API and does not directly interact with cloud provider SDKs (like AWS SDK, Azure SDK, etc.) for its core functionality. Local Android SDKs are covered in the Tech Stack.

## Change Log

| Change        | Date       | Version | Description                                     | Author     |
| :------------ | :--------- | :------ | :---------------------------------------------- | :--------- |
| Initial draft | 2025-05-08 | 0.1     | Initial draft detailing Pexels API usage.       | Architect AI |

