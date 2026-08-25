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
  required_constraint TEXT;
BEGIN
  FOREACH required_constraint IN ARRAY ARRAY[
    'fk_rights_recipient_organization',
    'fk_rights_request_recipient_organization',
    'ck_rights_source_request_required',
    'fk_retention_request_recipient',
    'fk_retention_source_rights_context'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_constraint c
      JOIN pg_class t ON t.oid=c.conrelid
      JOIN pg_namespace n ON n.oid=t.relnamespace
      WHERE n.nspname='seal' AND c.conname=required_constraint AND c.convalidated
    ) THEN
      RAISE EXCEPTION 'required validated constraint is missing: %', required_constraint;
    END IF;
  END LOOP;

  IF EXISTS (
    SELECT 1 FROM rights_request
    WHERE rights_type IN ('ERASURE','RESTRICTION','WITHDRAWAL')
      AND consent_request_id IS NULL
  ) THEN
    RAISE EXCEPTION 'rights request exists without its required consent request';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260825_008_harden_rights_scope_integrity.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Rights scope integrity checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Rights scope integrity verification passed'
