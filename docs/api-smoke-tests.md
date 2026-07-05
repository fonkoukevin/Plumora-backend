# Plumora API smoke tests

Ce jeu de tests verifie les routes HTTP de l'API contre une instance locale demarree.

Il couvre les parcours principaux :

- auth, profil et roles
- livres, chapitres et versions
- publication et catalogue public
- lecture, progression, favoris et reviews
- assistant IA d'ecriture et recommandations
- beta-reading, invitations, chapitres partages et commentaires
- notifications
- reports
- routes admin

## Prerequis

Demarrer l'API avec PostgreSQL :

```powershell
docker compose up --build
```

Les tests admin utilisent par defaut le compte seed :

```text
admin@plumora.test / password
```

Si ce compte n'est pas present dans la base locale, lance le script avec `-SkipAdmin` ou reseed la base de test.

Pour reseeder les comptes de demo :

```powershell
Get-Content .\docker\postgres\seed-test-data.sql | docker exec -i plumora-postgres psql -U plumora -d plumora_db
```

## Lancer la suite

Depuis la racine du repo :

```powershell
.\scripts\api-smoke-tests.ps1
```

Avec une autre URL :

```powershell
.\scripts\api-smoke-tests.ps1 -BaseUrl "http://localhost:8080/api/v1"
```

Sans routes admin :

```powershell
.\scripts\api-smoke-tests.ps1 -SkipAdmin
```

Avec un autre compte admin :

```powershell
.\scripts\api-smoke-tests.ps1 -AdminEmail "admin@plumora.test" -AdminPassword "password"
```

## Ce que fait le script

Le script cree des utilisateurs temporaires avec un suffixe unique, puis teste les routes dans un ordre metier coherent :

1. creation de comptes reader, author et beta-reader
2. creation d'un livre par l'auteur
3. verification des protections d'acces
4. creation de chapitres et versions
5. publication du livre
6. verification du catalogue public
7. lecture, progression, favoris et reviews
8. appels IA fake locaux
9. campagne beta-reading avec invitation, acceptation et commentaire
10. notifications et reports
11. routes admin si activees

Les donnees creees restent en base pour inspection, avec des emails du type :

```text
author.<suffix>@plumora.test
reader.<suffix>@plumora.test
beta.<suffix>@plumora.test
```

Le script retourne un code de sortie `1` des qu'une route critique echoue, ce qui permet aussi de l'utiliser dans un check CI local.
