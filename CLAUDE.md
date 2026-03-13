# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An intermediate Spring Boot service that integrates ONLYOFFICE Document Editors into Atlassian cloud products (Jira, Confluence, Bitbucket) via Atlassian Forge. It sits between Forge applications, Atlassian APIs, and ONLYOFFICE Document Servers.

## Commands

```bash
# Run application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=CallbackControllerTest

# Run a specific test method
./mvnw test -Dtest=CallbackControllerTest#methodName

# Checkstyle only
./mvnw checkstyle:check

# Full build (compile + test + checkstyle)
./mvnw clean verify

# Package JAR
./mvnw clean package

# Start full stack (app + PostgreSQL + Redis)
docker compose up -d
```

## Architecture

**Stack:** Spring Boot 3, Spring Security (OAuth2/JWT), Spring Data JPA (PostgreSQL), Spring Data Redis, Spring WebFlux, Thymeleaf, ONLYOFFICE Docs Integration SDK.

**Package structure** (`com.onlyoffice.docs.atlassian.remote`):

- `web/controller/` — REST controllers (`EditorController`, `CallbackController`, `DownloadController`, `RemoteAuthorizationController`, `RemoteSettingsController`, etc.)
- `api/` — Context interfaces and implementations per product (`JiraContext`, `ConfluenceContext`, `BitbucketContext`), `Product` enum, `XForgeTokenType`
- `client/` — WebClient-based HTTP clients for Confluence API, Jira API, Bitbucket API, and ONLYOFFICE Document Server (`ds/`)
- `configuration/` — Spring config beans: `SecurityConfiguration`, `ForgeProperties`, `RedisConfiguration`, `ClientConfiguration`, `DocsIntegrationSdkConfiguration`
- `security/` — JWT filter (`RemoteAppAuthenticationFilter`), token service (`RemoteAppJwtService`), token repository (`XForgeTokenRepository`), `SecurityUtils`
- `service/` — Business logic (e.g., `DemoServerConnectionService`)
- `entity/` + `repository/` — JPA entity `DemoServerConnection` and its Spring Data repository
- `sdk/` — Wrappers/utilities around the ONLYOFFICE SDK (managers, services)
- `cache/` + `aop/` — Request-scoped caching via `RequestScopedCache` and `RequestCacheAspect`

**Flow:** Forge app → this service (JWT-secured REST) → Atlassian product API + ONLYOFFICE Document Server.

## Key Configuration

**`src/main/resources/application.yml`** — main config (base URL, secret key, Forge app IDs, datasource, Redis, Atlassian API URLs).

**Required environment variables:**
```
APP_BASE_URL              # Public URL of this service
APP_SECRET_KEY            # JWT signing key
FORGE_JIRA_APP_ID
FORGE_CONFLUENCE_APP_ID
FORGE_BITBUCKET_APP_ID
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
SPRING_REDIS_USER
SPRING_REDIS_PASSWORD
SPRING_REDIS_DATABASE
```

See `.env.example` for a full list.

## Tests

- Base class for controller tests: `AbstractControllerTest` — uses `@SpringBootTest`, `@Testcontainers`, `@AutoConfigureMockMvc` with real PostgreSQL 16 and Redis 7 containers.
- Test profile config: `src/test/resources/application-test.yml`.
- Mocking: Mockito with `@MockitoBean`.

## Code Style

Checkstyle enforces Sun conventions with 120-char line limit and requires license headers on all files. Configuration is in `checkstyle.xml` / `checkstyle-suppressions.xml` / `onlyoffice.header`. Checkstyle runs automatically during `verify`.
