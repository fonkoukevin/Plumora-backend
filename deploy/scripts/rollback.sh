#!/usr/bin/env bash
set -euo pipefail

# Revient aux images backend/frontend precedemment deployees.
#
# Par defaut, lit deploy/.env.last-deployed (ecrit automatiquement par deploy.sh juste avant
# chaque deploiement) pour connaitre les images vers lesquelles revenir. Des images explicites
# peuvent aussi etre passees en arguments :
#   ./scripts/rollback.sh [backend-image] [frontend-image]
#
# CE SCRIPT NE TOUCHE JAMAIS A LA BASE DE DONNEES. Il ne fait revenir en arriere QUE le code
# applicatif (images des conteneurs backend et frontend-web) : PostgreSQL et les migrations
# Flyway deja appliquees restent en l'etat. Si le deploiement annule avait introduit une
# migration Flyway incompatible avec l'ancien code, revenir au code precedent NE restaure PAS
# le schema : l'ancien code peut alors se retrouver face a un schema qu'il ne reconnait pas.
# Dans ce cas, la seule option sure est une restauration de sauvegarde PostgreSQL anterieure a
# la migration en cause (./scripts/restore-postgres.sh), avec la perte de donnees que cela
# implique entre cette sauvegarde et maintenant. Ce script ne tente jamais cela automatiquement.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"
LAST_DEPLOYED_FILE="${DEPLOY_DIR}/.env.last-deployed"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-120}"

compose() {
	docker compose --project-name "${COMPOSE_PROJECT_NAME:-plumora}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

wait_healthy() {
	local service="$1" timeout="$2" waited=0 container_id status
	echo "Attente de l'etat 'healthy' pour '${service}' (max ${timeout}s)..."
	while (( waited < timeout )); do
		container_id="$(compose ps -q "${service}" 2>/dev/null || true)"
		if [[ -n "${container_id}" ]]; then
			status="$(docker inspect --format='{{.State.Health.Status}}' "${container_id}" 2>/dev/null || echo "unknown")"
			if [[ "${status}" == "healthy" ]]; then
				echo "'${service}' est en bonne sante."
				return 0
			fi
		fi
		sleep 3
		waited=$(( waited + 3 ))
	done
	return 1
}

if [[ ! -f "${ENV_FILE}" ]]; then
	echo "Fichier d'environnement introuvable : ${ENV_FILE}" >&2
	exit 1
fi

ROLLBACK_BACKEND_IMAGE="${1:-}"
ROLLBACK_FRONTEND_IMAGE="${2:-}"

if [[ -z "${ROLLBACK_BACKEND_IMAGE}" || -z "${ROLLBACK_FRONTEND_IMAGE}" ]]; then
	if [[ ! -f "${LAST_DEPLOYED_FILE}" ]]; then
		echo "Aucune image explicite fournie et $(basename "${LAST_DEPLOYED_FILE}") introuvable." >&2
		echo "Usage : $0 [backend-image] [frontend-image]" >&2
		echo "($(basename "${LAST_DEPLOYED_FILE}") est ecrit automatiquement par deploy.sh avant chaque deploiement.)" >&2
		exit 1
	fi
	# shellcheck disable=SC1090
	source "${LAST_DEPLOYED_FILE}"
	ROLLBACK_BACKEND_IMAGE="${ROLLBACK_BACKEND_IMAGE:-${PREVIOUS_BACKEND_IMAGE:-}}"
	ROLLBACK_FRONTEND_IMAGE="${ROLLBACK_FRONTEND_IMAGE:-${PREVIOUS_FRONTEND_IMAGE:-}}"
fi

if [[ -z "${ROLLBACK_BACKEND_IMAGE}" || -z "${ROLLBACK_FRONTEND_IMAGE}" ]]; then
	echo "Impossible de determiner les images vers lesquelles revenir." >&2
	exit 1
fi

echo "!!! ATTENTION !!!"
echo "Ce rollback va redemarrer :"
echo "  backend      -> ${ROLLBACK_BACKEND_IMAGE}"
echo "  frontend-web -> ${ROLLBACK_FRONTEND_IMAGE}"
echo
echo "Rappel : aucune action n'est effectuee sur PostgreSQL ni sur les migrations Flyway deja"
echo "appliquees. Si le probleme vient d'une migration incompatible avec le code precedent, ce"
echo "rollback seul NE SUFFIT PAS (voir l'entete de ce script)."
echo

read -r -p "Tapez exactement ROLLBACK pour confirmer : " CONFIRMATION

if [[ "${CONFIRMATION}" != "ROLLBACK" ]]; then
	echo "Confirmation invalide, rollback annule."
	exit 1
fi

echo "==> Rollback du backend"
export BACKEND_IMAGE="${ROLLBACK_BACKEND_IMAGE}"
compose up -d --no-deps backend

if ! wait_healthy backend "${HEALTH_TIMEOUT_SECONDS}"; then
	echo "Le backend n'est pas revenu a un etat sain apres le rollback." >&2
	echo "Diagnostiquer avec : docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} logs backend" >&2
	exit 1
fi

echo "==> Rollback du frontend"
export FRONTEND_IMAGE="${ROLLBACK_FRONTEND_IMAGE}"
compose up -d --no-deps frontend-web

echo "Rollback termine. Verifier avec ./scripts/health-check.sh"
