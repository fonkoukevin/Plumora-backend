# Shared Decisions

- Backend repo is the source of truth for API and data model.
- Frontend must follow docs/api-contract.md.
- If API changes, update docs/api-contract.md in both repositories.
- Backend uses PostgreSQL.
- Frontend never calls AI provider directly.
- All AI calls go through Spring Boot backend.
- Publication is direct by author, no admin validation in MVP.
- Royalties are out of MVP.