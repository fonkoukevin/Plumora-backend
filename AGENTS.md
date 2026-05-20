# AGENTS.md — Plumora Backend

## Project overview

Plumora is a digital book platform for writing, beta-reading, publishing, reading and sharing books.

The backend is a Java Spring Boot REST API used by:
- Flutter mobile app
- Flutter desktop app
- potential future web app

The MVP includes:
- user registration and authentication
- user roles: AUTHOR, READER, BETA_READER, ADMIN
- book and chapter management
- simple publication directly by the author
- reading catalog
- reading progress
- favorites
- reviews
- beta-reading campaigns, invitations and structured comments
- Mukeme AI writing assistant
- Mukeme AI book recommendation
- notifications

The MVP does NOT include:
- payment
- real royalties
- admin validation before publication
- subscription system
- marketplace
- real-time chat

Publication is simple:
A book is published when:
- status = PUBLISHED
- visibility = PUBLIC
- published_at is not null

There is no publication_request, publication_checklist or published_book table in the MVP.

## Technical stack

Use:
- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Security with JWT
- Flyway for database migrations
- Docker and Docker Compose
- Swagger/OpenAPI with springdoc
- Bean Validation
- Lombok if already configured

## Backend architecture

Use a layered architecture inspired by Clean Architecture and DDD.

Organize code by business domain, not by technical layer globally.

Root package:
`com.plumora.api`

Main domains:
- user
- book
- betaReading
- reading
- ai
- notification
- shared

Each domain should follow this structure when relevant:

```text
domain/
application/
infrastructure/
presentation/

Layer responsibilities:

presentation:
REST controllers, request DTOs, response DTOs, API mappers.
No business logic here.
application:
services and use cases.
Orchestrates business flows.
Handles transactions.
domain:
entities, enums, domain rules, business exceptions.
Keep core business rules here when possible.
infrastructure:
Spring Data JPA repositories, persistence details, external providers, technical adapters.

The dependency direction should remain clean:
presentation -> application -> domain
infrastructure supports persistence and technical integrations.

Database rules

Use PostgreSQL.

Use UUID primary keys.

Use Flyway migrations. Do not rely on Hibernate ddl-auto update.
spring.jpa.hibernate.ddl-auto must stay validate.

Tables in MVP:

users
roles
user_roles
books
chapters
chapter_versions
ai_writing_requests
ai_writing_suggestions
beta_reading_campaigns
beta_invitations
beta_shared_chapters
beta_comments
reading_progress
favorites
reviews
reports
ai_recommendation_requests
ai_recommendation_results
notifications

Do not create these tables for MVP:

publication_requests
publication_checklists
published_books
royalty_accounts
royalty_transactions
royalty_rules
Main business rules

Users:

A user can have several roles.
Roles are AUTHOR, READER, BETA_READER, ADMIN.
Email and username must be unique.

Books:

A book belongs to one author.
A book can have zero or more chapters.
A chapter belongs to one book.
A book starts as DRAFT and PRIVATE.
Only the author can modify the book.
A book can be published directly by its author.
When published, set:
status = PUBLISHED
visibility = PUBLIC
published_at = now()
Only PUBLISHED and PUBLIC books appear in the public catalog.
Archived books cannot be edited.

Chapters:

A chapter belongs to one book.
chapter_order must be unique within a book.
When a chapter is updated, create a chapter version if relevant.

Beta-reading:

An author can create a beta-reading campaign for their own book.
A campaign can share selected chapters only.
A user must be invited and must accept the invitation to read/comment in a beta campaign.
Beta comments are private and are used before publication.
Beta comments are structured by type, priority and status.

Reading:

A user can have only one reading progress per book.
A user can favorite a book only once.
A user can review a book only once.
Only published books can receive public reading progress, favorites and reviews.

AI:

Mukeme Writing Assistant helps authors reformulate or improve selected text.
AI suggestions must never modify the manuscript automatically.
The author must accept, modify or ignore a suggestion.
Mukeme Recommendation recommends published books based on reader intent.
Recommendation results must include match_score and reasons.
AI provider calls must be done from the backend, never directly from Flutter.

Notifications:

Use notifications for beta invitations, beta comments, book published, AI recommendation and system events.
API conventions

Base path:
/api/v1

Use REST conventions:

GET for retrieval
POST for creation/action with body
PUT for full update
PATCH for partial state change
DELETE for deletion/archive/removal

Use DTOs for all API inputs and outputs.
Do not expose JPA entities directly.

Use meaningful endpoint names:

/api/v1/auth/login
/api/v1/books
/api/v1/books/{bookId}/chapters
/api/v1/books/{bookId}/publish
/api/v1/catalog/books
/api/v1/ai/writing/suggestions
/api/v1/ai/recommendations/books
Error handling

Use a global exception handler.

Return consistent error responses:

{
  "timestamp": "2026-05-20T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Explanation",
  "path": "/api/v1/books"
}

Create business exceptions when needed:

ResourceNotFoundException
UnauthorizedActionException
BusinessException
DuplicateResourceException
Security

Use Spring Security with JWT.

Rules:

Public routes: register, login, public catalog.
Authenticated routes: profile, reading, favorites, reviews.
AUTHOR routes: book creation, chapter management, beta campaigns, AI writing.
BETA_READER routes: beta invitations and beta comments.
ADMIN routes: admin routes and report management.

Always check resource ownership in services.
Do not rely only on URL-level security.

Docker

The backend API must be dockerized.

Use:

Dockerfile for Spring Boot API
docker-compose.yml for API + PostgreSQL
environment variables for DB and JWT config

In Docker Compose, the PostgreSQL hostname is the service name, usually postgres.

Use:
jdbc:postgresql://postgres:5432/plumora_db

Development rules

When implementing a feature:

Respect the layered architecture.
Add or update Flyway migration if database schema changes.
Add DTOs instead of exposing entities.
Add validation annotations on request DTOs.
Add service-level business checks.
Add tests when possible.
Keep code readable and consistent.

Do not implement non-MVP features unless explicitly requested.
Do not add payment, royalties, subscription or publication validation workflow.


API connection

Use backend base URL from environment/config.

Local examples:

Android emulator: http://10.0.2.2:8080/api/v1
Desktop local: http://localhost:8080/api/v1
Physical device: http://<LAN_IP>:8080/api/v1

Production example:
https://api.plumora.fr/api/v1

Never hardcode a production URL directly in widgets.
Use a centralized API configuration.

API conventions

Expected backend routes include:

Auth:

POST /auth/register
POST /auth/login
GET /auth/me

Books:

POST /books
GET /books/my-books
GET /books/{bookId}
PUT /books/{bookId}
PATCH /books/{bookId}/publish

Chapters:

POST /books/{bookId}/chapters
GET /books/{bookId}/chapters
GET /chapters/{chapterId}
PUT /chapters/{chapterId}

Catalog:

GET /catalog/books
GET /catalog/books/{bookId}
GET /catalog/books/search

AI:

POST /ai/writing/suggestions
PATCH /ai/writing/suggestions/{suggestionId}/accept
POST /ai/recommendations/books

Reading:

GET /books/{bookId}/read
POST /books/{bookId}/reading-progress
PUT /books/{bookId}/reading-progress

Favorites:

POST /books/{bookId}/favorites
DELETE /books/{bookId}/favorites
GET /favorites/my

Reviews:

POST /books/{bookId}/reviews
GET /books/{bookId}/reviews

Beta-reading:

GET /beta-invitations/my-invitations
PATCH /beta-invitations/{invitationId}/accept
POST /beta-comments

Notifications:

GET /notifications/my
PATCH /notifications/{notificationId}/read
Development rules

When implementing a screen:

Keep widgets small and readable.
Keep API calls out of widgets.
Put API logic in data layer.
Use models/DTOs.
Handle loading, error and empty states.
Use the Plumora design system.
Keep mobile and desktop layouts responsive.
Do not implement features outside MVP unless requested.