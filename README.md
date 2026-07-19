# Plumora API

Backend Spring Boot (Java 21) de Plumora : ecriture collaborative, beta-lecture, catalogue
public-domain (Gutendex/Open Library), recommandations et assistance a l'ecriture par IA (Plumo
IA), et administration de la plateforme.

## Prerequis

- Docker et Docker Compose (aucun JDK local requis, tout tourne en conteneur).

## Demarrer l'API en local

Copier `.env.example` en `.env` (ignore par git) pour surcharger les variables locales si besoin
(activer Gemini, changer le profil Spring, etc.), puis :

```bash
docker compose up -d --build
```

Cela demarre trois services :

- `plumora-postgres` — PostgreSQL 17 (port `5432`).
- `plumora-api` — l'API Spring Boot (port `8080`, prefixe `/api/v1`), qui applique automatiquement
  les migrations Flyway au demarrage.
- `plumora-pgadmin` — interface web pgAdmin (port `5050`) pour inspecter la base.

La documentation OpenAPI est servie sur `/api/v1/swagger-ui.html` une fois l'API demarree.

`docker compose up -d --build` reconstruit l'image `api` si besoin. Pour la construire seule
(sans lancer le stack), par exemple pour l'inspecter ou la pousser vers un registre :

```bash
docker build -t plumora-api:local .
```

Le `Dockerfile` est multi-stage (compilation Maven puis image d'execution
`eclipse-temurin:21-jre-alpine`), tourne en utilisateur non-root, n'embarque aucun secret et
expose un `HEALTHCHECK` sur `/api/v1/actuator/health`.

### Variables d'environnement principales

| Variable | Role | Defaut local |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Profils Spring actifs (ex. `dev` pour le seed admin) | vide |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base de donnees | `plumora_db` / `plumora` / `plumora` |
| `JWT_SECRET` / `JWT_EXPIRATION` | Signature et duree de vie des tokens JWT | valeur de dev fournie |
| `AI_PROVIDER` | `mock` ou `gemini` | `mock` |
| `GEMINI_API_KEY` / `GEMINI_MODEL` | Cle et modele Gemini quand `AI_PROVIDER=gemini` | vide / `gemini-flash-lite-latest` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisees a appeler l'API, separees par des virgules | `http://localhost:*,http://127.0.0.1:*` |
| `PLUMORA_UPLOAD_DIR` | Dossier de stockage des uploads (couvertures, etc.) | `/app/uploads` |

Ces variables ont une valeur par defaut utilisable telle quelle en local (`application.yml`).
Deux autres profils Spring existent :

- `application-dev.yml` (`SPRING_PROFILES_ACTIVE=dev`) : logs plus verbeux, detail complet de
  `/actuator/health`, active le seed du compte admin de demonstration (voir plus bas).
- `application-prod.yml` (`SPRING_PROFILES_ACTIVE=prod`) : n'accepte plus aucune valeur par
  defaut pour les secrets, desactive Swagger, restreint les logs et les reponses d'erreur. Voir
  [`deploy/README.md`](deploy/README.md).

## Lancer les tests

```bash
docker run --rm -v "$(pwd -W)":/app -w /app -v plumora_maven_repo:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  maven:3.9-eclipse-temurin-21 mvn -o test -q
```

(`plumora_maven_repo` est un volume Docker qui met en cache les dependances Maven entre les
executions ; il est cree automatiquement au premier lancement.)

La majorite des tests sont des tests unitaires/tranche (mocks, aucune infrastructure requise).
Quelques tests d'integration (`FlywayMigrationIntegrationTest`, `ProductionProfileStartupIntegrationTest`)
demarrent un vrai PostgreSQL jetable via Testcontainers pour verifier que les migrations Flyway
s'appliquent reellement et que le contexte Spring demarre en profil `prod` : c'est pourquoi la
commande ci-dessus monte le socket Docker (`/var/run/docker.sock`). Ces tests n'appellent jamais
la vraie API Gemini (`AI_PROVIDER=mock` force par `application-test.yml`).

## Verifier le healthcheck

Une fois l'API demarree (`docker compose up -d`) :

```bash
curl http://localhost:8080/api/v1/actuator/health
```

Reponse attendue : `{"status":"UP"}`. Cet endpoint verifie a la fois l'application et la
connexion PostgreSQL (si la base est injoignable, Spring Boot Actuator renvoie `DOWN` avec un
code HTTP 503) ; il n'expose aucun autre detail (`management.endpoint.health.show-details:
never`). Seuls `health` et `info` sont exposes parmi les endpoints Actuator, et les deux sont
accessibles sans authentification (`SecurityConfig`) — le reste de l'API reste protege par JWT.
Note : le prefixe `/api/v1` s'applique aussi a Actuator puisqu'il partage le meme port que
l'API ; `/actuator/health` seul (sans le prefixe) renvoie 404.

## Module Administration

Un compte `ADMIN` est necessaire pour utiliser les routes `/admin/**` (tableau de bord, gestion
des utilisateurs et du catalogue, signalements, supervision de Plumo IA). Voir
[`docs/admin.md`](docs/admin.md) pour le detail des routes et du comportement.

Pour obtenir un compte admin en local, deux options :

1. **Compte de demonstration seed automatiquement** — demarrer l'API avec le profil `dev` :
   ```bash
   SPRING_PROFILES_ACTIVE=dev docker compose up -d --build
   ```
   Cela cree `admin@plumora.local` / `Admin123!` (role `ADMIN`) au demarrage, uniquement si ce
   compte n'existe pas deja. Ce seed ne s'active jamais sans le profil `dev` explicite : il ne
   tourne donc jamais accidentellement en production.
2. **Promouvoir un compte existant** — s'inscrire normalement via `POST /auth/register` puis
   attribuer le role `ADMIN` en base (`INSERT INTO user_roles ...`) ou via un premier admin deja
   promu, en utilisant `PATCH /admin/users/{userId}/role`.

## Migrations Flyway

Toute evolution du schema passe par une nouvelle migration dans
[`src/main/resources/db/migration`](src/main/resources/db/migration), nommee
`V<N+1>__description.sql` (N = derniere version existante). Elles s'appliquent automatiquement
au demarrage de l'API (`spring.flyway.enabled: true`, tous profils confondus) et Hibernate est en
`ddl-auto: validate` : le schema n'est jamais genere/modifie par JPA, uniquement par Flyway.

**Regles a respecter :**

- Ne jamais modifier une migration deja appliquee quelque part (dev, staging ou production) :
  Flyway detecterait un checksum different et refuserait de demarrer. Toujours ajouter une
  nouvelle migration, meme pour corriger une precedente.
- Si le SQL d'une migration echoue, le demarrage de l'application echoue aussi (Flyway ne laisse
  pas l'application demarrer sur un schema partiellement migre) : c'est le comportement voulu,
  aucune tentative de continuer avec un schema incoherent.
- Avant d'appliquer une migration en production, toujours prendre une sauvegarde de la base : la
  procedure complete (sauvegarde, restauration, rollback applicatif et ses limites vis-a-vis de
  Flyway) est documentee dans [`deploy/README.md`](deploy/README.md), qui est la seule
  documentation de deploiement/exploitation en production de ce depot (voir section suivante).

## Production

Ce depot ne construit et ne lance lui-meme que l'image backend ; il ne contient aucune
infrastructure de production. Le `docker-compose.yml` a la racine est reserve au developpement
local (voir plus haut) — ne jamais l'utiliser sur le VPS.

**Toute l'infrastructure de production officielle (Docker Compose, Caddy, variables
d'environnement, sauvegardes, restauration, rollback, logs, diagnostics, frontend Flutter Web,
domaines, images Docker) se trouve dans [`deploy/`](deploy/), documentee integralement dans
[`deploy/README.md`](deploy/README.md).**

## Documentation

- [`docs/api-contract.md`](docs/api-contract.md) — liste de toutes les routes de l'API.
- [`docs/admin.md`](docs/admin.md) — module Administration en detail.
- [`deploy/README.md`](deploy/README.md) — deploiement et exploitation en production (VPS).
