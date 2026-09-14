# Risques, indicateurs et arbitrages — Plumora

## 1. Tableau des risques

Échelle : Probabilité (P) et Impact (I) de 1 à 3. Criticité = P × I.

| Risque | P | I | Criticité | Prévention / mitigation | Statut |
| --- | :---: | :---: | :---: | --- | --- |
| Régression fonctionnelle après évolution | 2 | 3 | 6 | Tests automatisés, CI GitHub Actions, JaCoCo | Maîtrisé |
| Indisponibilité d’un fournisseur externe | 2 | 3 | 6 | Fallback / alternative quand possible, erreurs contrôlées | Maîtrisé |
| Défaillance du catalogue Gutendex | 3 | 2 | 6 | Open Library puis catalogue Gutenberg local | Traitée |
| Défaillance / retrait d’un modèle Gemini | 2 | 2 | 4 | Modèle configurable et changement d’alias | Traitée |
| Erreur de configuration production | 2 | 3 | 6 | Variables d’environnement, healthchecks, validation prod | Maîtrisé |
| Indisponibilité API / base de données | 2 | 3 | 6 | Prometheus, Grafana, exporters et Alertmanager | Maîtrisé |
| Croissance non bornée des logs | 2 | 2 | 4 | Rotation des logs Docker | Traitée |
| Concentration des rôles sur une seule personne | 3 | 2 | 6 | Documentation, automatisation CI/CD, backlog explicite | Accepté / suivi |
| Retard lié à l’étendue fonctionnelle | 2 | 3 | 6 | Priorisation MVP, backlog, découpage en itérations | Maîtrisé |

## 2. Indicateurs de pilotage

| Indicateur | Calcul / source | Objectif de pilotage |
| --- | --- | --- |
| Avancement sprint | Story points terminés / story points planifiés | Mesurer la réalisation de l’itération |
| Avancement global | Tâches terminées / tâches planifiées | Mesurer l’avancement produit |
| Respect des délais | Tâches terminées à temps / tâches prévues | Identifier les dérives |
| Charge reportée | Story points non terminés en fin d’itération | Ajuster la capacité future |
| Anomalies ouvertes | Issues de type Bug non clôturées | Suivre la qualité |
| CI | taux de workflows réussis / état du dernier pipeline | Garantir l’intégrabilité |
| Tests automatisés | nombre de tests + couverture JaCoCo | Prévenir les régressions |
| Disponibilité API | métriques Prometheus / healthcheck | Surveiller la production |
| Taux d’erreurs 5xx | métriques HTTP | Détecter les incidents applicatifs |
| Temps de réponse | métriques HTTP | Surveiller la performance |
| Ressources hôte | CPU / mémoire / disque | Anticiper les incidents infrastructure |
| PostgreSQL | état exporter / métriques | Surveiller la base |

> Les valeurs chiffrées présentées au jury doivent provenir des données réellement disponibles (GitHub Project, GitHub Actions, JaCoCo, Prometheus/Grafana). Ne pas inventer un historique de KPI qui n’a pas été mesuré.

## 3. Cas d’arbitrage réels exploitables à l’oral

### Arbitrage A — Catalogue Gutendex indisponible en production

**Problème** : Gutendex était bloqué par Cloudflare depuis l’environnement de production, empêchant la découverte et l’import de nouveaux livres.

**Options étudiées** :
1. conserver Gutendex seul et attendre un rétablissement ;
2. utiliser Open Library comme fallback ;
3. mettre en place un catalogue Gutenberg local.

**Décision** : déployer d’abord un fallback Open Library, puis un catalogue Gutenberg local pour retrouver une recherche fiable et des contenus réellement lisibles.

**Preuves Git** : `0530d2b` puis `d2127c6`.

### Arbitrage B — Modèle Gemini retiré

**Problème** : le modèle `gemini-2.5-flash-lite` renvoyait une erreur 404 pour les nouveaux utilisateurs.

**Options étudiées** :
1. désactiver temporairement Plumo ;
2. changer de fournisseur IA ;
3. remplacer le modèle par un alias Gemini maintenu.

**Décision** : utiliser `gemini-flash-lite-latest` afin de conserver l’intégration tout en réduisant le risque de nouveau retrait immédiat.

**Preuve Git** : `24ab6a1`.

### Arbitrage C — Observabilité en production

**Problème** : les healthchecks seuls ne suffisaient pas pour diagnostiquer les incidents et suivre les ressources.

**Options étudiées** :
1. rester sur les logs et healthchecks ;
2. mettre en place uniquement Prometheus/Grafana ;
3. compléter par logs centralisés, métriques hôte/base et alertes.

**Décision** : mise en place progressive de Prometheus/Grafana, puis extension avec node-exporter, postgres-exporter, Loki/Promtail et Alertmanager/Discord.

**Preuves Git** : `7254fd3` puis `2913ff2`.
