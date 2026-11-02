# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Internal tool for EV charging sessions: start a session at a charge point, stop it with the charged energy, see the cost. Kotlin/Spring Boot backend, React/TypeScript frontend. `README.md` has the API table.

## Commands

Backend (run from `backend/`, serves http://localhost:8080):

```bash
./gradlew bootRun
./gradlew test
./gradlew test --tests 'com.dcs.charging.session.PricingTest'                            # one class
./gradlew test --tests 'com.dcs.charging.session.PricingTest.rounds half up to cents'    # one test
```

**Gradle must run on JDK 21.** On JDK 25 the Kotlin 2.1.21 plugin fails with a bare `What went wrong: 25` (`IllegalArgumentException` from `JavaVersion.parse`). The toolchain setting in `build.gradle.kts` doesn't prevent this, because the plugin parses the version of the JVM running Gradle. If the default `java` is newer, prefix the command with `JAVA_HOME=<path to a JDK 21>`.

Frontend (run from `frontend/`, serves http://localhost:5173 and proxies `/api` to the backend):

```bash
npm install
npm run dev
npm run build    # tsc && vite build
```

The frontend has no test runner, and neither side has a linter. `npm run build` is the only check for frontend changes.

## Architecture

The backend has two feature packages, `session` and `tariff`, plus `common` for error handling. `session` depends on `tariff`.

**Request flow:** controller (validates with `@Valid`, maps to DTO) → `ChargingSessionService` (transactions, clock, state changes) → repository. Controllers return DTOs, never entities.

**Things that span files:**

- **Pricing** is computed once, in `Pricing.cost`, when a session is stopped, and stored on the session. Prices are gross EUR `BigDecimal`s, and the cost is rounded half-up to cents.
- **Time** comes from the `Clock` bean defined in `ChargingApplication`. Services inject it rather than calling `Instant.now()`.
- **Database:** H2 in-memory with `ddl-auto: create-drop` and no migrations, so the schema comes from the entities and all data is lost on restart. `TariffSeeder` recreates the tariffs at startup, which also makes them available in `@SpringBootTest`.
- **Lazy loading:** `open-in-view` is off, and `SessionResponse.from` runs in the controller, outside the transaction. Any lazy association it reads will fail; `ChargingSession.tariff` works because `@ManyToOne` is eager by default.
- **Validation:** Bean Validation failures return 400 with `{ message, errors: { field: msg } }`, built in `ApiExceptionHandler`. A missing non-nullable Kotlin field fails earlier, during JSON parsing, and returns "Malformed request body" with no field errors. That is why `StopSessionRequest.energyKwh` is nullable with `@NotNull`, and why the service uses `requireNotNull` on it.
- **API contract:** the types in `frontend/src/api.ts` are hand-written copies of the backend DTOs, with no code generation, so change both together. `ApiError` there reads the `ErrorResponse` shape above.
- **Kotlin + JPA:** entities are regular (not data) classes, opened by the `allOpen` block in `build.gradle.kts`. IDs are nullable until persisted.

## Tests

`ChargingSessionApiTest` is a `@SpringBootTest` with MockMvc against the real H2 database. Tests don't roll back and share one database, so each test uses its own `customerId` (`c-1`, `c-2`, ...). Assertions on list results depend on this, so new tests need a new ID.
