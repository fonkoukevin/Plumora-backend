# Module Administration

Le module Admin donne aux utilisateurs ayant le role `ADMIN` une vue d'ensemble de la plateforme
(tableau de bord, utilisateurs, catalogue, signalements, supervision de Plumo IA) et la capacite
d'agir dessus, avec une tracabilite complete via un journal d'audit.

Toutes les routes vivent sous `/admin/**` et sont protegees par `@PreAuthorize("hasRole('ADMIN')")`
au niveau du controleur (`AdminController`). Voir aussi `docs/api-contract.md` pour la liste brute
des routes.

## Securite

- **Non authentifie** -> `401 Unauthorized` (`RestAuthenticationEntryPoint`), corps JSON
  `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`).
- **Authentifie mais pas ADMIN** -> `403 Forbidden` (`AccessDeniedException` intercepte soit par
  `RestAccessDeniedHandler` au niveau du filtre, soit par `GlobalExceptionHandler` quand elle est
  levee par `@PreAuthorize` a l'interieur du controleur), meme corps JSON.
- **Authentifie et ADMIN** -> acces normal.

Ce comportement est verifie par `AdminControllerSecurityTest` (`@WebMvcTest` + `SecurityConfig`
reel, sans base de donnees) sur un echantillon de routes GET/PATCH representatif de chaque
sous-domaine (dashboard, utilisateurs, signalements, audit-logs).

## Tableau de bord

`GET /admin/dashboard` retourne `AdminDashboardDto` : nombre total et actif d'utilisateurs, nombre
total de livres et repartition Plumora / domaine public, signalements en attente et resolus,
livres archives, nombre cumule d'appels a Plumo IA (redaction + recommandations), et les 10
dernieres actions d'administration (`AdminActionLogDto`).

## Gestion des utilisateurs

- `GET /admin/users?query=&role=&status=` — liste filtrable (recherche sur username/email/nom,
  role, statut `ACTIVE`/`DISABLED`). `UserStatus` est derive du booleen `User.active` existant, ce
  n'est pas une nouvelle colonne.
- `GET /admin/users/{userId}` — detail avec `booksCount` et `reportsCount`.
- `PATCH /admin/users/{userId}/status` — `{ "status": "ACTIVE"|"DISABLED", "reason": "optionnel" }`.
- `PATCH /admin/users/{userId}/role` — `{ "roles": ["AUTHOR", "READER", ...] }`. Refuse (`400`)
  de retirer le role `ADMIN` au dernier administrateur restant.
- `PATCH /admin/users/{userId}/disable` et `/enable` — raccourcis historiques equivalents a
  `PATCH .../status`.

## Gestion du catalogue

- `GET /admin/books?query=&type=&status=` — liste filtrable (titre/auteur, type
  `PLUMORA_WORK`/`PUBLIC_DOMAIN` derive de `externalSource`, statut).
- `GET /admin/books/{bookId}` — detail avec `reportsCount` et `chaptersCount`.
- `PATCH /admin/books/{bookId}/status` — `{ "status": "...", "reason": "optionnel" }`. Archive ou
  restaure un livre ; la visibilite est forcee a `PRIVATE` des qu'un livre est archive ou restaure
  depuis l'etat archive.
- `PATCH /admin/books/{bookId}/metadata` — `{ "title", "authors", "summary", "subjects",
  "languages", "coverUrl" }`, tous optionnels ; seuls les champs fournis sont modifies.
- `PATCH /admin/books/{bookId}/archive` et `DELETE /admin/books/{bookId}` — raccourcis
  equivalents (pas de suppression physique, pour garder une trace).
- `POST /admin/books/import/gutendex/{gutendexId}` — reutilise
  `ExternalBookService.importGutendexBook` (dedoublonnage sur `externalSource`+`externalId`,
  `BusinessException` si aucun format lisible, `ExternalServiceUnavailableException` si Gutendex
  est injoignable) mais reserve aux admins, sans passer par le compte de l'utilisateur courant.

## Signalements

- `GET /admin/reports` et `GET /admin/reports/{reportId}` — meme forme que les routes existantes
  (`ReportResponse`).
- `PATCH /admin/reports/{reportId}/resolve` et `PATCH /admin/reports/{reportId}/reject` —
  corps optionnel `{ "reason": "optionnel" }` (ou aucun corps). Deplacent le signalement vers
  `RESOLVED`/`DISMISSED` (les statuts existants du module Report sont conserves, pas de nouveau
  vocabulaire admin) et horodatent `resolvedAt`.

## Supervision de Plumo IA

- `GET /admin/ai/status` — `{ "enabled", "updatedAt", "providerName", "modelName",
  "totalWritingRequests", "totalRecommendationRequests" }`. `providerName`/`modelName` sont
  informatifs (ex. `gemini` / `gemini-flash-lite-latest`) ; **la cle API n'est jamais exposee**.
- `PATCH /admin/ai/settings` — `{ "enabled": true|false, "reason": "optionnel" }`. Bascule un
  interrupteur en memoire (`AiFeatureToggle`, un bean Spring singleton, meme logique que
  `AiUsageLimiter` : reinitialise au redemarrage, suffisant pour un MVP).
- Quand Plumo IA est desactivee, **les trois services IA** (`AiWritingService`,
  `AiBetaReadingAnalysisService`, `AiRecommendationService`) rejettent toute requete avec
  `503 Service Unavailable` (`AiProviderUnavailableException`) avant meme d'appeler le provider —
  la coupure est donc verifiee a la source, pas seulement au niveau des routes `/admin/ai/**`.

## Journal d'audit

Chaque action sensible (changement de statut/role utilisateur, archivage/restauration/modification
de metadonnees d'un livre, import Gutendex, resolution/rejet de signalement, changement des
parametres IA) est enregistree dans la table `admin_audit_logs` (migration `V18`) via
`AdminAuditLogService.logAction(admin, action, targetType, targetId, description)`.

`GET /admin/audit-logs?action=&adminId=&targetType=&dateFrom=&dateTo=` liste les 200 entrees les
plus recentes correspondant aux filtres fournis (tous optionnels), triees par date decroissante.

## Tests

- `AdminServiceTest` — tests unitaires (Mockito) de toute la logique metier : dashboard, filtres
  utilisateurs/livres, protection du dernier administrateur, archivage/restauration, import
  Gutendex, signalements, supervision IA.
- `AdminControllerSecurityTest` — verifie le triptyque 401 (non authentifie) / 403 (authentifie
  sans role ADMIN) / 200 (ADMIN) sur un echantillon de routes representatif.
- `AiWritingServiceTest`, `AiBetaReadingAnalysisServiceTest`, `AiRecommendationServiceTest` —
  chacun verifie qu'une requete est rejetee avec `AiProviderUnavailableException` quand
  `AiFeatureToggle` est desactive.

Lancer la suite complete (voir aussi la racine de `README.md`) :

```bash
docker run --rm -v "$(pwd -W)":/app -w /app -v plumora_maven_repo:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn -o test -q
```

## Compte admin de demonstration (local uniquement)

Un compte `admin@plumora.local` / `Admin123!` est seed uniquement sous le profil Spring `dev`
(jamais en production). Voir `README.md` pour l'activer localement.
