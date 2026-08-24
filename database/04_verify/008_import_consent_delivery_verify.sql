\set ON_ERROR_STOP on
\encoding UTF8
\if :{?target_database}
\else
  \set target_database seal_dev
\endif
\if :{?expected_checksum}
\else
  \echo 'expected_checksum psql variable is required'
  \quit 3
\endif
\connect :target_database
SET search_path = seal, public;

DO $$
DECLARE
    expected TEXT[] := ARRAY['recipient_import_batch','recipient_import_row','public_consent_session',
        'consent_response','consent_response_item','mail_outbox','mail_delivery_attempt'];
    name TEXT;
BEGIN
    FOREACH name IN ARRAY expected LOOP
        IF to_regclass('seal.' || name) IS NULL THEN RAISE EXCEPTION 'missing table %', name; END IF;
        IF (SELECT tableowner FROM pg_tables WHERE schemaname='seal' AND tablename=name) <> 'seal_owner' THEN
            RAISE EXCEPTION 'wrong owner for %', name;
        END IF;
        IF has_table_privilege('seal_app','seal.' || name,'DELETE')
           OR has_table_privilege('seal_app','seal.' || name,'TRUNCATE') THEN
            RAISE EXCEPTION 'destructive privilege on %', name;
        END IF;
    END LOOP;
    IF has_table_privilege('seal_app','seal.consent_response','UPDATE')
       OR has_table_privilege('seal_app','seal.consent_response_item','UPDATE')
       OR has_table_privilege('seal_app','seal.mail_delivery_attempt','UPDATE') THEN
        RAISE EXCEPTION 'immutable records are updateable';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM permission WHERE permission_code='RECIPIENT_IMPORT')
       OR NOT EXISTS (SELECT 1 FROM permission WHERE permission_code='MAIL_SEND') THEN
        RAISE EXCEPTION 'phase permissions missing';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE connamespace='seal'::regnamespace
        AND conname='ck_recipient_source' AND pg_get_constraintdef(oid) LIKE '%EXCEL%') THEN
        RAISE EXCEPTION 'recipient EXCEL source not allowed';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE connamespace='seal'::regnamespace
        AND conname='ck_request_event_actor_type' AND pg_get_constraintdef(oid) LIKE '%PUBLIC%') THEN
        RAISE EXCEPTION 'public consent event actor not allowed';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE connamespace='seal'::regnamespace
        AND conname='ck_audit_actor_type' AND pg_get_constraintdef(oid) LIKE '%PUBLIC%') THEN
        RAISE EXCEPTION 'public audit actor not allowed';
    END IF;
END $$;

SELECT COALESCE((
    SELECT script_checksum = :'expected_checksum'
      FROM db_change_history
     WHERE script_name='03_patch/20260824_004_create_import_consent_delivery_tables.sql'
), FALSE) AS checksum_matches \gset

\if :checksum_matches
\else
  \echo 'Phase 3B-5 checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Phase 3B-5 schema verification passed'
