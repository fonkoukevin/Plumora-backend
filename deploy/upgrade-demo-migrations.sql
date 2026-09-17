-- Backup first; stop all backend instances. See docs/UX-RNCP.md.
BEGIN;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '14' AND (checksum NOT IN (-1116227248, -241283243) OR NOT success OR checksum IS NULL)) THEN
    RAISE EXCEPTION 'Unexpected V14 history: stop and investigate';
  END IF;
  IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '15' AND (checksum NOT IN (1666250325, -241283243) OR NOT success OR checksum IS NULL)) THEN
    RAISE EXCEPTION 'Unexpected V15 history: stop and investigate';
  END IF;
  IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '16' AND (checksum NOT IN (-1140426414, -241283243) OR NOT success OR checksum IS NULL)) THEN
    RAISE EXCEPTION 'Unexpected V16 history: stop and investigate';
  END IF;
  IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '19' AND (checksum NOT IN (-1984315891, -241283243) OR NOT success OR checksum IS NULL)) THEN
    RAISE EXCEPTION 'Unexpected V19 history: stop and investigate';
  END IF;
END $$;
UPDATE flyway_schema_history SET checksum = -241283243 WHERE version = '14' AND checksum = -1116227248 AND success;
UPDATE flyway_schema_history SET checksum = -241283243 WHERE version = '15' AND checksum = 1666250325 AND success;
UPDATE flyway_schema_history SET checksum = -241283243 WHERE version = '16' AND checksum = -1140426414 AND success;
UPDATE flyway_schema_history SET checksum = -241283243 WHERE version = '19' AND checksum = -1984315891 AND success;
COMMIT;
