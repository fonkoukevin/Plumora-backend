# Templates CI/CD pour le depot Flutter (plumora_app)

Ces fichiers **ne s'executent pas dans ce depot** (`plumora-api`) et ne sont pas dans
`.github/workflows/` pour cette raison precise : le code source Flutter n'est pas ici (voir
`deploy/README.md`, section "Frontend Flutter Web") et ce depot ne le recupere ni ne le
construit jamais. Ce sont des gabarits optionnels, prets a copier dans le depot Flutter separe
(`plumora_app`) si utiles, pour la partie tests uniquement.

**L'image `FRONTEND_IMAGE` elle-meme est construite exclusivement par le depot Flutter**, qui
possede deja son propre `Dockerfile` et sa propre configuration Nginx, valides independamment de
ce depot (port interne 8080, utilisateur non-root `nginx` uid 101, healthcheck Docker integre,
fallback SPA, en-tetes de securite — voir `deploy/README.md`, "Frontend Flutter Web"). Ce depot
backend ne construit, ne recree et ne maintient aucun Dockerfile ni configuration Nginx pour le
frontend : les gabarits `frontend.Dockerfile`/`nginx.frontend.conf` qui existaient ici avant que
cette architecture soit finalisee cote Flutter ont ete supprimes (devenus redondants avec la
solution reelle et validee du depot Flutter).

## Fichiers

| Fichier | A copier vers (dans le depot Flutter) | Role |
| --- | --- | --- |
| `frontend-ci.yml` | `.github/workflows/frontend-ci.yml` | Format, analyse, tests, build web de validation sur chaque PR/push |
| `frontend-release.yml` | `.github/workflows/frontend-release.yml` | Rejoue les tests sur `main`/tag — ne construit ni ne publie aucune image (voir son en-tete) |

## A adapter avant utilisation

- **Version Flutter** : `frontend-ci.yml`/`frontend-release.yml` utilisent `channel: stable` par
  defaut (`subosito/flutter-action`). Remplacer par `flutter-version: 'x.y.z'` des que la version
  exacte utilisee par le projet est figee, pour des builds reproductibles.
- **`--dart-define`** : `frontend-ci.yml` construit un build de validation avec
  `--dart-define=API_BASE_URL=https://staging-api.plumora.fr`. Le nom exact de la cle
  (`API_BASE_URL`) doit correspondre a celle reellement lue par le code Dart. La construction de
  l'image de production elle-meme (avec la vraie valeur `--dart-define` cote production) releve
  du pipeline reel du depot Flutter, pas de ce gabarit.
- **Secrets** : ces workflows n'ont besoin d'aucun secret applicatif (aucune cle API cote
  frontend, l'IA passe uniquement par le backend). `frontend-release.yml` ne publiant plus
  d'image ici, aucun secret de registre (`GHCR_TOKEN` ou equivalent) n'est necessaire dans ce
  gabarit — le pipeline reel de publication du depot Flutter gere cela de son cote.
