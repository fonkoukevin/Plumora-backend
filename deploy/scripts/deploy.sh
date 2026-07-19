#!/usr/bin/env bash
set -euo pipefail

# Deploiement (ou mise a jour) du stack de production Plumora.
#
# Etapes : verification du .env, validation de "docker compose config" (sans jamais imprimer la
# configuration resolue, qui contiendrait les secrets en clair), sauvegarde PostgreSQL si une
# base tourne deja, enregistrement des images actuellement deployees (pour un rollback eventuel
# via ./rollback.sh), recuperation des nouvelles images, demarrage progressif
# (postgres -> backend -> frontend-web/caddy) avec attente des healthchecks a chaque etape.
#
# Si le backend ne devient pas sain a temps, le script s'arrete avec un code d'erreur SANS
# tenter de correction automatique (pas de rollback automatique) : voir le message affiche.
#
# Usage : ./scripts/deploy.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"
LAST_DEPLOYED_FILE="${DEPLOY_DIR}/.env.last-deployed"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-120}"

compose() {
	docker compose --project-name "${COMPOSE_PROJECT_NAME:-plumora}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

current_image_of() {
	local service="$1" container_id
	container_id="$(compose ps -q "${service}" 2>/dev/null || true)"
	[[ -n "${container_id}" ]] && docker inspect --format='{{.Image}}' "${container_id}" 2>/dev/null || true
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

echo "==> Verification du fichier d'environnement"
if [[ ! -f "${ENV_FILE}" ]]; then
	echo "Fichier d'environnement introuvable : ${ENV_FILE}" >&2
	echo "Copier deploy/.env.example en deploy/.env et le completer avant de deployer." >&2
	exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

echo "==> Validation de la configuration Docker Compose"
if ! compose config --quiet; then
	echo "Configuration invalide (voir les erreurs ci-dessus). Deploiement annule." >&2
	exit 1
fi

echo "==> Sauvegarde de PostgreSQL avant deploiement"
if [[ -n "$(compose ps -q postgres 2>/dev/null)" ]]; then
	"${SCRIPT_DIR}/backup-postgres.sh"
else
	echo "Aucun conteneur postgres en cours d'execution (premier deploiement ?) : sauvegarde ignoree."
fi

echo "==> Enregistrement des images actuellement deployees (pour un rollback eventuel)"
PREVIOUS_BACKEND_IMAGE="$(current_image_of backend)"
PREVIOUS_FRONTEND_IMAGE="$(current_image_of frontend-web)"

if [[ -n "${PREVIOUS_BACKEND_IMAGE}" && -n "${PREVIOUS_FRONTEND_IMAGE}" ]]; then
	{
		echo "# Genere automatiquement par deploy.sh le $(date -u +%Y-%m-%dT%H:%M:%SZ)"
		echo "PREVIOUS_BACKEND_IMAGE=${PREVIOUS_BACKEND_IMAGE}"
		echo "PREVIOUS_FRONTEND_IMAGE=${PREVIOUS_FRONTEND_IMAGE}"
	} > "${LAST_DEPLOYED_FILE}"
	echo "Images precedentes enregistrees dans $(basename "${LAST_DEPLOYED_FILE}")."
else
	echo "Pas d'images precedentes en cours d'execution (premier deploiement ?) : rien a enregistrer."
fi

echo "==> Recuperation des nouvelles images"
compose pull

echo "==> Demarrage progressif : postgres"
compose up -d postgres
if ! wait_healthy postgres 60; then
	echo "PostgreSQL n'est pas devenu sain a temps. Deploiement arrete." >&2
	exit 1
fi

echo "==> Demarrage progressif : backend"
compose up -d backend
if ! wait_healthy backend "${HEALTH_TIMEOUT_SECONDS}"; then
	echo "Le backend n'est pas devenu sain a temps. Deploiement arrete." >&2
	echo "Diagnostiquer avec : docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} logs backend" >&2
	if [[ -f "${LAST_DEPLOYED_FILE}" ]]; then
		echo "Pour revenir aux images precedentes : ./scripts/rollback.sh" >&2
	fi
	exit 1
fi

echo "==> Demarrage progressif : frontend-web et caddy"
compose up -d frontend-web caddy

echo "==> Verification finale"
if "${SCRIPT_DIR}/health-check.sh"; then
	echo "Deploiement termine avec succes."
else
	echo "Le deploiement s'est termine mais la verification finale a echoue." >&2
	echo "Voir 'docker compose logs', et envisager ./scripts/rollback.sh si besoin." >&2
	exit 1
fi
