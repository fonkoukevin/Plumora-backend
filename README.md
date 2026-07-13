# Plumora API

Backend Spring Boot (Java 21) de Plumora : ecriture collaborative, beta-lecture, catalogue
public-domain (Gutendex/Open Library), recommandations et assistance a l'ecriture par IA (Plumo
IA), et administration de la plateforme.

## Prerequis

- Docker et Docker Compose (aucun JDK local requis, tout tourne en conteneur).

## Demarrer l'API en local

```bash
docker compose up -d --build
```

Cela demarre trois services :

- `plumora-postgres` — PostgreSQL 17 (port `5432`).
- `plumora-api` — l'API Spring Boot (port `8080`, prefixe `/api/v1`), qui applique automatiquement
  les migrations Flyway au demarrage.
- `plumora-pgadmin` — interface web pgAdmin (port `5050`) pour inspecter la base.

La documentation OpenAPI est servie sur `/api/v1/swagger-ui.html` une fois l'API demarree.

### Variables d'environnement principales

| Variable | Role | Defaut local |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Profils Spring actifs (ex. `dev` pour le seed admin) | vide |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base de donnees | `plumora_db` / `plumora` / `plumora` |
| `JWT_SECRET` / `JWT_EXPIRATION` | Signature et duree de vie des tokens JWT | valeur de dev fournie |
| `AI_PROVIDER` | `mock` ou `gemini` | `mock` |
| `GEMINI_API_KEY` / `GEMINI_MODEL` | Cle et modele Gemini quand `AI_PROVIDER=gemini` | vide / `gemini-flash-lite-latest` |
| `PLUMORA_UPLOAD_DIR` | Dossier de stockage des uploads (couvertures, etc.) | `/app/uploads` |

## Lancer les tests

```bash
docker run --rm -v "$(pwd -W)":/app -w /app -v plumora_maven_repo:/root/.m2 \
  maven:3.9-eclipse-temurin-21 mvn -o test -q
```

(`plumora_maven_repo` est un volume Docker qui met en cache les dependances Maven entre les
executions ; il est cree automatiquement au premier lancement.)

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

## Documentation

- [`docs/api-contract.md`](docs/api-contract.md) — liste de toutes les routes de l'API.
- [`docs/admin.md`](docs/admin.md) — module Administration en detail.
