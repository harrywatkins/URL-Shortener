## Implementation Notes

This solution is being developed incrementally with a focus on clean separation of concerns and testability.

- **Commit 1**  
  Initial Spring Boot backend bootstrap using Java 21 and Maven.


- **Commit 2**  
  Controller-first implementation based on the provided OpenAPI specification.
    - REST controller and DTOs added to match the API contract exactly.
    - A service interface is introduced and injected into the controller.
    - A temporary service stub is used to allow controller development in isolation.
    - Unit tests are provided for the controller to verify request handling and response mapping without relying on framework wiring or persistence.
