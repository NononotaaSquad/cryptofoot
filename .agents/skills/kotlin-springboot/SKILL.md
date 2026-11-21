---
name: kotlin-springboot
description: 'Get best practices for developing applications with Spring Boot and Kotlin.'
---

# Spring Boot with Kotlin Best Practices

Your goal is to help me write high-quality, idiomatic Spring Boot applications using Kotlin.

## Project Setup & Structure

- **Build Tool:** Use Gradle (`build.gradle.kts`) with the Kotlin plugins (`org.jetbrains.kotlin.jvm`).
- **Starters:** Use Spring Boot starters (e.g., `spring-boot-starter-web`) as usual. Use `spring-boot-starter-data-mongodb` for persistence; no JPA or other database starters.
- **Package Structure:** Organize code by feature/domain (e.g., `com.example.app.order`, `com.example.app.user`) rather than by layer.

## Dependency Injection & Components

- **Primary Constructors:** Always use the primary constructor for required dependency injection. It's the most idiomatic and concise approach in Kotlin.
- **Immutability:** Declare dependencies as `private val` in the primary constructor. Prefer `val` over `var` everywhere to promote immutability.
- **Component Stereotypes:** Use `@Service`, `@Repository`, and `@RestController` annotations just as you would in Java.

## Configuration

- **Externalized Configuration:** Use `application.yml` for its readability and hierarchical structure.
- **Type-Safe Properties:** Use `@ConfigurationProperties` with `data class` to create immutable, type-safe configuration objects.
- **Profiles:** Use Spring Profiles (`application-dev.yml`, `application-prod.yml`) to manage environment-specific configurations.
- **Secrets Management:** Never hardcode secrets. Use environment variables or a dedicated secret management tool like HashiCorp Vault or AWS Secrets Manager.

## Web Layer (Controllers)

- **RESTful APIs:** Design clear and consistent RESTful endpoints.
- **Data Classes for DTOs:** Use Kotlin `data class` for all DTOs. This provides `equals()`, `hashCode()`, `toString()`, and `copy()` for free and promotes immutability.
- **Validation:** Use Java Bean Validation (JSR 380) with annotations (`@Valid`, `@NotNull`, `@Size`) on your DTO data classes.
- **Error Handling:** Implement a global exception handler using `@ControllerAdvice` and `@ExceptionHandler` for consistent error responses.

## Service Layer

- **Business Logic:** Encapsulate business logic within `@Service` classes.
- **Statelessness:** Services should be stateless.
- **Transaction Management:** Prefer single-document writes. MongoDB multi-document transactions require a replica set.

## Data Layer (Repositories)

- **MongoDB Repositories:** Define repositories as interfaces extending `MongoRepository`, with models as `@Document` data classes.
- **Null Safety:** Leverage Kotlin's null-safety (`?`) to clearly define which fields are optional or required at the type level.
- **Coroutines:** For reactive applications, leverage Spring Boot's support for Kotlin Coroutines in the data layer.

## Logging

- **Companion Object Logger:** The idiomatic way to declare a logger is in a companion object.
  ```kotlin
  companion object {
      private val logger = LoggerFactory.getLogger(MyClass::class.java)
  }
  ```
- **Parameterized Logging:** Use parameterized messages (`logger.info("Processing user {}...", userId)`) for performance and clarity.

## Testing

- **Spock:** Tests are Spock specifications written in Groovy (`*Spec.groovy`). Unit specs live in `src/test/groovy` and extend `spock.lang.Specification` without a Spring context; integration specs live in `src/integrationTest/groovy` (`./gradlew integrationTest`). Use `given`/`when`/`then`/`expect` blocks, `where:` tables for data-driven cases, and Spock's built-in `Mock()`/`Stub()` for mocking.
- **Spring Integration:** `spock-spring` lets Spring test annotations (`@SpringBootTest`, `@AutoConfigureRestTestClient`) work on specs; inject beans with `@Autowired` fields. Integration specs extend `squad.nononotaa.backend.BaseSpec` (only available in `src/integrationTest`), which starts the app on a random port against a MongoDB Testcontainer (shared, started once per JVM) and exposes a `RestTestClient client` for real HTTP calls (`client.get().uri('/api/1/...').exchange().expectStatus()...`).
- **Test Slices:** Use test slice annotations like `@WebMvcTest` to test specific parts of the application.

## Coroutines & Asynchronous Programming

- **`suspend` functions:** For non-blocking asynchronous code, use `suspend` functions in your controllers and services. Spring Boot has excellent support for coroutines.
- **Structured Concurrency:** Use `coroutineScope` or `supervisorScope` to manage the lifecycle of coroutines.
