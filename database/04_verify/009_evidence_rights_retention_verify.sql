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
SET search_path=seal,public;

DO $$
DECLARE n TEXT;
BEGIN
  FOREACH n IN ARRAY ARRAY['evidence_package','evidence_file','evidence_verification','rights_request',
    'rights_request_event','consent_withdrawal','retention_anchor_event','legal_hold','retention_job','retention_job_event'] LOOP
    IF to_regclass('seal.'||n) IS NULL THEN RAISE EXCEPTION 'missing table %',n; END IF;
    IF (SELECT tableowner FROM pg_tables WHERE schemaname='seal' AND tablename=n)<>'seal_owner' THEN
      RAISE EXCEPTION 'wrong owner for %',n;
    END IF;
    IF has_table_privilege('seal_app','seal.'||n,'DELETE') OR has_table_privilege('seal_app','seal.'||n,'TRUNCATE') THEN
      RAISE EXCEPTION 'destructive privilege on %',n;
    END IF;
  END LOOP;
  FOREACH n IN ARRAY ARRAY['evidence_file','evidence_verification','rights_request_event','consent_withdrawal','retention_job_event'] LOOP
    IF has_table_privilege('seal_app','seal.'||n,'UPDATE') THEN RAISE EXCEPTION 'immutable update privilege on %',n; END IF;
  END LOOP;
  IF NOT EXISTS(SELECT 1 FROM pg_indexes WHERE schemaname='seal' AND indexname='uq_active_evidence_response') THEN
    RAISE EXCEPTION 'active evidence unique index missing';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM permission WHERE permission_code='STATISTICS_VIEW') THEN
    RAISE EXCEPTION 'phase6 permissions missing';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260824_005_create_evidence_rights_retention_tables.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Phase 6 checksum does not match the applied SQL file'
  \quit 4
\endif
\echo 'Phase 6 schema verification passed'
