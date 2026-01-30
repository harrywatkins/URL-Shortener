## Implementation Notes

This solution is being developed incrementally with a focus on clean separation of concerns and testability.

- **Commit 1**  
  Initial Spring Boot backend bootstrap using Java 21 and Maven.


- **Commit 2**
  - Controller-first implementation based on the provided OpenAPI specification.
      - REST controller and DTOs added to match the API contract exactly.
      - A service interface is introduced and injected into the controller.
      - A temporary service stub is used to allow controller development in isolation.
      - Unit tests are provided for the controller to verify request handling and response mapping without relying on framework wiring or persistence.
  
    
- **Commit 3**
  - Service layer implemented with full unit test coverage.
      - Introduces the core URL-shortening logic behind a service interface.
      - Validates input URLs and optional custom aliases.
      - Supports user-defined aliases and randomly generated aliases.
      - Ensures alias uniqueness using atomic operations.
      - Provides deterministic error handling for invalid input and missing aliases.
      - Implementation is intentionally storage-agnostic to allow a future switch to persistent storage without changing the controller.
  

- **Commit 4**
  - Added persistence, error handling, and verified the backend end-to-end.
      - Replaced the service’s in-memory storage with a JPA repository backed by an H2 **file-based** database to ensure shortened URLs persist across application restarts.
      - Enforced alias uniqueness at the database level and handled collisions gracefully in the service layer.
      - Introduced global exception handling using `@RestControllerAdvice` to map domain exceptions to appropriate HTTP responses (`400` and `404`), matching the OpenAPI contract.
      - Updated service unit tests to mock the repository and validate behaviour without requiring a database.
      - Performed manual end-to-end API verification using `curl`, covering:
          - URL creation with random and custom aliases
          - Redirect behaviour (`302`)
          - Listing and deletion of URLs
          - Error cases (duplicate alias, missing alias)
          - Persistence across application restarts


- **Commit 5**
  - Containerised the backend using a multi-stage Docker build.
      - Runs Spring Boot in a lightweight JRE image.
      - Persists H2 file database via a Docker volume to retain URLs across container restarts.
      - Added Docker ignore rules and documented build/run commands.
      - Untested right now

- **Commit 6**
  - Added a decoupled React frontend (Vite).
    - Supports creating short URLs with optional custom aliases 
    - Displays API errors 
    - Uses Vite proxy (/api) for backend communication in development

