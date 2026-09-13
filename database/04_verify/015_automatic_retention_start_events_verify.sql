\set ON_ERROR_STOP on
\encoding UTF8
\if :{?expected_checksum}
\else
  \echo 'expected_checksum psql variable is required'
  \quit 3
\endif
\if :{?target_database}
\else
  \set target_database seal_dev
\endif
\connect :target_database
SET search_path = seal, public;

DO $$
DECLARE
    definition TEXT;
BEGIN
    SELECT pg_get_constraintdef(oid)
      INTO definition
      FROM pg_constraint
     WHERE conrelid = 'seal.retention_policy_version'::regclass
       AND conname = 'ck_retention_start_event';

    IF definition IS NULL
       OR position('MAIL_FIRST_SENT_AT' IN definition) = 0
       OR position('REQUEST_EXPIRES_AT' IN definition) = 0 THEN
        RAISE EXCEPTION 'automatic retention start events are not allowed';
    END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260901_015_add_automatic_retention_start_events.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Automatic retention start events checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Automatic retention start events verification passed'
