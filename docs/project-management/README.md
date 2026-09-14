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

## 3. Configuration cible du GitHub Project

Nom recommandé : **Plumora — Pilotage projet**

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
| Type | Single select | Feature, Bug, Technique, Documentation, Dette technique |
| Module | Single select | Auth, Auteur, Lecture, Catalogue, Bêta-lecture, IA, Admin, DevOps, Monitoring |
| Sprint | Single select | S1 à S7, Backlog |
| Charge | Number | Story points : 1, 2, 3, 5, 8, 13 |
| Responsable | Assignee | fonkoukevin ou testeur concerné |
| Date de début | Date | Début planifié / reconstitué |
| Date cible | Date | Échéance prévue |
| Date réelle | Date | Date de réalisation issue de Git |
| Preuve | Text | Commit, PR, workflow ou URL de démonstration |

### Vues recommandées

1. **Kanban — Global** : groupé par Statut.
2. **Product Backlog** : trié par Priorité puis Sprint.
3. **Sprint courant** : filtre sur le Sprint sélectionné.
4. **Roadmap** : vue chronologique Date de début / Date cible.
5. **Par module** : groupé par Module.
6. **Risques / anomalies** : filtre Type = Bug ou Dette technique.

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

- [ROADMAP.md](ROADMAP.md) — lots, sprints et preuves Git ;
- [RACI.md](RACI.md) — affectation des responsabilités ;
- [RISKS_AND_KPI.md](RISKS_AND_KPI.md) — risques et indicateurs ;
- historique des dépôts `Plumora-backend` et `Plumora-frontend` — preuves de réalisation.
