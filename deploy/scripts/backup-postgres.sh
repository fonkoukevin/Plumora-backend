#!/usr/bin/env bash
set -euo pipefail

# Sauvegarde PostgreSQL de production (pg_dump compresse, horodate, retention configurable).
#
# pg_dump est execute A L'INTERIEUR du conteneur postgres (docker compose exec), jamais depuis
# l'hote : PostgreSQL n'a aucun port publie en production (voir compose.prod.yml). La connexion
# se fait via le socket local du conteneur, que pg_hba.conf autorise sans mot de passe (regle
# "local ... trust" du postgres officiel) : le mot de passe n'est donc jamais requis ni affiche
# par ce script, meme dans les logs.
#
# Usage : ./scripts/backup-postgres.sh
# Prevu pour une execution quotidienne (voir deploy/systemd/plumora-backup.timer), depuis
# n'importe quel repertoire courant.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"

if [[ ! -f "${ENV_FILE}" ]]; then
	echo "Fichier d'environnement introuvable : ${ENV_FILE}" >&2
	exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

: "${POSTGRES_DB:?POSTGRES_DB manquant dans ${ENV_FILE}}"
: "${POSTGRES_USER:?POSTGRES_USER manquant dans ${ENV_FILE}}"

COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-plumora}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/plumora/postgres}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

# Repertoire protege : seul le proprietaire (l'operateur ou root via systemd) peut lire les
# archives.
mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"

TIMESTAMP="$(date +%Y-%m-%d_%H-%M-%S)"
BACKUP_FILE="${BACKUP_DIR}/plumora-${POSTGRES_DB}-${TIMESTAMP}.sql.gz"
TMP_FILE="${BACKUP_FILE}.tmp"

echo "Sauvegarde de la base '${POSTGRES_DB}' vers ${BACKUP_FILE}..."

docker compose --project-name "${COMPOSE_PROJECT}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
	exec -T postgres pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
	| gzip > "${TMP_FILE}"

mv "${TMP_FILE}" "${BACKUP_FILE}"
chmod 600 "${BACKUP_FILE}"

echo "Sauvegarde terminee : ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"

echo "Suppression des sauvegardes de plus de ${RETENTION_DAYS} jours..."
find "${BACKUP_DIR}" -maxdepth 1 -name 'plumora-*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -print -delete

# Envoi optionnel vers un stockage S3 (desactive par defaut). Definir BACKUP_S3_BUCKET dans
# .env et disposer d'awscli configure sur le VPS (identifiants geres hors de ce depot) pour
# l'activer. Ne fait rien tant que BACKUP_S3_BUCKET est vide.
if [[ -n "${BACKUP_S3_BUCKET:-}" ]]; then
	if command -v aws >/dev/null 2>&1; then
		echo "Envoi vers s3://${BACKUP_S3_BUCKET}/..."
		aws s3 cp "${BACKUP_FILE}" "s3://${BACKUP_S3_BUCKET}/$(basename "${BACKUP_FILE}")"
	else
		echo "BACKUP_S3_BUCKET est defini mais 'aws' est introuvable : envoi S3 ignore." >&2
	fi
fi

echo "Sauvegardes actuellement conservees :"
find "${BACKUP_DIR}" -maxdepth 1 -name 'plumora-*.sql.gz' -type f | sort
