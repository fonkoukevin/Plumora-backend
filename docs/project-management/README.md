# Pilotage du projet Plumora — Bloc 3 RNCP

Ce dossier formalise la gestion de projet de **Plumora** pour le Bloc 3 « Coordonner et piloter un projet de développement d’applications logicielles ».

> **Traçabilité et transparence** — Cette formalisation a été consolidée en septembre 2026 à partir du périmètre réel du produit, de l’historique Git, des commits, des pull requests, des workflows GitHub Actions et des versions effectivement déployées. Elle ne prétend pas que chaque artefact de pilotage existait sous cette forme dès le premier jour du projet.

## 1. Méthodologie retenue

Plumora est piloté avec une **approche Agile itérative inspirée de Kanban et de Scrum**, adaptée à un projet réalisé principalement individuellement.

Le projet n’est pas présenté comme un Scrum strict : il n’existe pas d’équipe Scrum complète avec Product Owner, Scrum Master et Daily Scrum formels. Les pratiques retenues sont celles qui apportent une valeur réelle au projet :

1. constitution et priorisation d’un Product Backlog ;
2. découpage en lots fonctionnels / itérations ;
3. Sprint Planning pour sélectionner et estimer les éléments d’une itération ;
4. suivi Kanban de l’état des tâches ;
5. tests et validation avant passage en Terminé ;
6. revue des écarts et ajustement de l’itération suivante.

Flux de travail :

`Backlog → Prêt → En cours → Revue / Tests → Terminé`

## 2. Outils de pilotage

| Besoin | Outil / artefact | Utilisation |
| --- | --- | --- |
| Gestion opérationnelle | GitHub Projects | Backlog, Kanban, priorités, sprints, responsables, charge et statut |
| Planification macro | Diagramme de Gantt | Phases, dates, dépendances, prévisionnel / réalisé |
| Responsabilités | Matrice RACI | Clarification des rôles et responsabilités |
| Traçabilité technique | GitHub | Issues, branches, commits, pull requests et GitHub Actions |
| Preuves de réalisation | Historique Git / CI/CD | Association des tâches aux livrables réellement produits |

## 3. Configuration du GitHub Project

Nom : **Plumora — Pilotage projet**

### Statuts

- `Backlog`
- `Prêt`
- `En cours`
- `Revue / Tests`
- `Terminé`

### Champs personnalisés

| Champ | Type | Valeurs recommandées |
| --- | --- | --- |
| Priorité | Single select | P0 Critique, P1 Haute, P2 Moyenne, P3 Basse |
| Type de travail | Single select | Feature, Bug, Technique, Documentation, Dette technique, Certification RNCP |
| Module | Single select | Auth, Auteur, Lecture, Catalogue, Bêta-lecture, IA, Admin, DevOps, Monitoring |
| Sprint | Single select | S1 à S8, Backlog |
| Charge | Number | Story points : 1, 2, 3, 5, 8, 13 |
| Responsable | Assignee | fonkoukevin ou testeur concerné |
| Date de début | Date | Début planifié / reconstitué |
| Date cible | Date | Échéance prévue |
| Date réelle | Date | Date de réalisation issue de Git |
| Preuve | Text | Commit, PR, workflow ou URL de démonstration |

### Vues

1. **Kanban — Global** : groupé par Statut.
2. **Product Backlog** : trié par Priorité puis Sprint.
3. **Sprint courant** : filtre sur le Sprint sélectionné.
4. **Roadmap** : vue chronologique basée sur les champs de dates.
5. **Par module** : groupé par Module.
6. **Risques / anomalies** : filtre `Type de travail = Bug` ou `Dette technique`.

## 4. Règles de gestion du backlog

Une tâche doit contenir au minimum :

- un objectif clair ;
- une priorité ;
- un module ;
- un sprint ou `Backlog` ;
- une estimation en story points ;
- un responsable ;
- des critères d’acceptation ;
- une preuve de réalisation lorsqu’elle est terminée.

### Definition of Ready

Une tâche peut passer de `Backlog` à `Prêt` lorsque :

- le besoin est compris ;
- les critères d’acceptation sont écrits ;
- les dépendances principales sont identifiées ;
- la priorité et l’estimation sont renseignées.

### Definition of Done

Une tâche peut passer à `Terminé` lorsque :

- l’implémentation est terminée ;
- les tests nécessaires sont passés ;
- aucune anomalie bloquante connue ne subsiste ;
- le code est tracé dans Git ;
- le comportement est validé sur l’environnement concerné ;
- la documentation est mise à jour si nécessaire.

## 5. Gestion de la charge

Plumora étant principalement un projet individuel, une limite de travail en cours est retenue : **2 tâches maximum en `En cours`**. Cette règle évite de disperser l’effort et favorise la finalisation des fonctionnalités avant d’en démarrer de nouvelles.

Échelle d’estimation :

- `1` : très faible ;
- `2` : faible ;
- `3` : simple ;
- `5` : intermédiaire ;
- `8` : complexe ;
- `13` : très complexe / à redécouper si possible.

## 6. Indicateurs de suivi

Les indicateurs utilisés pour le pilotage sont :

- taux d’avancement : éléments terminés / éléments planifiés ;
- respect des échéances : tâches terminées à la date cible ;
- charge réalisée par sprint ;
- nombre de tâches bloquées ;
- nombre d’anomalies ouvertes / corrigées ;
- état de la CI ;
- disponibilité de la production ;
- risques ouverts et actions de mitigation.

## 7. Documents associés

- [ROADMAP.md](ROADMAP.md) — lots, sprints historiques et preuves Git ;
- [SPRINT_8_FINALISATION.md](SPRINT_8_FINALISATION.md) — Sprint 8 prospectif réel avant soutenance ;
- [RACI.md](RACI.md) — affectation des responsabilités ;
- [RISKS_AND_KPI.md](RISKS_AND_KPI.md) — risques et indicateurs ;
- historique des dépôts `Plumora-backend` et `Plumora-frontend` — preuves de réalisation.

## Traçabilité RNCP39583

Le Project ajoute une couche de traçabilité documentaire pour **34 compétences** (dont **15 éliminatoires**, selon la liste de travail fournie) réparties entre les quatre blocs : cadrage, conception et développement, pilotage, maintenance.

Les trackers sont des collecteurs de preuves. **Seul le jury peut statuer officiellement sur les compétences.** Le statut `Terminé` signifie uniquement que la collecte/formalisation documentaire est suffisante ; il ne constitue pas une décision du jury.

- [🎓 Couverture RNCP](https://github.com/users/fonkoukevin/projects/2/views/8) : table des 34 compétences et de leur couverture.
- [🔴 Compétences éliminatoires](https://github.com/users/fonkoukevin/projects/2/views/9) : table filtrée sur les 15 compétences critiques.
- [RNCP_COVERAGE.md](RNCP_COVERAGE.md) : photographie vérifiée des champs du Project, preuves, limites et actions restantes.

Les quatre champs ajoutés sont `Bloc RNCP`, `Compétence RNCP`, `Criticité RNCP` et `Couverture RNCP`. Les trackers portent `Type de travail = Certification RNCP` et sont exclus des six vues opérationnelles, qui conservent leurs filtres précédents. Le champ existant `Preuve` contient les liens directs.

États documentaires : **À formaliser**, **Preuve partielle**, **Preuve disponible**, **À vérifier**. Statuts de collecte : Backlog (preuve à identifier), Prêt (travail de formalisation identifié), En cours (preuve à compléter), Terminé (collecte documentée). Les trackers ne sont pas inclus dans les charges/sprints produit : leur statut En cours décrit une couverture incomplète et ne mesure pas le travail produit simultané.

Les trackers du Bloc 1 et les issues historiques #23 à #33 sont rétrospectifs. Les tâches S8 #35 à #38 restent prospectives. Les liens de fichiers utilisés comme preuves sont figés sur les SHA inspectés ; aucune date historique n’a été réécrite.

**État vérifié au 15 septembre 2026 : 11 preuves disponibles, 20 partielles, 3 à formaliser.** Le regroupement des tables par Bloc RNCP (ordre 1 → 4) et le tri croissant par Compétence RNCP restent à appliquer dans l’interface : ces réglages ne sont pas exposés par l’API publique inspectée et aucun navigateur connecté n’était disponible. Le rendu des dates de la Roadmap reste également à vérifier. L’issue [#34](https://github.com/fonkoukevin/Plumora-backend/issues/34) est donc ouverte et En cours.
