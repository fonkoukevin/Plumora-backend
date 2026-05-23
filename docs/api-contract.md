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

GET `/admin/users`
PATCH `/admin/users/{userId}/disable`
PATCH `/admin/users/{userId}/enable`
GET `/admin/books`
PATCH `/admin/books/{bookId}/archive`
GET `/admin/reports`
