# Plumora app and Swagger test plan

Ce plan sert a verifier que le frontend dialogue correctement avec le backend et que les routes Swagger repondent au besoin metier.

Il complete le script automatique :

```powershell
.\scripts\api-smoke-tests.ps1
```

## Environnement

Backend local :

```text
http://localhost:8080/api/v1
```

Swagger local :

```text
http://localhost:8080/api/v1/swagger-ui.html
```

URLs app selon le support :

```text
Android emulator: http://10.0.2.2:8080/api/v1
Desktop local:    http://localhost:8080/api/v1
Physical device:  http://<LAN_IP>:8080/api/v1
```

Comptes de test seedes :

```text
alice.author@plumora.test / password
clara.reader@plumora.test / password
noah.beta@plumora.test / password
admin@plumora.test / password
```

Si les comptes ne sont pas presents :

```powershell
Get-Content .\docker\postgres\seed-test-data.sql | docker exec -i plumora-postgres psql -U plumora -d plumora_db
```

## Methode Swagger

1. Ouvrir Swagger.
2. Appeler `POST /auth/login`.
3. Copier le `token`.
4. Cliquer sur `Authorize`.
5. Entrer :

```text
Bearer <token>
```

Refaire cette operation quand il faut changer de role utilisateur.

## Donnees a noter pendant les tests

```text
authorToken=
readerToken=
betaToken=
adminToken=
bookId=
chapterId=
versionId=
campaignId=
invitationId=
betaCommentId=
reviewId=
reportId=
notificationId=
recommendationRequestId=
aiWritingRequestId=
suggestionId=
```

## App QA checklist

### 1. Authentification

| ID | Action dans l'app | Resultat attendu |
| --- | --- | --- |
| APP-AUTH-01 | Ouvrir l'app sans session | L'app affiche l'ecran de login/register, aucun crash reseau. |
| APP-AUTH-02 | Login avec `clara.reader@plumora.test / password` | Redirection vers l'espace lecteur, token conserve, profil charge. |
| APP-AUTH-03 | Login avec mauvais mot de passe | Message d'erreur clair, pas de session locale creee. |
| APP-AUTH-04 | Aller sur profil/me | Les infos utilisateur viennent de `/auth/me` ou `/users/me`, aucun champ `password` visible. |
| APP-AUTH-05 | Deconnexion puis redemarrage app | Session nettoyee, routes privees non accessibles. |

### 2. Configuration API

| ID | Action dans l'app | Resultat attendu |
| --- | --- | --- |
| APP-CONF-01 | Verifier la base URL dans la config app | Pas d'URL production hardcodee dans les widgets. |
| APP-CONF-02 | Couper le backend puis ouvrir une liste | L'app affiche un etat erreur/retry, pas une page vide incoherente. |
| APP-CONF-03 | Relancer le backend puis retry | Les donnees reviennent sans devoir reinstaller l'app. |

### 3. Catalogue public

| ID | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- |
| APP-CAT-01 | Ouvrir Decouvrir sans login si l'app le permet | `GET /catalog/books` | Liste seulement des livres `PUBLISHED/PUBLIC`. |
| APP-CAT-02 | Rechercher un titre | `GET /catalog/books/search?q=...` | Resultats filtres, etat vide propre si rien. |
| APP-CAT-03 | Ouvrir le detail d'un livre | `GET /catalog/books/{bookId}` | Detail coherent, auteur, genre, image, nombre de chapitres. |
| APP-CAT-04 | Ouvrir populaires/recents si ecrans presents | `GET /catalog/books/popular`, `GET /catalog/books/latest` | Tri coherent, pagination sans doublons visibles. |

### 4. Parcours auteur

Se connecter avec `alice.author@plumora.test`.

| ID | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- |
| APP-AUTHOR-01 | Ouvrir Ecrire / Mes livres | `GET /books/my-books` | Uniquement les livres de l'auteur connecte. |
| APP-AUTHOR-02 | Creer un livre | `POST /books` | Livre cree en `DRAFT` et `PRIVATE`. |
| APP-AUTHOR-03 | Modifier un livre | `PUT /books/{bookId}` | Donnees mises a jour, retour visible dans l'app. |
| APP-AUTHOR-04 | Ajouter un chapitre | `POST /books/{bookId}/chapters` | Chapitre visible dans la liste du livre. |
| APP-AUTHOR-05 | Creer une version | `POST /chapters/{chapterId}/versions` | Version visible dans l'historique. |
| APP-AUTHOR-06 | Restaurer une version | `POST /chapter-versions/{versionId}/restore` | Contenu du chapitre restaure. |
| APP-AUTHOR-07 | Publier sans chapitre | `PATCH /books/{bookId}/publish` | Erreur metier affichee. |
| APP-AUTHOR-08 | Publier avec chapitre | `PATCH /books/{bookId}/publish` | Livre en `PUBLISHED`, `PUBLIC`, `publishedAt` non null. |
| APP-AUTHOR-09 | Archiver un livre | `PATCH /books/{bookId}/archive` ou `DELETE /books/{bookId}` | Livre archive, non visible dans le catalogue public. |

### 5. Parcours lecteur

Se connecter avec `clara.reader@plumora.test`.

| ID | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- |
| APP-READ-01 | Ouvrir un livre publie en lecture | `GET /books/{bookId}/read` | Chapitres charges, progression initialisee. |
| APP-READ-02 | Sauvegarder la progression | `PUT /books/{bookId}/reading-progress` | Pourcentage et chapitre courant conserves. |
| APP-READ-03 | Terminer la lecture | `PATCH /books/{bookId}/reading-progress/finish` | Progression a 100%, date de fin visible si affichee. |
| APP-READ-04 | Ajouter aux favoris | `POST /books/{bookId}/favorites` | Etat favori actif. |
| APP-READ-05 | Retirer des favoris | `DELETE /books/{bookId}/favorites` | Etat favori inactif. |
| APP-READ-06 | Ajouter une review | `POST /books/{bookId}/reviews` | Review visible sur detail livre et dans mes reviews. |
| APP-READ-07 | Modifier/supprimer une review si UI presente | `PUT /reviews/{reviewId}`, `DELETE /reviews/{reviewId}` | Modification ou suppression refletee dans l'app. |

### 6. Beta-reading

Utiliser `alice.author@plumora.test` puis `noah.beta@plumora.test`.

Depuis la refonte du role beta-lecteur, l'acces en lecture/commentaire n'est plus conditionne par une invitation : tout utilisateur avec le role `BETA_READER` peut acceder a une campagne `ACTIVE`, voir ses chapitres partages et y commenter. L'invitation reste disponible pour que l'auteur notifie un lecteur en particulier, mais accepter/refuser n'a plus d'impact sur l'acces.

| ID | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- |
| APP-BETA-01 | Auteur cree une campagne beta | `POST /books/{bookId}/beta-campaigns` | Campagne `ACTIVE`, tous les BETA_READER recoivent une notification `BETA_CAMPAIGN_OPEN`. |
| APP-BETA-02 | Auteur partage des chapitres | `PUT /beta-campaigns/{campaignId}/chapters` | Seuls les chapitres choisis sont exposes. |
| APP-BETA-03 | Noah decouvre les campagnes ouvertes | `GET /beta-campaigns` | La campagne d'Alice apparait, sans avoir ete invite. |
| APP-BETA-04 | Auteur invite Noah en plus (optionnel) | `POST /beta-campaigns/{campaignId}/invitations` | Invitation `PENDING`, notification ciblee creee. |
| APP-BETA-05 | Noah voit les chapitres partages | `GET /beta-campaigns/{campaignId}/chapters` | Acces autorise sans invitation prealable ; pas d'acces aux chapitres non partages. |
| APP-BETA-06 | Noah cree un commentaire structure | `POST /beta-comments` | Commentaire `OPEN`, type/priorite/status visibles. |
| APP-BETA-07 | Auteur voit et traite le commentaire | `GET /books/{bookId}/beta-comments`, `PATCH /beta-comments/{commentId}/status` | Statut mis a jour. |

### 7. IA

| ID | Role | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- | --- |
| APP-AI-01 | Auteur | Demander une suggestion d'ecriture | `POST /ai/writing/suggestions` | Suggestion `PENDING`, le manuscrit n'est pas modifie automatiquement. |
| APP-AI-02 | Auteur | Accepter/modifier/ignorer | `PATCH /ai/writing/suggestions/{id}/accept|modify|ignore` | Statut change, pas de modification automatique du chapitre. |
| APP-AI-03 | Lecteur | Demander recommandations | `POST /ai/recommendations/books` | Reponses avec `match_score` et `reasons`. |
| APP-AI-04 | Lecteur | Consulter historique IA | `GET /ai/recommendations/my-requests` | Requete retrouvee. |

### 8. Notifications

| ID | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- |
| APP-NOTIF-01 | Ouvrir notifications | `GET /notifications/my` | Liste triee, etat vide gere. |
| APP-NOTIF-02 | Lire le badge non lu | `GET /notifications/unread-count` | Badge coherent. |
| APP-NOTIF-03 | Marquer une notification lue | `PATCH /notifications/{notificationId}/read` | Badge diminue. |
| APP-NOTIF-04 | Tout marquer lu | `PATCH /notifications/read-all` | Toutes les notifications sont lues. |
| APP-NOTIF-05 | Supprimer une notification | `DELETE /notifications/{notificationId}` | Notification retiree. |

### 9. Reports et admin

| ID | Role | Action dans l'app | Route attendue | Resultat attendu |
| --- | --- | --- | --- | --- |
| APP-REPORT-01 | Lecteur | Signaler un livre publie | `POST /books/{bookId}/reports` | Report `OPEN`. |
| APP-REPORT-02 | Lecteur | Voir mes reports | `GET /reports/my` | Report visible uniquement pour le reporter. |
| APP-ADMIN-01 | Admin | Voir utilisateurs | `GET /admin/users` | Liste chargee, pas de mot de passe expose. |
| APP-ADMIN-02 | Admin | Desactiver/reactiver un user test | `PATCH /admin/users/{userId}/disable`, `enable` | `active` bascule correctement. |
| APP-ADMIN-03 | Admin | Voir reports | `GET /reports` ou `GET /admin/reports` | Tous les reports visibles. |
| APP-ADMIN-04 | Admin | Changer statut report | `PATCH /reports/{reportId}/status` | Statut `IN_REVIEW`, `RESOLVED` ou `DISMISSED`. |
| APP-ADMIN-05 | Admin | Archiver un livre | `PATCH /admin/books/{bookId}/archive` | Livre archive et retire du catalogue. |

### 10. Tests de securite dans l'app

| ID | Action | Resultat attendu |
| --- | --- | --- |
| APP-SEC-01 | Lecteur tente d'ouvrir une route auteur | L'action est cachee ou retourne une erreur 403 lisible. |
| APP-SEC-02 | Auteur tente de modifier un livre d'un autre auteur | Erreur 403, aucune mutation. |
| APP-SEC-03 | Utilisateur sans role BETA_READER tente d'ouvrir une campagne ou d'y commenter | Erreur 403, aucun chapitre ni commentaire affiche. |
| APP-SEC-04 | Lecteur tente favoris/review sur livre non publie | Erreur metier, action bloquee. |
| APP-SEC-05 | Token expire ou invalide | Retour login ou refresh gere proprement selon l'app. |

## Swagger scenario complet

Les payloads ci-dessous permettent de rejouer un parcours complet dans Swagger. Remplacer les valeurs entre chevrons.

### 1. Login auteur

`POST /auth/login`

```json
{
  "email": "alice.author@plumora.test",
  "password": "password"
}
```

Attendu : `200`, `token`, `user.roles` contient `AUTHOR`.

### 2. Creer un livre

`POST /books`

```json
{
  "title": "Test Swagger Plumora",
  "subtitle": "Parcours manuel",
  "summary": "Livre cree pour verifier les routes depuis Swagger.",
  "coverUrl": "https://placehold.co/600x900/263238/ffffff?text=Swagger",
  "genre": "Fantasy",
  "languageCode": "fr"
}
```

Attendu : `201`, `status=DRAFT`, `visibility=PRIVATE`.

Noter `bookId`.

### 3. Tenter publication trop tot

`PATCH /books/{bookId}/publish`

Attendu : `400`, message indiquant qu'un livre doit avoir au moins un chapitre.

### 4. Ajouter un chapitre

`POST /books/{bookId}/chapters`

```json
{
  "title": "Chapitre 1",
  "content": "Une scene de test pour verifier le backend et le frontend.",
  "chapterOrder": 1
}
```

Attendu : `201`.

Noter `chapterId`.

### 5. Versionner le chapitre

`POST /chapters/{chapterId}/versions`

Attendu : `201`, `versionNumber=1`.

Noter `versionId`.

### 6. Publier le livre

`PATCH /books/{bookId}/publish`

Attendu : `200`, `status=PUBLISHED`, `visibility=PUBLIC`, `publishedAt` non null.

### 7. Verifier catalogue public

Sans token ou avec n'importe quel token :

`GET /catalog/books`

`GET /catalog/books/{bookId}`

`GET /catalog/books/search?q=Test%20Swagger`

Attendu : livre visible dans les trois cas.

### 8. Login lecteur

`POST /auth/login`

```json
{
  "email": "clara.reader@plumora.test",
  "password": "password"
}
```

Remplacer le token dans `Authorize`.

### 9. Lire et progresser

`GET /books/{bookId}/read`

Attendu : livre + chapitres + progression.

`PUT /books/{bookId}/reading-progress`

```json
{
  "currentChapterId": "<chapterId>",
  "progressPercentage": 42.50
}
```

Attendu : progression a `42.50`.

`PATCH /books/{bookId}/reading-progress/finish`

Attendu : progression a `100.00`.

### 10. Favoris

`POST /books/{bookId}/favorites`

Attendu : `201`.

`GET /books/{bookId}/favorites/status`

Attendu : `favorite=true`.

`DELETE /books/{bookId}/favorites`

Attendu : `204`.

### 11. Reviews

`POST /books/{bookId}/reviews`

```json
{
  "rating": 5,
  "comment": "Lecture validee depuis Swagger."
}
```

Attendu : `201`.

Noter `reviewId`.

`GET /books/{bookId}/reviews`

Attendu : review visible.

`PUT /reviews/{reviewId}`

```json
{
  "rating": 4,
  "comment": "Review modifiee depuis Swagger."
}
```

Attendu : `200`, rating modifie.

### 12. Recommandations IA

`POST /ai/recommendations/books`

```json
{
  "query_text": "Je veux une fantasy courte avec du mystere.",
  "mood": "curious",
  "preferred_duration": "short",
  "preferred_genre": "Fantasy"
}
```

Attendu : `201`, `recommendations` contient `match_score` et `reasons`.

### 13. Login beta-reader

`POST /auth/login`

```json
{
  "email": "noah.beta@plumora.test",
  "password": "password"
}
```

Garder le token beta, puis revenir au token auteur pour creer la campagne.

### 14. Creer campagne beta

Avec le token auteur :

`POST /books/{bookId}/beta-campaigns`

```json
{
  "title": "Campagne beta Swagger",
  "instructions": "Verifier rythme, clarte et personnages.",
  "deadline": "2026-12-31"
}
```

Attendu : `201`, `status=ACTIVE`. Chaque utilisateur avec le role `BETA_READER` recoit une notification `BETA_CAMPAIGN_OPEN` (verifiable avec le token Noah sur `GET /notifications/my`).

Noter `campaignId`.

Avec le token Noah (sans invitation) :

`GET /beta-campaigns`

Attendu : `200`, la campagne d'Alice apparait dans la liste.

### 15. Partager chapitre

`PUT /beta-campaigns/{campaignId}/chapters`

```json
{
  "chapterIds": [
    "<chapterId>"
  ]
}
```

Attendu : liste contenant le chapitre.

### 16. Inviter beta-reader (optionnel)

L'invitation ne conditionne plus l'acces (Noah peut deja lire et commenter grace a son role `BETA_READER`) ; elle sert uniquement a notifier un lecteur en particulier.

Il faut connaitre l'id de Noah. Le plus simple :

1. se connecter avec Noah
2. appeler `GET /users/me`
3. noter `id`
4. revenir au token auteur

`POST /beta-campaigns/{campaignId}/invitations`

```json
{
  "betaReaderId": "<noahUserId>"
}
```

Attendu : `201`, `status=PENDING`.

Noter `invitationId`.

### 17. Accepter invitation (optionnel)

Avec le token Noah :

`GET /beta-invitations/my-invitations`

Attendu : invitation visible.

`PATCH /beta-invitations/{invitationId}/accept`

Attendu : `status=ACCEPTED`. Sans faire cette etape, Noah peut deja acceder aux chapitres partages et commenter (verifiable en inversant l'ordre des etapes 17 et 18).

### 18. Commentaire beta

Avec le token Noah :

`POST /beta-comments`

```json
{
  "campaignId": "<campaignId>",
  "chapterId": "<chapterId>",
  "commentText": "Le passage est clair, mais le rythme peut etre plus tendu.",
  "selectedText": "Une scene de test",
  "positionStart": 0,
  "positionEnd": 18,
  "feedbackType": "PACING",
  "priority": "HIGH"
}
```

Attendu : `201`, `status=OPEN`.

Noter `betaCommentId`.

Avec le token auteur :

`PATCH /beta-comments/{betaCommentId}/status`

```json
{
  "status": "RESOLVED"
}
```

Attendu : `status=RESOLVED`.

### 19. Suggestion IA auteur

Avec le token auteur :

`POST /ai/writing/suggestions`

```json
{
  "chapter_id": "<chapterId>",
  "selected_text": "Une scene de test pour verifier le backend.",
  "context_text": "Chapitre de validation Swagger.",
  "action_type": "IMPROVE_STYLE"
}
```

Attendu : `201`, suggestion `PENDING`.

Noter `suggestionId`.

`PATCH /ai/writing/suggestions/{suggestionId}/accept`

Attendu : `status=ACCEPTED`.

Verifier que le contenu du chapitre n'a pas ete modifie automatiquement.

### 20. Notifications

Avec le token Noah :

`GET /notifications/my`

Attendu : notification d'invitation beta.

Avec le token auteur :

`GET /notifications/my`

Attendu : notification de commentaire beta.

Tester :

```text
GET /notifications/unread-count
PATCH /notifications/{notificationId}/read
PATCH /notifications/read-all
DELETE /notifications/{notificationId}
```

### 21. Report et admin

Avec le token Clara :

`POST /books/{bookId}/reports`

```json
{
  "reason": "Test Swagger",
  "description": "Signalement cree pour verifier le workflow admin."
}
```

Attendu : `201`, `status=OPEN`.

Noter `reportId`.

Avec le token admin :

`GET /reports`

`PATCH /reports/{reportId}/status`

```json
{
  "status": "IN_REVIEW"
}
```

Attendu : report visible puis statut mis a jour.

Tester aussi :

```text
GET /admin/users
GET /admin/books
GET /admin/reports
PATCH /admin/users/{userId}/disable
PATCH /admin/users/{userId}/enable
PATCH /admin/books/{bookId}/archive
```

## Tests negatifs Swagger

| ID | Route | Payload/contexte | Attendu |
| --- | --- | --- | --- |
| SWG-NEG-01 | `GET /users/me` | Sans token | `401` ou `403`. |
| SWG-NEG-02 | `POST /books` | Token lecteur | `403`. |
| SWG-NEG-03 | `POST /auth/login` | Mauvais password | `401`. |
| SWG-NEG-04 | `POST /auth/register` | Email deja utilise | `409`. |
| SWG-NEG-05 | `POST /books` | `title` vide | `400`. |
| SWG-NEG-06 | `POST /books/{bookId}/chapters` | `chapterOrder` deja utilise | `400`. |
| SWG-NEG-07 | `PATCH /books/{bookId}/publish` | Livre sans chapitre | `400`. |
| SWG-NEG-08 | `POST /books/{bookId}/favorites` | Livre non publie | `400`. |
| SWG-NEG-09 | `POST /beta-comments` | Token d'un utilisateur sans role BETA_READER | `403`. |
| SWG-NEG-10 | `POST /beta-comments` | Chapitre non partage | `400`. |
| SWG-NEG-11 | `PATCH /reports/{reportId}/status` | Token non-admin | `403`. |
| SWG-NEG-12 | `GET /catalog/books/{bookId}` | Livre archive ou prive | `404`. |

## Signes que le frontend implemente mal une route

- Le back repond correctement dans Swagger mais l'app affiche une page vide.
- Les noms JSON ne correspondent pas : `query_text`, `chapter_id`, `selected_text`, `action_type`, `match_score`, `rank_position`, `is_read`.
- Le token n'est pas envoye avec le prefixe `Bearer`.
- L'app appelle `/api/v1/api/v1/...` a cause d'une base URL double.
- Les routes publiques de catalogue demandent une session inutilement.
- Les erreurs `400`, `401`, `403`, `404`, `409` ne sont pas distinguees dans l'UI.
- Les images relatives comme `uploads/book-covers/...` ne sont pas resolues depuis la base URL backend.
- Les roles ne pilotent pas l'affichage : actions auteur visibles lecteur, actions admin visibles non-admin.
- Les ecrans n'ont pas d'etats loading, error et empty.

## Critere de validation finale

Le frontend est considere correctement branche quand :

1. chaque parcours APP ci-dessus est valide dans l'application,
2. le meme parcours reussit dans Swagger,
3. les erreurs metier sont affichees proprement,
4. les donnees creees dans l'app sont visibles dans Swagger et inversement,
5. aucune route privee ne fonctionne sans token ou avec le mauvais role.
