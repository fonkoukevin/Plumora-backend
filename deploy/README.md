# Deploiement production Plumora (VPS OVHcloud Ubuntu)

Infrastructure Docker Compose **officielle et unique** pour la production : Caddy (reverse
proxy + HTTPS), backend Spring Boot, PostgreSQL, frontend Flutter Web. Le backend et le
frontend sont deployes a partir d'images pre-construites (`BACKEND_IMAGE`, `FRONTEND_IMAGE`)
publiees par leurs depots respectifs (ce depot pour le backend, un depot Flutter separe pour le
frontend) : ce dossier ne construit rien lui-meme et ne contient aucun code source applicatif.

Ce fichier est la documentation unique du deploiement production : premier deploiement, mises
a jour, sauvegardes, restauration, rollback, logs, diagnostics, variables d'environnement,
frontend, domaines, images Docker. Le `docker-compose.yml` a la racine du depot reste reserve au
developpement local (voir le [README principal](../README.md)) et n'est pas concerne par ce
document.

```
deploy/
├── compose.prod.yml       Stack Docker Compose de production
├── Caddyfile               Reverse proxy + HTTPS
├── .env.example             Gabarit de variables (aucune vraie valeur)
├── scripts/
│   ├── deploy.sh            Deploiement / mise a jour
│   ├── rollback.sh          Retour aux images precedentes
│   ├── backup-postgres.sh   Sauvegarde PostgreSQL
│   ├── restore-postgres.sh  Restauration PostgreSQL (manuelle, confirmee)
│   └── health-check.sh      Verification app.plumora.fr + api.plumora.fr
├── systemd/
│   ├── plumora-backup.service
│   └── plumora-backup.timer     Sauvegarde quotidienne automatique
└── ci-templates/           Gabarits pour le depot Flutter separe (voir section 17)

.github/workflows/          CI/CD de ce depot (backend) - voir section 17
├── backend-ci.yml           Tests + package, sur chaque PR et push
├── release.yml               Build + push de l'image backend vers GHCR (main/tags)
└── deploy.yml                 Deploiement SSH sur le VPS, apres succes de release.yml
```

## 1. Prerequis VPS

- Ubuntu avec Docker Engine et le plugin Docker Compose (`docker compose version` doit
  fonctionner).
- Pare-feu : seuls les ports **22** (SSH), **80** et **443** ouverts publiquement. PostgreSQL et
  le backend ne publient de toute facon aucun port (voir `compose.prod.yml`), mais le pare-feu du
  VPS doit rester la deuxieme ligne de defense.
- Un compte capable de faire `docker` (root, ou utilisateur dans le groupe `docker`).
- Acces en pull aux registres contenant `BACKEND_IMAGE` et `FRONTEND_IMAGE` (`docker login` au
  registre si les images sont privees, avant le premier `docker compose pull`).

## 2. Installation

```bash
sudo mkdir -p /opt/plumora
sudo chown "$USER" /opt/plumora
git clone <url-de-ce-depot> /opt/plumora
cd /opt/plumora/deploy
```

Le reste de ce document suppose que `deploy/` se trouve a cet emplacement
(`/opt/plumora/deploy`) ; adapter les chemins sinon, y compris dans les unites systemd
([`systemd/plumora-backup.service`](systemd/plumora-backup.service)).

## 3. DNS

Avant le premier lancement, creer des enregistrements DNS **A** (et **AAAA** si IPv6) pointant
vers l'IP du VPS pour les trois domaines :

- `app.plumora.fr` → Flutter Web
- `api.plumora.fr` → API Spring Boot
- `www.plumora.fr` → optionnel (redirige vers `app.plumora.fr` tant qu'aucun site vitrine dedie
  n'existe, voir [`Caddyfile`](Caddyfile))

Caddy ne pourra obtenir de certificat HTTPS que si le DNS pointe deja correctement vers le VPS
au moment du premier demarrage.

## 4. Frontend Flutter Web

Le service `frontend-web` de [`compose.prod.yml`](compose.prod.yml) fait tourner l'image
`FRONTEND_IMAGE`, et Caddy lui fait un `reverse_proxy` (`app.plumora.fr` → `frontend-web:8080`).
**Le code source de ce frontend n'est pas dans ce depot et n'est jamais recupere ni construit
ici** : ce depot ne fait que consommer une image deja publiee par le depot Flutter, dont le
contrat ci-dessous est **valide et definitif**.

**`FRONTEND_IMAGE` doit etre construite exclusivement depuis le depot Flutter** (`plumora_app`),
qui possede son propre `Dockerfile` et sa propre configuration Nginx, deja valides
independamment de ce depot backend. Ce depot backend ne construit, ne recree et ne maintient
aucun Dockerfile ni configuration Nginx pour le frontend — y compris a titre de gabarit : les
fichiers `deploy/ci-templates/frontend.Dockerfile` et `nginx.frontend.conf` qui existaient ici
avant que cette architecture soit finalisee cote Flutter ont ete supprimes, redondants avec la
solution reelle du depot Flutter.

### Contrat definitif de `FRONTEND_IMAGE`

1. **Port interne fixe : `8080`.** Non configurable via une variable d'environnement (il n'y a
   plus de `FRONTEND_PORT`) : `compose.prod.yml` et `Caddyfile` ciblent tous deux
   `frontend-web:8080` en dur, exactement comme `backend:8080`.
2. **Serveur statique integre** : sert directement les fichiers Flutter Web compiles
   (`flutter build web`), sans dependance externe.
3. **Fallback SPA integre.** Toute route inconnue du serveur de fichiers renvoie `index.html`
   (code 200), pas une erreur 404 — verifie en particulier pour un chargement direct ou un
   rafraichissement navigateur (F5) sur :
   - `/admin/users`
   - `/author/manuscripts/42`
   - `/books/12/read`

   Caddy ne fait qu'un `reverse_proxy` vers cette image : il ne sert aucun fichier lui-meme et
   n'implemente **volontairement pas** un second fallback SPA (voir [`Caddyfile`](Caddyfile)) —
   ce serait redondant avec un mecanisme deja garanti par l'image.
4. **`HEALTHCHECK` Docker fonctionnel deja integre a l'image.** `compose.prod.yml` ne declare
   donc aucun bloc `healthcheck:` pour `frontend-web` : Docker Compose lit directement l'etat de
   sante rapporte par l'image, et `caddy` en depend (`condition: service_healthy`) exactement
   comme il depend du healthcheck du `backend`.
5. **Utilisateur non-root** : s'execute en `nginx` uid 101 a l'interieur du conteneur.
6. **Aucune variable secrete au runtime.** `frontend-web` n'a aucun bloc `environment:` dans
   `compose.prod.yml` : il ne recoit jamais `JWT_SECRET`, `GEMINI_API_KEY`, un mot de passe
   PostgreSQL, ni quoi que ce soit d'autre. Toute configuration necessaire (URL de l'API, etc.)
   est deja integree dans l'image au moment de sa construction, par le pipeline de release du
   depot Flutter (propre a ce depot, non reproduit ici).
7. **En-tetes de securite deja configures dans l'image** (voir `Caddyfile`) : Caddy n'importe
   donc, pour `app.plumora.fr`, que les en-tetes de niveau transport qu'il est seul a pouvoir
   affirmer (HSTS, retrait de l'en-tete `Server`) et ne duplique pas les en-tetes de contenu
   (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`) que l'image fournit deja.

## 5. Images de production

**Image backend : `ghcr.io/<owner>/plumora-backend`** (`<owner>` = proprietaire GitHub du depot,
`${{ github.repository_owner }}` cote workflow). Publiee automatiquement par
[`release.yml`](../.github/workflows/release.yml) — voir section 17 pour le detail du processus
et des tags produits.

```
BACKEND_IMAGE=ghcr.io/compte/plumora-backend:v1.0.0-beta
FRONTEND_IMAGE=ghcr.io/compte/plumora-frontend:v1.0.0-beta
```

**Ne pas dependre uniquement du tag `:latest` en production.** `:latest` est pratique en
developpement mais rend impossible de savoir avec certitude quelle version tourne, et complique
le rollback (le tag change de sens a chaque publication). Preferer un tag immuable par version,
par exemple `v1.0.0` ci-dessus, ou directement un digest de contenu
(`ghcr.io/compte/plumora-backend@sha256:...`), pour que `BACKEND_IMAGE`/`FRONTEND_IMAGE` pointent
toujours exactement vers la meme image tant que `.env` n'est pas modifie explicitement.

`deploy.sh` et `rollback.sh` fonctionnent avec n'importe laquelle de ces formes (tag mobile,
tag de version, ou digest) : ils manipulent `BACKEND_IMAGE`/`FRONTEND_IMAGE` comme de simples
chaines, sans logique specifique a `:latest`. `rollback.sh` ne se fie d'ailleurs jamais au tag
en tant que tel : `deploy.sh` capture, avant chaque deploiement, le **digest reel** de l'image
actuellement en cours d'execution (`docker inspect --format='{{.Image}}'`) dans
`deploy/.env.last-deployed`, et c'est ce digest exact que `rollback.sh` relance — le rollback
fonctionne donc correctement meme si `BACKEND_IMAGE`/`FRONTEND_IMAGE` utilisent `:latest`.

Cela dit, un tag de version explicite reste fortement recommande : il permet de savoir, en
lisant simplement `.env`, quelle version est censee tourner, sans avoir a interroger Docker.

## 6. Creation du .env

```bash
cp .env.example .env
chmod 600 .env
nano .env   # ou tout autre editeur
```

`.env` n'est jamais commite (voir [`.gitignore`](../.gitignore) a la racine du depot). Voir la
section 15 pour la liste complete des variables et lesquelles sont obligatoires.

## 7. Premier lancement

```bash
cd /opt/plumora/deploy
docker compose --env-file .env -f compose.prod.yml config --quiet && echo "Configuration valide"
./scripts/deploy.sh
```

`deploy.sh` valide la configuration, demarre PostgreSQL, attend qu'il soit sain, demarre le
backend, attend son healthcheck (`/api/v1/actuator/health`), puis demarre le frontend et Caddy.
Au premier lancement il n'y a rien a sauvegarder ni aucune image precedente a enregistrer : ces
etapes s'affichent comme ignorees, c'est normal.

Verifier ensuite :

```bash
./scripts/health-check.sh
```

## 8. Mise a jour

Publier une nouvelle image (idealement sous un tag de version immuable, voir section 5), mettre
a jour `BACKEND_IMAGE`/`FRONTEND_IMAGE` dans `.env` si le tag a change, puis :

```bash
cd /opt/plumora/deploy
./scripts/deploy.sh
```

Le script sauvegarde PostgreSQL, enregistre les images actuellement en cours d'execution
(pour un rollback eventuel), recupere les nouvelles images et redemarre progressivement
(PostgreSQL → backend → frontend/Caddy), en s'arretant avec un code d'erreur si le backend ne
devient pas sain — sans jamais rien annuler automatiquement.

## 9. Sauvegarde

### PostgreSQL

```bash
./scripts/backup-postgres.sh
```

- `pg_dump` compresse, execute a l'interieur du conteneur `postgres` (aucun port n'est publie
  vers l'hote).
- Nom de fichier avec date/heure : `plumora-<db>-AAAA-MM-JJ_HH-MM-SS.sql.gz`.
- Ecrit dans `BACKUP_DIR` (par defaut `/var/backups/plumora/postgres`, **hors du depot git**,
  `chmod 700`, fichiers `chmod 600`).
- Retention configurable via `BACKUP_RETENTION_DAYS` dans `.env` (defaut 14 jours) : les fichiers
  plus vieux sont supprimes automatiquement a chaque execution.
- N'affiche jamais le mot de passe (la connexion locale au conteneur ne le requiert pas).
- Envoi optionnel vers S3 (desactive par defaut) : definir `BACKUP_S3_BUCKET` dans `.env` et
  avoir `awscli` configure sur le VPS (identifiants geres hors de ce depot).
- `deploy.sh` l'appelle automatiquement avant chaque mise a jour.

### Volume des uploads (couvertures de livres)

Le backend ecrit les couvertures de livres uploadees par les utilisateurs sur disque, dans le
dossier defini par `PLUMORA_UPLOAD_DIR` (`/app/uploads` en production — voir
[`LocalBookCoverStorage`](../src/main/java/com/plumora/api/book/infrastructure/LocalBookCoverStorage.java)).
Ces fichiers ne sont **pas** dans PostgreSQL (seule leur URL relative l'est) : sauvegarder
uniquement la base ne suffit pas a pouvoir tout restaurer. `compose.prod.yml` monte donc un
volume Docker nomme et persistant, `backend_uploads`, sur `/app/uploads` — les fichiers survivent
deja aux redemarrages et redeploiements du conteneur `backend`. Ce volume n'est **jamais**
uniquement dans le systeme de fichiers ephemere du conteneur.

Pour sauvegarder ce volume separement (par exemple avant une operation risquee, ou en plus de la
sauvegarde PostgreSQL quotidienne) :

```bash
docker run --rm \
  -v plumora_backend_uploads:/data:ro \
  -v /var/backups/plumora/uploads:/backup \
  alpine tar czf "/backup/uploads-$(date +%Y-%m-%d_%H-%M-%S).tar.gz" -C /data .
```

(Le nom exact du volume est prefixe par `COMPOSE_PROJECT_NAME`, donc `plumora_backend_uploads`
par defaut — verifier avec `docker volume ls` si `COMPOSE_PROJECT_NAME` a ete change.) Pour
restaurer, extraire l'archive dans le volume avec la meme technique en sens inverse
(`tar xzf ... -C /data`) pendant que le backend est arrete.

**Permissions utilisateur non-root :** le `Dockerfile` du backend cree `/app/uploads` et le
donne au proprietaire de l'utilisateur applicatif non-root (`plumora:plumora`) avant de creer le
point de montage, de sorte qu'un volume nomme fraichement cree en herite directement — le
backend (qui ne tourne jamais en root, voir le `Dockerfile` a la racine) peut y ecrire sans
ajustement manuel de permissions.

### Sauvegarde automatique quotidienne (systemd)

```bash
sudo cp systemd/plumora-backup.service systemd/plumora-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now plumora-backup.timer
systemctl list-timers plumora-backup.timer   # verifier la prochaine execution
```

Adapter au prealable `WorkingDirectory`/`ExecStart` dans
[`systemd/plumora-backup.service`](systemd/plumora-backup.service) si le depot n'est pas a
`/opt/plumora`. Ce timer ne couvre que PostgreSQL (`backup-postgres.sh`) ; adapter/dupliquer si
une sauvegarde automatique reguliere du volume uploads est egalement souhaitee.

## 10. Restauration

```bash
./scripts/restore-postgres.sh /var/backups/plumora/postgres/plumora-plumora-2026-07-19_03-00-00.sql.gz
```

- Exige le chemin du fichier en argument et verifie son existence.
- **Remplace integralement** le contenu actuel de la base par celui de la sauvegarde : toute
  donnee ecrite depuis cette sauvegarde est perdue.
- Demande une confirmation explicite (taper `RESTORE`).
- Ne s'execute jamais automatiquement (aucun appel depuis `deploy.sh`, `rollback.sh`, ou un
  timer systemd) : reserve a une intervention manuelle en cas d'incident reel.

## 11. Rollback

```bash
./scripts/rollback.sh
```

Sans argument, relit `deploy/.env.last-deployed` (ecrit automatiquement par `deploy.sh` avant
chaque deploiement) pour revenir aux images backend/frontend precedemment en cours d'execution.
Des images explicites peuvent aussi etre passees :

```bash
./scripts/rollback.sh ghcr.io/compte/plumora-backend:v0.9.0 ghcr.io/compte/plumora-frontend:v0.9.0
```

Demande une confirmation explicite (taper `ROLLBACK`).

**Limite importante, volontaire : ce rollback ne touche jamais a PostgreSQL.** Il ne fait
revenir en arriere que le code applicatif (images des conteneurs). Si le deploiement annule
avait introduit une migration Flyway incompatible avec le code precedent, ce rollback seul ne
suffit pas : le code plus ancien peut se retrouver face a un schema qu'il ne reconnait pas.
Aucun rollback de migration destructive n'est tente automatiquement — la seule option sure dans
ce cas est une restauration de sauvegarde PostgreSQL anterieure a la migration en cause
(`./scripts/restore-postgres.sh`), avec la perte de donnees que cela implique entre cette
sauvegarde et l'incident (voir section 12).

## 12. Migrations Flyway

- Les migrations (`src/main/resources/db/migration/V*.sql` dans le depot backend) s'appliquent
  automatiquement au demarrage du conteneur `backend`, quel que soit l'environnement
  (`spring.flyway.enabled: true`). Hibernate est en `ddl-auto: validate` : le schema n'est jamais
  genere/modifie par JPA, uniquement par Flyway.
- **Ne jamais modifier une migration deja appliquee en production.** Flyway detecterait un
  changement de checksum et refuserait de demarrer — le backend resterait "unhealthy" jusqu'a
  correction. Toujours ajouter une nouvelle migration `V<N+1>__description.sql`, meme pour
  corriger une precedente.
- Si le SQL d'une migration echoue, le demarrage du backend echoue aussi : Flyway ne laisse
  jamais l'application demarrer sur un schema partiellement migre. C'est le comportement voulu.
- **Avant tout deploiement contenant une nouvelle migration, prendre une sauvegarde** —
  `./scripts/backup-postgres.sh` (`deploy.sh` le fait deja automatiquement avant chaque mise a
  jour, voir section 8). Flyway n'a pas de "down migration" automatique dans ce projet : revenir
  en arriere apres une migration problematique passe soit par une nouvelle migration corrective,
  soit par la restauration de cette sauvegarde (section 10) — jamais par une modification ou
  suppression de la migration en cause.

## 13. Logs

```bash
docker compose --env-file .env -f compose.prod.yml logs -f backend
docker compose --env-file .env -f compose.prod.yml logs -f postgres
docker compose --env-file .env -f compose.prod.yml logs -f frontend-web
docker compose --env-file .env -f compose.prod.yml logs -f caddy
```

En profil `prod`, les logs applicatifs du backend sont volontairement sobres (niveau `INFO`, SQL
Hibernate en `WARN`) et ne contiennent jamais de secrets ni de stacktrace cote reponse HTTP.

Les logs d'acces de Caddy (par domaine) sont en plus persistes dans le volume `caddy_data` :

```bash
docker compose --env-file .env -f compose.prod.yml exec caddy sh -c 'tail -f /data/logs/*.log'
```

## 14. Diagnostics

```bash
# Etat et sante de chaque conteneur
docker compose --env-file .env -f compose.prod.yml ps

# Verification complete (conteneurs + app.plumora.fr + api.plumora.fr/actuator/health)
./scripts/health-check.sh

# Le backend applique les migrations Flyway au demarrage : en cas d'echec, l'application ne
# demarre pas et le healthcheck backend reste "starting"/"unhealthy" - regarder ses logs en
# premier :
docker compose --env-file .env -f compose.prod.yml logs backend | tail -100

# Confirmer que PostgreSQL et le backend ne sont bien joignables que depuis l'interieur du
# reseau Docker (ne doit rien retourner depuis l'hote) :
curl -m 2 http://localhost:5432 ; curl -m 2 http://localhost:8080
```

## 15. Securite

- `.env` ne doit jamais etre commite, ni copie ailleurs que sur le VPS. Permissions
  recommandees : `chmod 600 .env` (fait a la section 6).
- **PostgreSQL et le backend ne publient volontairement aucun port sur l'hote** (voir
  `compose.prod.yml`) : toute tentative de les rendre accessibles depuis l'exterieur (ajout d'un
  bloc `ports:` sur l'un de ces deux services) doit etre consideree comme une regression de
  securite. Seul `caddy` publie 80 et 443.
- Aucun pgAdmin ni autre interface d'administration de base de donnees dans ce stack. Pour
  inspecter la base en production, utiliser :
  `docker compose --env-file .env -f compose.prod.yml exec postgres psql -U $POSTGRES_USER -d $POSTGRES_DB`
  plutot que d'exposer un port ou d'ajouter une interface web.
- Swagger/OpenAPI (`/api/v1/swagger-ui.html`, `/api/v1/api-docs`) est desactive par le profil
  `prod` du backend (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).
- Seuls les endpoints Actuator `health` et `info` sont exposes par le backend
  (`management.endpoints.web.exposure.include`) et accessibles sans authentification ; aucun
  autre endpoint Actuator (`env`, `beans`, `heapdump`, etc.) n'est accessible. `/api/v1/actuator/health`
  ne renvoie que le statut global (`UP`/`DOWN`), jamais de details d'infrastructure
  (`management.endpoint.health.show-details: never`).
- Le compte de demonstration (`admin@plumora.local`) ne peut jamais s'activer en production : il
  n'est cree que si le profil Spring `dev` est actif, et `SPRING_PROFILES_ACTIVE` doit toujours
  valoir exactement `prod` dans `.env` (jamais `dev,prod`). Le backend refuse d'ailleurs de
  demarrer si les deux profils sont actifs en meme temps (voir `ProductionEnvironmentValidator`
  dans le depot backend).
- Le backend refuse egalement de demarrer en profil `prod` si `JWT_SECRET` ou
  `SPRING_DATASOURCE_PASSWORD` sont absents, trop faibles, ou egaux a une valeur de
  developpement connue, et si `AI_PROVIDER=gemini` sans `GEMINI_API_KEY` — defense en profondeur
  independante de la validation `docker compose config` (section 16).
- `CORS_ALLOWED_ORIGINS` doit rester limite aux domaines reellement utilises par le frontend
  (`https://app.plumora.fr`, eventuellement `https://www.plumora.fr`) : ne pas y ajouter de
  wildcard large.
- Cles SSH, certificats prives et sauvegardes ne doivent jamais se retrouver dans Git (voir
  [`.gitignore`](../.gitignore) a la racine).

## 16. Validation effectuee (dans ce depot)

```bash
docker compose --env-file .env.example -f compose.prod.yml config --quiet
```

→ voir le message de fin de session pour le resultat exact obtenu sur ce depot (healthcheck
officiel : `/api/v1/actuator/health`, aucun `ports:` sur `backend`/`postgres`, seul `caddy`
publie 80/443, volumes `postgres_data`/`backend_uploads`/`caddy_data`/`caddy_config` presents).

## 17. CI/CD (GitHub Actions)

Trois workflows dans [`.github/workflows/`](../.github/workflows/) de ce depot :

| Workflow | Declenchement | Role | Secrets utilises |
| --- | --- | --- | --- |
| `backend-ci.yml` | Chaque pull request, chaque push | Tests, packaging, build Docker + smoke test compose | Aucun |
| `release.yml` | Push sur `main`, tag `v*.*.*` | Rejoue les tests, construit et publie l'image backend sur GHCR | `GHCR_TOKEN` (optionnel) |
| `deploy.yml` | Automatique apres succes de `release.yml` sur `main`, ou manuel (`workflow_dispatch`) | Connexion SSH au VPS, mise a jour des references d'image, `./scripts/deploy.sh`, verification du healthcheck | `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_PORT` (optionnel) |

Le frontend Flutter a son propre pipeline dans son propre depot, deja valide et operationnel
(Dockerfile, configuration Nginx et publication de `FRONTEND_IMAGE` propres a ce depot). Voir
[`ci-templates/`](ci-templates/) pour un gabarit optionnel de verification (`flutter test`) sur
pull request/push (`frontend-ci.yml`, `frontend-release.yml`) — ce depot backend ne recupere, ne
construit et ne publie jamais lui-meme le code ou l'image Flutter (voir section 4).

### Secrets GitHub requis

A configurer dans **Settings → Secrets and variables → Actions** de ce depot :

| Secret | Obligatoire | Role |
| --- | --- | --- |
| `VPS_HOST` | oui | Adresse (IP ou nom DNS) du VPS |
| `VPS_USER` | oui | Utilisateur SSH sur le VPS (capable de `docker`, voir section 1) |
| `VPS_SSH_KEY` | oui | Cle privee SSH dediee au deploiement (voir ci-dessous), format OpenSSH, jamais protegee par une passphrase (une passphrase ne peut pas etre saisie dans un pipeline non interactif) |
| `VPS_PORT` | non (defaut `22`) | Port SSH si different du defaut |
| `GHCR_TOKEN` | non | PAT avec le scope `write:packages` (et `delete:packages` si suppression manuelle de versions souhaitee). Necessaire uniquement si le `GITHUB_TOKEN` automatique ne suffit pas pour publier le package (organisation differente du depot, gestion manuelle) — sinon `release.yml` retombe automatiquement sur `GITHUB_TOKEN` |

Aucun de ces secrets n'est un secret **applicatif** : `DATABASE_PASSWORD`/`POSTGRES_PASSWORD`,
`JWT_SECRET` et `GEMINI_API_KEY` ne sont jamais lus ni transmis par ces workflows. Ils restent
exclusivement dans `.env` sur le VPS (section 6), edite une seule fois manuellement — `deploy.yml`
ne modifie que les lignes `BACKEND_IMAGE`/`FRONTEND_IMAGE` de ce fichier, jamais les autres.

### Creation de la cle de deploiement

Sur votre poste (pas sur le VPS), generer une paire de cles dediee (ne pas reutiliser une cle
personnelle) :

```bash
ssh-keygen -t ed25519 -C "plumora-deploy" -f plumora_deploy_key -N ""
```

Puis :

1. Copier **la cle publique** (`plumora_deploy_key.pub`) dans `~/.ssh/authorized_keys` de
   l'utilisateur `VPS_USER` sur le VPS (`ssh-copy-id -i plumora_deploy_key.pub
   VPS_USER@VPS_HOST`, ou copier/coller manuellement).
2. Coller **le contenu de la cle privee** (`plumora_deploy_key`, en entier, y compris les lignes
   `-----BEGIN...-----`/`-----END...-----`) dans le secret GitHub `VPS_SSH_KEY`.
3. Supprimer la copie locale de la cle privee une fois le secret enregistre
   (`rm plumora_deploy_key`) ; seule la cle publique (`.pub`) peut rester si besoin de la
   redeployer ailleurs.
4. Idealement, restreindre cote VPS ce que cette cle peut faire (utilisateur dedie sans `sudo`,
   uniquement membre du groupe `docker` — pas root).

### Fonctionnement des tags

Voir aussi section 5. `release.yml` (et son equivalent frontend) publient toujours au moins :

- `sha-<7-caracteres>` — a chaque execution (push sur `main` ou tag), l'unique reference
  **immuable** garantie ; c'est celle que `deploy.yml` utilise par defaut pour le declenchement
  automatique.
- `<tag Git exact>` — uniquement quand le workflow est declenche par un tag de version
  (`git tag v1.0.0-beta && git push --tags` → tag Docker **`v1.0.0-beta`**, prefixe `v` et
  suffixe de pre-version preserves tels quels, jamais normalises en `1.0.0-beta`). Le filtre de
  declenchement (`on.push.tags` dans `release.yml`) accepte tout tag `vX.Y.Z` avec un suffixe
  optionnel (`-beta`, `-rc1`, etc.).
- `latest` — uniquement sur un push vers `main` (jamais sur un tag) : pointeur pratique vers le
  plus recent `main`, jamais la seule facon de reference une image (voir section 5).

**En production, `BACKEND_IMAGE`/`FRONTEND_IMAGE` doivent toujours utiliser un tag de version ou
un digest — jamais `latest` seul** (voir section 5) : `deploy.sh` refuse de deployer sans mettre
a jour manuellement un tag mobile qui n'aurait pas change, ce qui rendrait un rollback ambigu.

### Rollback vers un ancien tag ou digest

Trois methodes, du plus simple au plus explicite :

1. **Automatique** (recommande pour annuler le tout dernier deploiement) : `./scripts/rollback.sh`
   sur le VPS (ou via `workflow_dispatch` sur `deploy.yml` sans rien renseigner) relit
   `deploy/.env.last-deployed`, ecrit automatiquement par `deploy.sh` avant chaque deploiement,
   et revient exactement a l'image precedente par son **digest** (voir section 11).
2. **Explicite vers un SHA de commit precis** (au-dela du seul precedent) : les images sont
   conservees sur GHCR sous leur tag `sha-<7-caracteres>` correspondant a chaque commit publie.
   Retrouver le SHA voulu (`git log --oneline`, ou l'onglet Actions de `release.yml`), puis :
   ```bash
   ./scripts/rollback.sh ghcr.io/<owner>/plumora-backend:sha-<7-caracteres> ghcr.io/<owner>/plumora-frontend:sha-<7-caracteres>
   ```
3. **Explicite vers un tag de version ou un digest** : de la meme facon, en utilisant directement
   le tag de version publie (`git tag`/onglet *Releases*) ou un digest de contenu
   (visible dans le resume de `release.yml` ou via `docker inspect --format='{{.RepoDigests}}'`) :
   ```bash
   ./scripts/rollback.sh ghcr.io/<owner>/plumora-backend:v0.9.0 ghcr.io/<owner>/plumora-frontend:v0.9.0
   # ou, par digest :
   ./scripts/rollback.sh ghcr.io/<owner>/plumora-backend@sha256:<digest> ghcr.io/<owner>/plumora-frontend@sha256:<digest>
   ```
   Dans les trois cas, cela peut aussi se faire depuis GitHub en declenchant `deploy.yml`
   manuellement (`workflow_dispatch`) avec cette meme reference en entree
   `backend_image`/`frontend_image`.

### Procedure de deploiement manuel de secours

Si GitHub Actions est indisponible, ou pour un controle total :

```bash
ssh VPS_USER@VPS_HOST
cd /opt/plumora/deploy
nano .env   # ajuster BACKEND_IMAGE/FRONTEND_IMAGE si besoin
./scripts/deploy.sh
./scripts/health-check.sh
```

C'est exactement ce que `deploy.yml` execute a distance : aucune etape "cachee", la CI/CD
n'est qu'une automatisation de cette meme procedure manuelle, jamais un chemin different.

### Notes de securite specifiques a ces workflows

- `backend-ci.yml` s'execute sur les pull requests **sans aucun secret** (`permissions:
  contents: read` uniquement) : une pull request externe ne peut donc jamais en extraire quoi
  que ce soit de sensible, et ne peut jamais declencher `release.yml`/`deploy.yml` (tous deux
  restreints a `push` sur `main`/tags, jamais a `pull_request`).
- `deploy.yml` declare `environment: production` : creer cet environnement dans **Settings →
  Environments** avec des "Required reviewers" pour transformer cette ligne en validation
  manuelle effective avant tout deploiement — sans cette configuration, la ligne seule ne
  bloque rien.
- La cle privee SSH n'apparait jamais dans une ligne de commande (ecrite dans un fichier
  `chmod 600`, referencee uniquement via `-i <fichier>`) ; GitHub masque de toute facon
  automatiquement la valeur de tout secret qui apparaitrait dans un log.
- Toutes les actions tierces sont epinglees a une version majeure precise
  (`actions/checkout@v4`, `docker/build-push-action@v6`, etc.) ; pour un niveau de securite
  supply-chain superieur, elles peuvent etre repinglees a un SHA de commit exact.

## Variables obligatoires (sans valeur par defaut)

`BACKEND_IMAGE`, `FRONTEND_IMAGE`, `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`,
`CORS_ALLOWED_ORIGINS`, `APP_DOMAIN`, `API_DOMAIN`, `CADDY_ACME_EMAIL`. `docker compose config
--quiet` echoue explicitement (nom de la variable manquante, jamais sa valeur) si l'une d'elles
est absente de `.env`.

Le reste (`COMPOSE_PROJECT_NAME`, `POSTGRES_DB`, `POSTGRES_USER`,
`SPRING_PROFILES_ACTIVE`, `JWT_EXPIRATION`, `AI_PROVIDER`, `GEMINI_*`, `WWW_DOMAIN`,
`BACKUP_RETENTION_DAYS`, `BACKUP_DIR`, `BACKUP_S3_BUCKET`) a une valeur par defaut raisonnable si
absent de `.env` — voir [`.env.example`](.env.example) pour le detail.

## Premier deploiement : commandes exactes

```bash
sudo mkdir -p /opt/plumora && sudo chown "$USER" /opt/plumora
git clone <url-de-ce-depot> /opt/plumora
cd /opt/plumora/deploy
cp .env.example .env && chmod 600 .env
nano .env   # remplir les valeurs (voir section suivante)
docker compose --env-file .env -f compose.prod.yml config --quiet && echo OK
./scripts/deploy.sh
./scripts/health-check.sh
```

## Valeurs a remplir manuellement dans .env

- `BACKEND_IMAGE`, `FRONTEND_IMAGE` — references reelles des images publiees (registre + tag).
  Preferer un tag de version immuable a `:latest` (section 5).
- `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` — meme mot de passe fort et unique (le
  backend et PostgreSQL doivent s'accorder ; ne jamais reutiliser une valeur de developpement).
- `JWT_SECRET` — aleatoire, au moins 32 caracteres (ex. `openssl rand -base64 48`).
- `GEMINI_API_KEY` — uniquement si `AI_PROVIDER=gemini` ; laisser `AI_PROVIDER=mock` sinon (aucune
  cle requise dans ce cas).
- `CORS_ALLOWED_ORIGINS`, `APP_DOMAIN`, `API_DOMAIN`, `WWW_DOMAIN` — confirmer qu'ils
  correspondent exactement aux domaines DNS reellement pointes vers ce VPS.
- `CADDY_ACME_EMAIL` — adresse email reelle pour les notifications Let's Encrypt.
