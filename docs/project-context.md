
---

# 4. Fichier `docs/project-context.md`

Tu peux créer ce fichier dans **les deux repos**. Il résume le produit.

```md
# Plumora — Project Context

Plumora is a platform for writing, beta-reading, publishing, reading and sharing digital books.

The project is developed for an RNCP level 7 certification.

## Main objective

Build a professional MVP showing:
- software architecture
- backend API design
- frontend mobile/desktop UX
- database modeling
- AI integration
- Docker deployment
- maintainable code

## Core users

### Author
Can:
- create books
- create chapters
- edit manuscripts
- use Mukeme Writing Assistant
- send a book to beta-reading
- receive beta comments
- publish directly

### Reader
Can:
- discover published books
- use Mukeme Recommendation
- read books
- add favorites
- leave reviews
- report books

### Beta-reader
Can:
- receive beta-reading invitations
- read private shared chapters
- add structured comments before publication

### Admin
MVP minimal role:
- manage users if needed
- manage reports
- archive problematic books

## Publication model

Publication is simple in the MVP.

There is no admin validation.

A book is published when:
- status = PUBLISHED
- visibility = PUBLIC
- published_at is not null

## AI features

Only two AI features:

1. Mukeme Writing Assistant
- reformulate selected text
- improve style
- fix repetitions
- make text more emotional
- improve dialogue

2. Mukeme Reading Recommendation
- recommend published books based on user intent
- input: free text, mood, duration, genre
- output: books, match score, reasons

## Out of scope for MVP

Do not implement:
- real payment
- royalties
- subscription
- marketplace
- publication validation workflow
- chat