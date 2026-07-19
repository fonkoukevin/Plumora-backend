#!/usr/bin/env bash
set -uo pipefail

# Verifie l'etat du stack de production : conteneurs, app.plumora.fr, api.plumora.fr/actuator/health.
# Sort avec un code non nul si l'une des verifications echoue (utilisable en supervision/cron).
#
# Usage : ./scripts/health-check.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yml"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"

if [[ -f "${ENV_FILE}" ]]; then
	set -a
	# shellcheck disable=SC1090
	source "${ENV_FILE}"
	set +a
fi

COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-plumora}"
APP_URL="${APP_URL:-https://${APP_DOMAIN:-app.plumora.fr}}"
API_HEALTH_URL="${API_HEALTH_URL:-https://${API_DOMAIN:-api.plumora.fr}/api/v1/actuator/health}"

EXIT_CODE=0

echo "=== Etat des conteneurs ==="
if [[ -f "${ENV_FILE}" ]]; then
	docker compose --project-name "${COMPOSE_PROJECT}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps
else
	docker compose --project-name "${COMPOSE_PROJECT}" -f "${COMPOSE_FILE}" ps
fi
echo

check_url() {
	local label="$1" url="$2" expect_body="${3:-}"
	local http_code body

	http_code="$(curl -fsS -o /tmp/plumora-health-check-body -w '%{http_code}' "${url}" 2>/dev/null || echo "000")"
	body="$(cat /tmp/plumora-health-check-body 2>/dev/null || true)"
	rm -f /tmp/plumora-health-check-body

	echo "=== ${label} (${url}) ==="
	echo "Code HTTP : ${http_code}"

	if [[ "${http_code}" != "200" ]]; then
		echo "PROBLEME : ${label} a repondu ${http_code} (attendu 200)." >&2
		EXIT_CODE=1
		return
	fi

	if [[ -n "${expect_body}" ]] && ! grep -q "${expect_body}" <<<"${body}"; then
		echo "PROBLEME : ${label} a repondu 200 mais le contenu attendu ('${expect_body}') est absent." >&2
		EXIT_CODE=1
		return
	fi

	echo "OK"
}

check_url "app.plumora.fr" "${APP_URL}"
echo
check_url "api.plumora.fr/actuator/health" "${API_HEALTH_URL}" '"status":"UP"'

echo
if [[ "${EXIT_CODE}" -eq 0 ]]; then
	echo "Toutes les verifications sont passees."
else
	echo "Au moins une verification a echoue." >&2
fi

exit "${EXIT_CODE}"
