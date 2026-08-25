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
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes
     WHERE schemaname='seal' AND tablename='legal_hold'
       AND indexname='uq_active_legal_hold_reason'
       AND indexdef LIKE '%(consent_request_id, hold_reason_code)%'
       AND indexdef LIKE '%WHERE active%'
  ) THEN
    RAISE EXCEPTION 'active legal hold reason unique index is missing';
  END IF;

  IF EXISTS (
    SELECT 1 FROM pg_indexes
     WHERE schemaname='seal' AND indexname='uq_active_legal_hold'
  ) THEN
    RAISE EXCEPTION 'legacy single active legal hold index still exists';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260825_010_allow_legal_hold_reasons.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Legal hold reason checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Legal hold reason verification passed'
