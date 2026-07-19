#!/usr/bin/env bash
set -euo pipefail

# Restauration d'une sauvegarde PostgreSQL de production.
#
# CETTE OPERATION REMPLACE INTEGRALEMENT LES DONNEES ACTUELLES de la base par celles de la
# sauvegarde fournie. Elle NE S'EXECUTE JAMAIS automatiquement : ce script exige le chemin d'une
# sauvegarde en argument et une confirmation explicite tapee au clavier avant d'ecraser quoi
# que ce soit. A n'utiliser qu'en cas de besoin reel (incident, restauration de secours),
# jamais depuis un cron/systemd timer ni depuis deploy.sh/rollback.sh.
#
# Usage : ./scripts/restore-postgres.sh /chemin/vers/plumora-xxx.sql.gz

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"

BACKUP_FILE="${1:-}"

if [[ -z "${BACKUP_FILE}" ]]; then
	echo "Usage : $0 /chemin/vers/sauvegarde.sql.gz" >&2
	exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
	echo "Fichier de sauvegarde introuvable : ${BACKUP_FILE}" >&2
	exit 1
fi

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

echo "!!! ATTENTION !!!"
echo "Cette operation va REMPLACER INTEGRALEMENT le contenu actuel de la base '${POSTGRES_DB}'"
echo "par celui de :"
echo "  ${BACKUP_FILE}"
echo "Toutes les donnees ecrites depuis cette sauvegarde seront perdues. Cette action est"
echo "IRREVERSIBLE sauf a disposer d'une autre sauvegarde plus recente."
echo

read -r -p "Tapez exactement RESTORE pour confirmer : " CONFIRMATION

if [[ "${CONFIRMATION}" != "RESTORE" ]]; then
	echo "Confirmation invalide, restauration annulee."
	exit 1
fi

echo "Restauration en cours..."

gunzip -c "${BACKUP_FILE}" | docker compose --project-name "${COMPOSE_PROJECT}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" \
	exec -T postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

echo "Restauration terminee. Verifiez l'application (./scripts/health-check.sh) avant de"
echo "considerer l'operation reussie. Rappel : cette restauration ne touche pas au code"
echo "applicatif deploye (images backend/frontend) - si l'incident venait d'un deploiement,"
echo "voir aussi ./scripts/rollback.sh."
