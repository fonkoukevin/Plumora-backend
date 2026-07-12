# Plumora API Contract

Base URL:
`/api/v1`

## Auth

POST `/auth/register`
POST `/auth/login`
GET `/auth/me`

## Books

POST `/books`
GET `/books/my-books`
GET `/books/{bookId}`
PUT `/books/{bookId}`
PATCH `/books/{bookId}/publish`
PATCH `/books/{bookId}/archive`

Create/update book requests accept an optional `coverUrl` field:

```json
{
  "title": "Les Brumes de Cendrelune",
  "subtitle": "Livre I",
  "summary": "Une apprentie cartographe suit des lanternes impossibles.",
  "coverUrl": "https://placehold.co/600x900/263238/ffffff?text=Cendrelune",
  "genre": "Fantasy",
  "languageCode": "fr"
}
```

Book responses expose the same image as `coverUrl`. Book-related list responses expose it as `coverUrl` or `bookCoverUrl` when the response is centered on another resource, for example favorites, reviews, reading progress, beta invitations, reports and AI recommendation results.

Book creation and update also accept `multipart/form-data` on the same routes:

- text fields: `title`, `subtitle`, `summary`, `genre`, `languageCode`
- image file field: `coverImage` (aliases accepted: `cover_image`, `image`, `imageFile`, `cover`, `file`)

The stored image is returned as a relative `coverUrl`, for example:

```json
{
  "coverUrl": "uploads/book-covers/2f8f3d0d-9d4b-4508-aeb5-981a3af4a7d6.jpg"
}
```

Clients should resolve this relative URL from the API base URL:
`http://localhost:8080/api/v1/uploads/book-covers/...`.

## Chapters

POST `/books/{bookId}/chapters`
GET `/books/{bookId}/chapters`
GET `/chapters/{chapterId}`
PUT `/chapters/{chapterId}`
DELETE `/chapters/{chapterId}`

## Catalog

GET `/catalog/books`
GET `/catalog/books/{bookId}`
GET `/catalog/books/search`
GET `/catalog/books/popular`
GET `/catalog/books/latest`
GET `/catalog/genres`

Only books with status PUBLISHED and visibility PUBLIC are returned in the catalog.

Imported external books can expose additional optional fields in book, catalog and read responses:

- `externalSource`, for example `GUTENDEX`
- `externalId`
- `externalAuthors`
- `sourceUrl`
- `readUrl`
- `downloadCount`
- `formats` on catalog detail responses

## External public-domain books

GET `/external-books`

Optional query parameters:

- `search`: title or author search
- `language`: language code, for example `fr` or `en`
- `topic`: subject/topic search
- `genre`: alias of `topic`, useful for Discover filter chips
- `page`: Plumora page index, starting at `0`

The backend queries Gutendex with `sort=popular` and `copyright=false` by default, enriches each result with a cover URL from Gutendex or Open Library when available, and returns:

```json
{
  "content": [
    {
      "externalId": "123",
      "source": "GUTENDEX",
      "title": "Les Miserables",
      "authors": ["Victor Hugo"],
      "summary": "Un roman social.",
      "subjects": ["French fiction"],
      "languages": ["fr"],
      "copyright": false,
      "mediaType": "Text",
      "downloadCount": 42,
      "coverUrl": "https://covers.openlibrary.org/b/id/987-L.jpg?default=false",
      "readUrl": "https://www.gutenberg.org/files/123/123-h/123-h.htm",
      "formats": {
        "text/html": "https://www.gutenberg.org/files/123/123-h/123-h.htm"
      },
      "sourceUrl": "https://www.gutenberg.org/ebooks/123",
      "imported": true,
      "internalBookId": "2f8f3d0d-9d4b-4508-aeb5-981a3af4a7d6"
    }
  ],
  "page": 0,
  "size": 32,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

GET `/external-books/{gutendexId}`

Returns the same `ExternalBookDto` shape for one Gutendex book. If the Gutendex book was already imported into Plumora, `imported=true` and `internalBookId` contains the internal `books.id_book` value. Otherwise `imported=false` and `internalBookId=null`.

GET `/external-books/filters`

Returns suggested filters for the Discover page. `topic=null` means no topic filter.

```json
[
  {"label": "Tous", "topic": null},
  {"label": "Fantasy", "topic": "fantasy"},
  {"label": "Romance", "topic": "romance"},
  {"label": "Thriller", "topic": "thriller"},
  {"label": "Sci-Fi", "topic": "science fiction"},
  {"label": "Mystere", "topic": "mystery"},
  {"label": "Aventure", "topic": "adventure"},
  {"label": "Horreur", "topic": "horror"}
]
```

GET `/external-books/{gutendexId}/reviews`

Returns Plumora reader reviews attached to a Gutendex book, even if the book has not been imported into the internal catalog.

```json
[
  {
    "id": "2f8f3d0d-9d4b-4508-aeb5-981a3af4a7d6",
    "externalId": "2701",
    "source": "GUTENDEX",
    "userId": "4fb2ad69-d2e5-4d99-8e11-73f1815c8d3f",
    "username": "clara",
    "rating": 5,
    "comment": "Excellent classique.",
    "createdAt": "2026-07-04T10:00:00",
    "updatedAt": null
  }
]
```

POST `/external-books/{gutendexId}/reviews`

Reader-only. Creates a Plumora review for a Gutendex book.

```json
{
  "rating": 5,
  "comment": "Excellent classique."
}
```

POST `/books/import/gutendex/{gutendexId}`

Authenticated users only. Imports the Gutendex book into the Plumora catalog as `PUBLISHED` and `PUBLIC`, stores external metadata and avoids duplicates through `externalSource + externalId`.

During import, Plumora downloads a readable Gutendex format in this order:

- `text/html`
- `text/plain`

The downloaded content is sanitized before persistence. For the MVP, the import creates one internal chapter named `Texte intégral`; after that, the existing reader endpoint can open the imported book directly:

GET `/books/{bookId}/read`

If no readable `text/html` or `text/plain` format exists, the import returns a business error instead of creating an unreadable book. The original external `readUrl` remains available as metadata/fallback.

## Reading

GET `/books/{bookId}/read`
GET `/reading-progress/my`
GET `/books/{bookId}/reading-progress`
POST `/books/{bookId}/reading-progress`
PUT `/books/{bookId}/reading-progress`
PATCH `/books/{bookId}/reading-progress/finish`

## Favorites

POST `/books/{bookId}/favorites`
DELETE `/books/{bookId}/favorites`
GET `/favorites/my`
GET `/books/{bookId}/favorites/status`

## Reviews

POST `/books/{bookId}/reviews`
GET `/books/{bookId}/reviews`
GET `/reviews/my`
PUT `/reviews/{reviewId}`
DELETE `/reviews/{reviewId}`

`rating` is the star rating value and must be between 1 and 5.
The same user can post several reviews/comments on the same published public book.

## Beta-reading

POST `/books/{bookId}/beta-campaigns`
GET `/books/{bookId}/beta-campaigns`
GET `/beta-campaigns/{campaignId}`
PATCH `/beta-campaigns/{campaignId}/close`
PATCH `/beta-campaigns/{campaignId}/cancel`

POST `/beta-campaigns/{campaignId}/invitations`
GET `/beta-campaigns/{campaignId}/invitations`
GET `/beta-invitations/my-invitations`
PATCH `/beta-invitations/{invitationId}/accept`
PATCH `/beta-invitations/{invitationId}/refuse`

GET `/beta-campaigns/{campaignId}/chapters`
PUT `/beta-campaigns/{campaignId}/chapters`

POST `/beta-comments`
GET `/books/{bookId}/beta-comments`
GET `/beta-campaigns/{campaignId}/comments`
GET `/chapters/{chapterId}/beta-comments`
PATCH `/beta-comments/{commentId}/status`
DELETE `/beta-comments/{commentId}`

## AI

POST `/ai/writing/suggestions`
GET `/ai/writing/requests`
GET `/ai/writing/requests/{requestId}`
PATCH `/ai/writing/suggestions/{suggestionId}/accept`
PATCH `/ai/writing/suggestions/{suggestionId}/modify`
PATCH `/ai/writing/suggestions/{suggestionId}/ignore`

POST `/ai/recommendations/books`
GET `/ai/recommendations/my-requests`
GET `/ai/recommendations/requests/{requestId}`

### Plumo IA (stateless, Gemini-backed)

POST `/ai/writing/rewrite`
POST `/ai/writing/summarize`
POST `/ai/writing/continue`
POST `/ai/writing/titles`
POST `/ai/beta-reading/analyze`
POST `/ai/books/recommend`

## Notifications

GET `/notifications/my`
GET `/notifications/unread-count`
PATCH `/notifications/{notificationId}/read`
PATCH `/notifications/read-all`
DELETE `/notifications/{notificationId}`

## Reports

POST `/books/{bookId}/reports`
GET `/reports/my`
GET `/reports`
PATCH `/reports/{reportId}/status`

Only authenticated users can report published public books.
Only admins can list all reports and update report status.

## Admin

All `/admin/**` routes require the `ADMIN` role (`@PreAuthorize("hasRole('ADMIN')")` on the whole controller). Non-admin users get 403, unauthenticated requests get 401. See `docs/admin.md` for the full module documentation.

GET `/admin/dashboard`
GET `/admin/users`
PATCH `/admin/users/{userId}/disable`
PATCH `/admin/users/{userId}/enable`
GET `/admin/books`
PATCH `/admin/books/{bookId}/archive`
GET `/admin/reports`
GET `/admin/audit-logs`

Every sensitive admin action (user disable/enable, book archive) is recorded in `admin_audit_logs` and can be filtered on `/admin/audit-logs` by `action`, `adminId`, `targetType`, `dateFrom` and `dateTo`.
