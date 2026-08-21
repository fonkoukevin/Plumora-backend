-- Cree le role Postgres dedie, en lecture seule, utilise par postgres-exporter. A executer UNE
-- SEULE FOIS (pas applique automatiquement : docker-entrypoint-initdb.d ne se declenche que sur
-- un volume postgres_data neuf/vide, et celui de ce depot contient deja des donnees).
--
-- Executer sur le VPS, apres avoir mis POSTGRES_EXPORTER_PASSWORD dans deploy/.env :
--   docker compose --env-file .env -f compose.prod.yml exec postgres psql -U $POSTGRES_USER -d postgres
-- puis coller les instructions ci-dessous en remplacant REPLACE_WITH_POSTGRES_EXPORTER_PASSWORD_VALUE
-- par la meme valeur que POSTGRES_EXPORTER_PASSWORD dans .env.
--
-- pg_monitor est un role integre a PostgreSQL (10+) : accorde uniquement un SELECT sur les vues
-- pg_stat_*/pg_settings et fonctions de supervision associees. Ne donne aucun acces aux tables
-- applicatives (books, users, manuscripts, ...) ni aucun droit CREATE/INSERT/UPDATE/DELETE.

CREATE USER postgres_exporter WITH PASSWORD 'REPLACE_WITH_POSTGRES_EXPORTER_PASSWORD_VALUE';
ALTER USER postgres_exporter SET SEARCH_PATH TO postgres_exporter, pg_catalog;
GRANT pg_monitor TO postgres_exporter;
GRANT CONNECT ON DATABASE postgres TO postgres_exporter;
