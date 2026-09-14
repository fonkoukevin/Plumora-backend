# Affectation des missions et matrice RACI — Plumora

## Contexte d’organisation

Plumora a été réalisé principalement individuellement. Plusieurs rôles projet sont donc portés par la même personne. La matrice ci-dessous distingue volontairement les responsabilités **par rôle** afin de clarifier l’organisation, sans inventer une équipe qui n’existait pas.

## Rôles projet

| Rôle | Mission principale | Porteur |
| --- | --- | --- |
| Chef de projet | Planification, priorisation, arbitrages, suivi | Kevin |
| Architecte logiciel | Architecture, choix techniques, cohérence globale | Kevin |
| Développeur backend | API Spring Boot, sécurité, données, tests backend | Kevin |
| Développeur frontend | Application Flutter, parcours utilisateurs, intégration API | Kevin |
| DevOps | Docker, CI/CD, OVHcloud, observabilité | Kevin |
| QA / test technique | Tests automatisés, non-régression, validation technique | Kevin |
| Testeur / utilisateur | Recette fonctionnelle et retours d’usage | Utilisateur/testeur concerné |

## Matrice RACI

Légende :
- **R** — Responsible : réalise l’activité ;
- **A** — Accountable : porte la responsabilité finale ;
- **C** — Consulted : consulté avant décision / validation ;
- **I** — Informed : informé du résultat.

| Activité | Chef de projet | Architecte | Dev backend | Dev frontend | DevOps | QA | Utilisateur / testeur |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Cadrage du besoin | A/R | C | I | I | I | I | C |
| Définition de l’architecture | A | R | C | C | C | I | I |
| Modèle de données / API | A | C | R | C | I | C | I |
| Développement backend | A | C | R | C | I | C | I |
| Développement frontend | A | C | C | R | I | C | C |
| Intégration Plumo IA | A | C | R | R | I | C | C |
| Tests automatisés | A | I | R | R | I | R | I |
| Recette fonctionnelle | A | I | C | C | I | R | R |
| CI/CD et conteneurisation | A | C | C | C | R | C | I |
| Déploiement production | A | I | C | C | R | C | I |
| Supervision / alerting | A | C | C | I | R | C | I |
| Arbitrage projet | A/R | C | C | C | C | C | C |
| Validation de version | A | I | C | C | C | R | R |
| Documentation / restitution | A/R | C | C | C | C | C | I |

## Affectation opérationnelle dans GitHub Projects

Pour chaque Issue / tâche, renseigner :

- **Assignee** : responsable opérationnel ;
- **Sprint** : itération concernée ;
- **Priorité** : P0 à P3 ;
- **Module** : Auth, Auteur, Lecture, Catalogue, Bêta-lecture, IA, Admin, DevOps, Monitoring ;
- **Charge** : story points ;
- **Statut** : Backlog, Prêt, En cours, Revue / Tests, Terminé ;
- **Preuve** : commit, PR, workflow ou validation associée.

Cette combinaison permet de distinguer :
- l’affectation concrète des tâches dans GitHub Projects ;
- la responsabilité générale des rôles via la matrice RACI.
