# Roadmap, sprints et planning — Plumora

Cette roadmap formalise les itérations du projet à partir du périmètre réel et des traces Git disponibles.

## Sprint 1 — Socle & authentification

**Objectif** : disposer d’un socle applicatif sécurisé permettant l’inscription, la connexion et la gestion des rôles.

Périmètre :
- structure Spring Boot / Flutter ;
- PostgreSQL et migrations ;
- authentification JWT ;
- inscription / connexion ;
- rôles Auteur, Lecteur, Bêta-lecteur, Administrateur.

Preuves Git :
- Backend : `058a025` — `feature auth` — 2026-05-21 ;
- Frontend : `5d0cc7c` — socle Flutter — 2026-05-21 ;
- Frontend : `a18a20d` — authentification JWT — 2026-05-21.

**Critère de sortie** : un utilisateur peut créer un compte, se connecter et accéder à l’espace correspondant à son rôle.

## Sprint 2 — Auteur & écriture

**Objectif** : permettre à un auteur de créer et structurer un manuscrit.

Périmètre :
- création / modification d’un livre ;
- chapitres ;
- éditeur ;
- espace auteur ;
- publication.

Preuve Git principale :
- Frontend : `87c66b5` — `feature book` — 2026-05-22.

**Critère de sortie** : l’auteur peut créer un livre, gérer ses chapitres et préparer son contenu à la publication.

## Sprint 3 — Lecture & catalogue

**Objectif** : proposer un parcours lecteur complet.

Périmètre :
- catalogue ;
- recherche ;
- fiche livre ;
- lecture des chapitres ;
- progression ;
- favoris ;
- avis ;
- domaine public.

Preuves Git :
- Frontend : `eae68df` — lecture, favoris, avis, catalogue — 2026-05-23 ;
- Frontend : `3e6f861` — catalogue Gutendex / domaine public — 2026-07-05 ;
- Backend : `0530d2b` — fallback Open Library — 2026-07-21 ;
- Backend : `d2127c6` — catalogue Gutenberg local — 2026-07-21.

**Critère de sortie** : un lecteur peut découvrir, ouvrir et lire une œuvre avec une progression persistée.

## Sprint 4 — Bêta-lecture & collaboration

**Objectif** : organiser la relecture d’un manuscrit avant publication.

Périmètre :
- campagnes de bêta-lecture ;
- chapitres partagés ;
- invitations ;
- commentaires structurés ;
- suivi des chapitres lus ;
- notifications.

Preuves Git :
- Backend : `3dda0ba` — `feature beta-lecteur` — 2026-05-21 ;
- Frontend : `eae68df` — module bêta-lecture de bout en bout — 2026-05-23 ;
- Backend : `6730051` — ouverture des campagnes — 2026-07-09 ;
- Backend : `23e15f6` — transitions d’état et suivi des chapitres — 2026-07-12.

**Critère de sortie** : un auteur peut lancer une campagne et un bêta-lecteur peut consulter les chapitres et transmettre des retours.

## Sprint 5 — Plumo IA & enrichissement

**Objectif** : enrichir l’écriture et la découverte avec Plumo.

Périmètre :
- reformulation ;
- résumé ;
- continuation ;
- suggestions de titres ;
- pré-analyse avant bêta-lecture ;
- recommandations ;
- provider Gemini.

Preuves Git :
- Frontend : `eae68df` — assistant IA et recommandations — 2026-05-23 ;
- Backend : `44db70d` — module Plumo IA / Gemini — 2026-07-12 ;
- Backend : `24ab6a1` — correction du modèle Gemini — 2026-07-12.

**Critère de sortie** : les fonctionnalités IA prévues répondent via Gemini et restent facultatives / non destructives pour le manuscrit.

## Sprint 6 — Industrialisation, qualité & production

**Objectif** : rendre Plumora testable, déployable et exploitable.

Périmètre :
- tests automatisés ;
- JaCoCo ;
- Docker ;
- GitHub Actions ;
- déploiement OVHcloud ;
- Caddy / HTTPS ;
- healthchecks ;
- corrections de production.

Preuves Git :
- Backend : `1740857` — correction Caddy apex domain — 2026-07-20 ;
- Backend : `8a4718a` — JaCoCo + Testcontainers 1.21.3 — 2026-07-23 ;
- Backend : `0a33874` — reset password — 2026-07-24 ;
- Backend : `c7eaf78` — SMTP réel — 2026-07-24 ;
- Backend : `6764cbb` — correction MAIL_FROM en production — 2026-07-24.

**Critère de sortie** : la chaîne CI/CD est opérationnelle, la production répond aux healthchecks et les fonctions critiques sont couvertes par des tests.

## Sprint 7 — Observabilité & stabilisation

**Objectif** : superviser l’exploitation et réduire le temps de détection / diagnostic des incidents.

Périmètre :
- Prometheus ;
- Grafana ;
- node-exporter ;
- postgres-exporter ;
- Loki / Promtail ;
- Alertmanager ;
- alertes Discord ;
- stabilisation finale.

Preuves Git :
- Backend : `7254fd3` — Prometheus + Grafana — 2026-08-20 ;
- Backend : `2913ff2` — exporters + Loki/Promtail + Alertmanager/Discord — 2026-08-21.

**Critère de sortie** : métriques, logs et alertes permettent d’identifier l’état de l’API, de l’hôte et de PostgreSQL.

---

# Planning macro à reporter dans le Gantt

| Phase / lot | Début observé | Fin / jalon observé |
| --- | --- | --- |
| Étude / cadrage / conception initiale | mai 2026 | mai 2026 |
| Socle & authentification | 21/05/2026 | mai 2026 |
| Auteur & écriture | 22/05/2026 | mai 2026 |
| Lecture / catalogue / bêta-lecture MVP | 23/05/2026 | juillet 2026 |
| Enrichissement catalogue & UX | 05/07/2026 | juillet 2026 |
| Plumo IA / Gemini | 12/07/2026 | juillet 2026 |
| Industrialisation / déploiement / corrections prod | juillet 2026 | 24/07/2026 et évolutions ultérieures |
| Monitoring / observabilité | 20/08/2026 | 21/08/2026 |
| Stabilisation / documentation / restitution | août 2026 | septembre 2026 |

> Les dates ci-dessus proviennent des traces Git disponibles et servent de base au Gantt « réalisé ». Le Gantt « prévisionnel » doit être identifié comme une planification formalisée/reconstituée s’il n’existait pas sous cette forme au démarrage.
