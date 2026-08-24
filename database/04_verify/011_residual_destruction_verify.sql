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
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='public_consent_session' AND column_name='flow_destroyed_at') THEN
    RAISE EXCEPTION 'public session destruction timestamp is missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='rights_request' AND column_name='personal_data_destroyed_at') THEN
    RAISE EXCEPTION 'rights personal-data destruction timestamp is missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='retention_job' AND column_name='destroyed_session_count') THEN
    RAISE EXCEPTION 'retention residual destruction counts are missing';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='public_consent_session' AND column_name='flow_hmac' AND is_nullable<>'YES') THEN
    RAISE EXCEPTION 'public session flow HMAC must be nullable after destruction';
  END IF;
  IF has_table_privilege('seal_app','seal.public_consent_session','DELETE')
      OR has_table_privilege('seal_app','seal.rights_request','DELETE') THEN
    RAISE EXCEPTION 'application role has destructive table privileges';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260824_007_add_residual_destruction.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Residual destruction checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Residual destruction verification passed'
