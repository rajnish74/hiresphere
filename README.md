# HireSphere

![CI](https://github.com/<your-username>/hiresphere/actions/workflows/ci.yml/badge.svg)

An AI-ready hiring platform built with Java 21 and Spring Boot 4. Candidates build profiles and apply
to jobs, recruiters publish jobs and run a hiring pipeline, and admins verify companies and moderate content.

**Live API docs:** <link to your deployed Swagger UI, or remove this line>

## Features

- **Auth & security:** JWT access tokens, rotating refresh tokens with reuse detection, email verification,
  password reset, login rate limiting, instant session revocation (token versioning)
- **Candidates:** profile (skills, education, experience, projects), resume upload with content validation,
  job search with filters, saved jobs, applications with a status timeline, job reports, privacy controls
- **Recruiters:** company verification workflow, job lifecycle (draft, published, closed), hiring pipeline
  with notes, resume snapshots, auto-close when all openings are filled
- **Admin:** user management and blocking, job moderation, report resolution, platform stats, immutable audit log
- **Platform:** async email notifications with retry and email log, notification preferences,
  event-driven modules, Flyway migrations, scheduled jobs

## Architecture

Modular monolith, package-by-feature: `auth`, `user`, `candidate`, `recruiter`, `job`, `application`,
`notification`, `admin`. Modules talk through services and domain events, never through each other's repositories.

## Tech stack

Java 21 (virtual threads), Spring Boot 4, Spring Security 7 (JWT), Spring Data JPA, PostgreSQL 17, Flyway,
Thymeleaf (email templates), springdoc-openapi, JUnit 5, Mockito, Docker, GitHub Actions.

## Run it locally

Prerequisites: Docker, JDK 21.

```bash
cp .env.example .env            # then set JWT_SECRET (openssl rand -base64 48)
docker compose --profile app up -d --build
```

- API: http://localhost:8082
- Fake mail inbox (Mailpit): http://localhost:8025
- API docs: set `SWAGGER_ENABLED=true` in `.env`, restart, open http://localhost:8082/swagger-ui.html

Development without Docker for the app: `docker compose up -d` (database and mail only), then run
`HiresphereApplication` from your IDE.

## Tests

```bash
./mvnw verify    # needs PostgreSQL from docker compose
```

## Key design decisions

- Refresh tokens are stored hashed and rotated on every use; reuse revokes the whole token family.
- Access tokens carry a version; blocking a user or changing a password invalidates them immediately.
- Every state change (job status, application status, company verification) goes through an entity method
  that enforces the allowed transitions.
- Public job visibility rules live in one place and are reused by search, direct links, facets and applying.
- Emails are sent after commit, asynchronously, with retry; failures never break the main request.

## Roadmap

AI resume analysis (Gemini), recruiter Kanban pipeline, interview scheduling, real-time notifications, Redis caching.