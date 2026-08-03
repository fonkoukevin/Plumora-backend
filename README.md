# Plumora Backend

API REST de **Plumora**, une plateforme de lecture et d’écriture de livres numériques. Ce dépôt contient le backend Spring Boot utilisé par les applications Flutter mobile, desktop et web.

Le MVP permet notamment de gérer l’authentification, les manuscrits, les chapitres, la publication directe, le catalogue de lecture, la bêta-lecture, les interactions des lecteurs, Plumo IA, les notifications et l’administration de la plateforme.

> Le frontend Flutter vit dans un dépôt séparé : [Plumora-frontend](https://github.com/fonkoukevin/Plumora-frontend).

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Démarrage rapide avec Docker](#démarrage-rapide-avec-docker)
- [Démarrage avec Java et Maven](#démarrage-avec-java-et-maven)
- [Configuration](#configuration)
- [Documentation et routes de l’API](#documentation-et-routes-de-lapi)
- [Authentification](#authentification)
- [Règles métier importantes](#règles-métier-importantes)
- [Base de données et migrations](#base-de-données-et-migrations)
- [Tests et qualité](#tests-et-qualité)
- [Docker et production](#docker-et-production)
- [Dépannage](#dépannage)
- [Documentation complémentaire](#documentation-complémentaire)

## Fonctionnalités

### Comptes et sécurité

- inscription et connexion par email/mot de passe ;
- connexion Google par vérification d’un ID token côté backend ;
- récupération et réinitialisation du mot de passe ;
- authentification stateless avec JWT ;
- profils et rôles multiples : `AUTHOR`, `READER`, `BETA_READER`, `ADMIN` ;
- contrôle des autorisations par rôle et vérification de la propriété des ressources dans les services.

### Écriture et publication

- création et gestion des livres ;
- ajout, modification, suppression et réorganisation des chapitres ;
- historique des versions de chapitre et restauration d’une version ;
- upload local des couvertures de livre ;
- calcul du nombre de chapitres et de mots ;
- publication directe par l’auteur ;
- archivage des livres.

### Catalogue et lecture

- catalogue public des livres Plumora publiés ;
- recherche, filtres, nouveautés et livres populaires ;
- catalogue du domaine public alimenté par Project Gutenberg ;
- intégrations Gutendex et Open Library comme sources complémentaires ;
- import de livres du domaine public ;
- lecture des chapitres, progression de lecture, favoris et avis ;
- avis dédiés aux livres externes ;
- signalement de contenus.

### Bêta-lecture

- campagnes de bêta-lecture créées par les auteurs ;
- partage d’une sélection de chapitres ;
- invitations ciblées et campagnes actives accessibles aux bêta-lecteurs ;
- suivi des chapitres consultés ;
- commentaires structurés par type, priorité et statut ;
- notifications lors des principaux événements.

### Plumo IA

- reformulation, résumé et continuation d’un texte ;
- suggestions de titres ;
- pré-analyse d’un manuscrit avant bêta-lecture ;
- recommandations de livres publiés ;
- provider `mock` utilisable sans clé externe ;
- provider Gemini optionnel, appelé uniquement par le backend ;
- aucune suggestion n’est appliquée automatiquement au manuscrit.

### Administration

- tableau de bord et statistiques ;
- gestion des utilisateurs et de leurs rôles/statuts ;
- gestion et archivage du catalogue ;
- import administratif de livres Gutendex ;
- modération des signalements ;
- activation ou désactivation de Plumo IA ;
- journal d’audit des actions sensibles.

Le MVP n’inclut pas les paiements, les royalties, les abonnements, la marketplace, le chat temps réel ni une validation administrative préalable à la publication.

## Stack technique

| Composant | Technologie |
| --- | --- |
| Langage | Java 21 |
| Framework | Spring Boot 3.3.5 |
| API | Spring MVC, Bean Validation, springdoc OpenAPI |
| Sécurité | Spring Security, JWT, BCrypt, Google ID Token |
| Persistance | Spring Data JPA, PostgreSQL 17 |
| Migrations | Flyway |
| Emails | Spring Mail, provider journal ou SMTP |
| IA | Provider simulé ou API Gemini |
| Tests | JUnit 5, Mockito, Spring Security Test, Testcontainers |
| Couverture | JaCoCo |
| Build | Maven Wrapper, Docker multi-stage |
| Exploitation | Docker Compose, Caddy, GitHub Actions, GHCR |

## Architecture

Le code est organisé par domaine métier dans le package racine `com.plumora.api` :

```text
src/main/java/com/plumora/api/
├── admin/
├── ai/
├── betaReading/
├── book/
├── notification/
├── reading/
├── report/
├── user/
└── shared/
```

Chaque domaine suit, lorsque nécessaire, une architecture en couches :

```text
presentation  -> contrôleurs REST, DTO et mappers
application   -> services, cas d’usage et transactions
domain        -> entités, enums et règles métier
infrastructure-> repositories JPA, stockage et clients externes
```

Principes appliqués :

- les entités JPA ne sont jamais exposées directement par l’API ;
- les contrôleurs délèguent la logique métier aux services ;
- les entrées sont validées avec Bean Validation ;
- les services contrôlent les rôles et la propriété des ressources ;
- les erreurs sont uniformisées par un gestionnaire global ;
- les appels à Gemini, Gutenberg, Gutendex, Open Library et SMTP restent côté serveur.

## Démarrage rapide avec Docker

### Prérequis

- Docker Engine ou Docker Desktop ;
- le plugin Docker Compose (`docker compose version`).

Aucun JDK ni Maven local n’est nécessaire avec cette méthode.

### 1. Préparer la configuration locale

Sous macOS ou Linux :

```bash
cp .env.example .env
```

Sous PowerShell :

```powershell
Copy-Item .env.example .env
```

Le fichier `.env` est ignoré par Git. Les valeurs fournies conviennent au développement local et peuvent être adaptées avant le lancement.

### 2. Construire et démarrer la stack

```bash
docker compose up -d --build
```

Services démarrés :

| Service | Adresse | Utilité |
| --- | --- | --- |
| API | `http://localhost:8080/api/v1` | Backend Spring Boot |
| PostgreSQL | `localhost:5432` | Base `plumora_db` |
| pgAdmin | `http://localhost:5050` | Administration de PostgreSQL |

Identifiants pgAdmin locaux par défaut : `admin@plumora.com` / `admin`.

Au premier lancement, Flyway crée le schéma. La synchronisation initiale du catalogue Gutenberg peut également prendre un peu de temps ; son échec ne bloque pas le démarrage de l’API.

### 3. Vérifier le démarrage

```bash
docker compose ps
docker compose logs -f api
curl http://localhost:8080/api/v1/actuator/health
```

Réponse attendue :

```json
{"status":"UP"}
```

### 4. Arrêter la stack

```bash
docker compose down
```

Pour supprimer également les volumes et toutes les données locales :

```bash
docker compose down -v
```

> Cette dernière commande efface la base PostgreSQL, les uploads et les données pgAdmin du projet local.

## Démarrage avec Java et Maven

### Prérequis

- JDK 21 ;
- Docker pour PostgreSQL et les tests Testcontainers.

Démarrer uniquement PostgreSQL :

```bash
docker compose up -d postgres
```

Puis lancer l’API avec le Maven Wrapper.

Sous macOS ou Linux :

```bash
./mvnw spring-boot:run
```

Sous Windows :

```powershell
.\mvnw.cmd spring-boot:run
```

La configuration par défaut se connecte à `jdbc:postgresql://localhost:5432/plumora_db`.

### Profil de développement

Le profil `dev` active des logs détaillés, le détail du healthcheck et un compte administrateur de démonstration.

Sous macOS ou Linux :

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Sous PowerShell :

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

Compte créé de façon idempotente :

- email : `admin@plumora.local` ;
- mot de passe : `Admin123!` ;
- rôle : `ADMIN`.

Ce compte n’est créé que sous le profil `dev` et ne doit jamais être utilisé en production.

## Configuration

La configuration commune se trouve dans [`application.yml`](src/main/resources/application.yml). Les profils [`application-dev.yml`](src/main/resources/application-dev.yml) et [`application-prod.yml`](src/main/resources/application-prod.yml) la complètent.

Le Compose local transmet au conteneur les variables déclarées dans la section `api.environment` de [`docker-compose.yml`](docker-compose.yml) : base de données, JWT, uploads, IA et CORS. Les variables Google OAuth et SMTP ci-dessous sont déjà prévues dans le Compose de production ; pour les tester localement sans modifier le Compose, lancer l’application avec Maven et définir les variables dans le terminal.

### Application et base de données

| Variable | Description | Valeur locale par défaut |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Profils Spring actifs (`dev`, `prod`) | vide |
| `SERVER_PORT` | Port HTTP de Spring Boot | `8080` |
| `SPRING_DATASOURCE_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/plumora_db` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur PostgreSQL | `plumora` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | `plumora` |
| `POSTGRES_DB` | Base créée par Docker Compose | `plumora_db` |
| `POSTGRES_USER` | Utilisateur créé par Docker Compose | `plumora` |
| `POSTGRES_PASSWORD` | Mot de passe créé par Docker Compose | `plumora` |
| `PLUMORA_UPLOAD_DIR` | Répertoire des fichiers uploadés | `uploads` hors Docker, `/app/uploads` dans Compose |

### Authentification et frontend

| Variable | Description | Valeur locale par défaut |
| --- | --- | --- |
| `JWT_SECRET` | Secret de signature JWT | valeur de développement uniquement |
| `JWT_EXPIRATION` | Durée de validité du JWT en millisecondes | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées, séparées par des virgules | localhost et 127.0.0.1 |
| `GOOGLE_OAUTH_CLIENT_ID` | Audience OAuth attendue pour Google Sign-In | vide |
| `FRONTEND_BASE_URL` | Base des liens de réinitialisation | `http://localhost:3000` |
| `PASSWORD_RESET_TOKEN_EXPIRATION_MINUTES` | Durée d’un token de réinitialisation | `60` |

### Plumo IA et services externes

| Variable | Description | Valeur locale par défaut |
| --- | --- | --- |
| `AI_PROVIDER` | Provider `mock` ou `gemini` | `mock` |
| `GEMINI_API_KEY` | Clé Gemini, requise si le provider vaut `gemini` | vide |
| `GEMINI_MODEL` | Modèle Gemini | `gemini-flash-lite-latest` |
| `GEMINI_TIMEOUT_SECONDS` | Timeout des appels Gemini | `30` |
| `GEMINI_MAX_INPUT_CHARS` | Taille maximale des entrées IA | `12000` |
| `GEMINI_BASE_URL` | URL de l’API Gemini | URL officielle Google |
| `GUTENDEX_BASE_URL` | URL de Gutendex | `https://gutendex.com` |
| `GUTENBERG_CATALOG_URL` | Catalogue CSV Project Gutenberg | URL officielle Gutenberg |
| `OPEN_LIBRARY_BASE_URL` | URL d’Open Library | `https://openlibrary.org` |

### Réinitialisation de mot de passe

| Variable | Description | Valeur locale par défaut |
| --- | --- | --- |
| `MAIL_PROVIDER` | `log` pour journaliser le lien ou `smtp` pour envoyer un email | `log` |
| `SMTP_HOST` | Serveur SMTP | `smtp.gmail.com` |
| `SMTP_PORT` | Port SMTP | `587` |
| `SMTP_USERNAME` | Identifiant SMTP | vide |
| `SMTP_PASSWORD` | Mot de passe ou mot de passe d’application | vide |
| `MAIL_FROM` | Adresse expéditrice ; retombe sur `SMTP_USERNAME` si vide | vide |

En production, les valeurs sensibles ne possèdent volontairement pas de valeur de secours acceptable. Le `ProductionEnvironmentValidator` refuse notamment les secrets faibles, la combinaison des profils `dev` et `prod`, ou Gemini activé sans clé.

## Documentation et routes de l’API

Toutes les routes partagent le préfixe :

```text
/api/v1
```

Une fois l’application lancée hors profil `prod` :

- Swagger UI : [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- OpenAPI JSON : [http://localhost:8080/api/v1/api-docs](http://localhost:8080/api/v1/api-docs)
- Healthcheck : [http://localhost:8080/api/v1/actuator/health](http://localhost:8080/api/v1/actuator/health)
- Informations : [http://localhost:8080/api/v1/actuator/info](http://localhost:8080/api/v1/actuator/info)

Swagger et l’endpoint OpenAPI sont désactivés dans le profil `prod`.

### Principales familles de routes

| Domaine | Exemples |
| --- | --- |
| Authentification | `POST /auth/register`, `POST /auth/login`, `POST /auth/google`, `GET /auth/me` |
| Utilisateur | `GET /users/me`, `PUT /users/me`, `PUT /users/me/roles` |
| Livres | `POST /books`, `GET /books/my-books`, `PATCH /books/{bookId}/publish` |
| Chapitres | `POST /books/{bookId}/chapters`, `PUT /chapters/{chapterId}` |
| Catalogue | `GET /catalog/books`, `GET /catalog/books/search`, `GET /catalog/genres` |
| Domaine public | `GET /external-books`, `GET /external-books/{gutendexId}` |
| Lecture | `GET /books/{bookId}/read`, `PUT /books/{bookId}/reading-progress` |
| Favoris et avis | `POST /books/{bookId}/favorites`, `POST /books/{bookId}/reviews` |
| Bêta-lecture | `POST /books/{bookId}/beta-campaigns`, `POST /beta-comments` |
| Plumo IA | `POST /ai/writing/rewrite`, `POST /ai/books/recommend` |
| Notifications | `GET /notifications/my`, `PATCH /notifications/read-all` |
| Signalements | `POST /books/{bookId}/reports`, `GET /reports/my` |
| Administration | `GET /admin/dashboard`, `GET /admin/users`, `GET /admin/reports` |

Le contrat détaillé des requêtes et réponses se trouve dans [`docs/api-contract.md`](docs/api-contract.md).

## Authentification

### Créer un compte

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Alice",
    "lastname": "Martin",
    "username": "alice",
    "email": "alice@example.com",
    "password": "MotDePasse123!"
  }'
```

Un nouveau compte reçoit le rôle `READER` par défaut. La réponse contient un token JWT :

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "user": {}
}
```

### Se connecter

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"MotDePasse123!"}'
```

### Appeler une route protégée

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <jwt>"
```

L’API ne conserve aucune session serveur : le token doit être transmis dans l’en-tête `Authorization` de chaque requête protégée.

### Routes publiques

- inscription, connexion, Google Sign-In et réinitialisation du mot de passe ;
- consultation du catalogue Plumora ;
- consultation du catalogue externe ;
- couvertures publiques sous `/uploads/**` ;
- Swagger/OpenAPI en dehors de la production ;
- Actuator `health` et `info`.

Les autres routes exigent un JWT valide et, selon le cas, un rôle métier spécifique.

## Règles métier importantes

- Un livre appartient à un seul auteur.
- Seul l’auteur peut modifier son livre ou ses chapitres.
- Un livre archivé ne peut plus être édité.
- La publication est directe : `status = PUBLISHED`, `visibility = PUBLIC` et `publishedAt` renseigné.
- Seuls les livres publiés et publics apparaissent dans le catalogue Plumora.
- Une mise à jour de chapitre peut produire une version restaurable.
- Un utilisateur possède au maximum une progression et un favori par livre.
- Les avis et interactions publiques ne concernent que les livres publiés.
- Les commentaires de bêta-lecture restent privés et structurés.
- Plumo IA ne modifie jamais automatiquement un manuscrit.
- Les recommandations IA ne proposent que des livres publiés existants.
- Les actions d’administration sensibles sont enregistrées dans le journal d’audit.

## Gestion des erreurs

Les erreurs utilisent un format commun :

```json
{
  "timestamp": "2026-08-03T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Explication de l'erreur",
  "path": "/api/v1/books"
}
```

Codes courants :

- `400` : validation ou règle métier ;
- `401` : authentification absente ou invalide ;
- `403` : rôle insuffisant ou ressource appartenant à un autre utilisateur ;
- `404` : ressource inexistante ;
- `409` : doublon ou conflit ;
- `503` : service externe indisponible ou provider non configuré.

## Base de données et migrations

PostgreSQL est l’unique base supportée. Les identifiants principaux sont des UUID.

Le schéma est géré exclusivement par les migrations Flyway situées dans [`src/main/resources/db/migration`](src/main/resources/db/migration). Le projet contient actuellement les migrations `V1` à `V21`.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Règles de contribution :

1. ne jamais modifier une migration déjà appliquée ;
2. créer une nouvelle migration `V<N+1>__description.sql` ;
3. maintenir la migration compatible avec PostgreSQL ;
4. mettre à jour les entités et les tests associés ;
5. valider le démarrage avec une vraie base via Testcontainers.

En cas d’erreur Flyway, l’application refuse de démarrer afin de ne jamais fonctionner avec un schéma incohérent.

## Tests et qualité

### Tests

Sous macOS ou Linux :

```bash
./mvnw clean test
```

Sous Windows :

```powershell
.\mvnw.cmd clean test
```

La suite comprend :

- tests unitaires des services métier ;
- tests des contrôleurs et de la sécurité HTTP ;
- tests de sérialisation et de validation ;
- tests des clients Gemini, Gutenberg, Gutendex et Open Library ;
- tests d’intégration PostgreSQL/Flyway avec Testcontainers ;
- test de démarrage sous le profil de production.

Les tests utilisent le provider IA simulé et n’appellent pas Gemini avec une vraie clé.

### Couverture JaCoCo

```bash
./mvnw clean verify
```

Sous Windows :

```powershell
.\mvnw.cmd clean verify
```

Le rapport HTML est généré dans `target/site/jacoco/index.html`.

### Package et image Docker

```bash
./mvnw clean package
docker build -t plumora-api:local .
```

Le JAR généré se trouve dans `target/`.

## Docker et production

Le [`Dockerfile`](Dockerfile) est multi-stage :

1. compilation Maven avec Java 21 ;
2. image d’exécution `eclipse-temurin:21-jre-alpine` ;
3. exécution avec un utilisateur non-root ;
4. healthcheck sur `/api/v1/actuator/health` ;
5. arrêt gracieux de Spring Boot.

Le [`docker-compose.yml`](docker-compose.yml) racine est réservé au développement local.

La production utilise l’infrastructure du dossier [`deploy/`](deploy/) :

- Caddy pour HTTPS et le reverse proxy ;
- frontend Flutter Web et backend sous forme d’images préconstruites ;
- PostgreSQL privé dans le réseau Docker ;
- sauvegardes et restaurations ;
- scripts de déploiement, rollback et diagnostic ;
- déploiement automatisé par GitHub Actions.

La procédure complète se trouve dans [`deploy/README.md`](deploy/README.md). Ne pas utiliser le Compose local sur le VPS.

### CI/CD

- `backend-ci.yml` : tests, package, construction Docker et smoke tests sur `main` et les pull requests ;
- `release.yml` : publication de l’image `ghcr.io/<owner>/plumora-backend` avec tags de version/SHA ;
- `deploy.yml` : déploiement distant après publication réussie ;
- les images publiées incluent une SBOM, une provenance SLSA et une signature keyless Cosign/Sigstore.

## Dépannage

### L’API ne démarre pas

```bash
docker compose ps
docker compose logs postgres
docker compose logs api
```

Vérifier en priorité :

- que PostgreSQL est `healthy` ;
- que le port `8080` est disponible ;
- que `JWT_SECRET` est valide pour le profil utilisé ;
- qu’aucune migration Flyway n’échoue ;
- que `dev` et `prod` ne sont pas activés simultanément.

### L’API ne rejoint pas PostgreSQL dans Docker

Dans Docker Compose, l’hôte PostgreSQL doit être le nom du service :

```text
jdbc:postgresql://postgres:5432/plumora_db
```

`localhost` désignerait le conteneur de l’API lui-même.

### Le frontend reçoit une erreur CORS

Ajouter son origine exacte à `CORS_ALLOWED_ORIGINS`, séparée par une virgule des autres origines, puis redémarrer l’API.

### Plumo IA renvoie une erreur de configuration

- laisser `AI_PROVIDER=mock` pour travailler sans service externe ;
- avec `AI_PROVIDER=gemini`, renseigner `GEMINI_API_KEY` ;
- consulter les logs sans exposer la clé : `docker compose logs -f api`.

### La réinitialisation du mot de passe n’envoie aucun email

Le provider par défaut est `MAIL_PROVIDER=log` : le lien est écrit dans les logs. Pour un véritable email, utiliser `MAIL_PROVIDER=smtp` et configurer les variables `SMTP_*` ainsi que `MAIL_FROM`.

### Une couverture uploadée disparaît après recréation du conteneur

Vérifier que le volume `plumora_uploads` est monté sur `/app/uploads`. Les fichiers ne doivent pas être stockés uniquement dans la couche éphémère du conteneur.

## Documentation complémentaire

- [Contrat complet de l’API](docs/api-contract.md)
- [Module Administration](docs/admin.md)
- [Modèle de données](docs/data-model.md)
- [Tests smoke de l’API](docs/api-smoke-tests.md)
- [Plan de test Swagger](docs/app-swagger-test-plan.md)
- [Déploiement et exploitation en production](deploy/README.md)
- [Contexte produit](docs/project-context.md)
- [Décisions partagées frontend/backend](docs/shared-decisions.md)

## Contribuer

Avant de proposer une modification :

1. respecter l’architecture par domaine et par couche ;
2. utiliser des DTO pour toutes les entrées et sorties REST ;
3. valider les entrées et les règles métier dans la couche appropriée ;
4. vérifier les autorisations et la propriété des ressources dans les services ;
5. ajouter une migration Flyway pour toute évolution du schéma ;
6. ajouter ou adapter les tests ;
7. exécuter `./mvnw clean test` avant le push ;
8. mettre à jour `docs/api-contract.md` si le contrat HTTP change.
