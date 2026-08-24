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
    expected_tables TEXT[] := ARRAY[
        'recipient', 'consent_request', 'consent_request_snapshot',
        'consent_request_item', 'consent_request_item_data',
        'consent_request_event', 'secure_link'
    ];
    phase_table_name TEXT;
    immutable_table TEXT;
    expected_constraint TEXT;
    table_owner TEXT;
BEGIN
    FOREACH phase_table_name IN ARRAY expected_tables LOOP
        IF to_regclass('seal.' || phase_table_name) IS NULL THEN
            RAISE EXCEPTION 'missing Phase 3A table: %', phase_table_name;
        END IF;

        SELECT tableowner INTO table_owner
          FROM pg_tables
         WHERE schemaname = 'seal' AND tablename = phase_table_name;
        IF table_owner <> 'seal_owner' THEN
            RAISE EXCEPTION 'wrong owner for %, expected seal_owner, got %',
                phase_table_name, table_owner;
        END IF;

        IF has_table_privilege('seal_app', 'seal.' || phase_table_name, 'DELETE')
           OR has_table_privilege('seal_app', 'seal.' || phase_table_name, 'TRUNCATE') THEN
            RAISE EXCEPTION 'seal_app has destructive privilege on %', phase_table_name;
        END IF;
    END LOOP;

    IF NOT has_table_privilege('seal_app', 'seal.recipient', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.consent_request', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.secure_link', 'SELECT,INSERT,UPDATE') THEN
        RAISE EXCEPTION 'seal_app current-state DML privilege is incomplete';
    END IF;

    FOREACH immutable_table IN ARRAY ARRAY[
        'consent_request_snapshot', 'consent_request_item',
        'consent_request_item_data', 'consent_request_event'
    ] LOOP
        IF NOT has_table_privilege('seal_app', 'seal.' || immutable_table, 'SELECT,INSERT')
           OR has_table_privilege('seal_app', 'seal.' || immutable_table, 'UPDATE') THEN
            RAISE EXCEPTION 'immutable table privilege is wrong for %', immutable_table;
        END IF;
    END LOOP;

    FOREACH expected_constraint IN ARRAY ARRAY[
        'fk_request_recipient_organization',
        'fk_request_processing_activity',
        'fk_request_processing_retention',
        'fk_request_template_activity',
        'fk_request_template_version',
        'fk_request_template_processing_version',
        'fk_request_snapshot',
        'fk_request_current_link'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
             WHERE connamespace = 'seal'::regnamespace
               AND conname = expected_constraint
               AND contype = 'f'
        ) THEN
            RAISE EXCEPTION 'missing Phase 3A composite foreign key: %', expected_constraint;
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
         WHERE schemaname = 'seal' AND indexname = 'ux_recipient_active_email'
    ) THEN
        RAISE EXCEPTION 'active recipient email uniqueness index is missing';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
         WHERE schemaname = 'seal' AND indexname = 'ux_secure_link_current_per_request'
    ) THEN
        RAISE EXCEPTION 'current secure-link uniqueness index is missing';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'secure_link'
           AND column_name IN ('token', 'raw_token', 'token_plaintext')
    ) THEN
        RAISE EXCEPTION 'raw secure-link token column must not exist';
    END IF;

    IF (SELECT count(*) FROM permission WHERE permission_code IN (
            'RECIPIENT_VIEW', 'RECIPIENT_EDIT', 'CONSENT_REQUEST_VIEW',
            'CONSENT_REQUEST_EDIT', 'CONSENT_REQUEST_ACTIVATE',
            'CONSENT_REQUEST_CANCEL', 'CONSENT_REQUEST_LINK_REISSUE')) <> 7 THEN
        RAISE EXCEPTION 'Phase 3A permission count is not 7';
    END IF;

    IF (SELECT count(*)
          FROM role_permission rp
          JOIN admin_role r ON r.role_id = rp.role_id
          JOIN permission p ON p.permission_id = rp.permission_id
         WHERE r.role_code = 'SYSTEM_ADMIN' AND r.active = TRUE
           AND rp.active = TRUE
           AND p.permission_code IN (
               'RECIPIENT_VIEW', 'RECIPIENT_EDIT', 'CONSENT_REQUEST_VIEW',
               'CONSENT_REQUEST_EDIT', 'CONSENT_REQUEST_ACTIVATE',
               'CONSENT_REQUEST_CANCEL', 'CONSENT_REQUEST_LINK_REISSUE')) <> 7 THEN
        RAISE EXCEPTION 'SYSTEM_ADMIN does not have all Phase 3A permissions';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM db_change_history
         WHERE script_name = '03_patch/20260824_003_create_recipient_request_tables.sql'
           AND script_checksum ~ '^[0-9a-f]{64}$'
    ) THEN
        RAISE EXCEPTION 'Phase 3A change history is missing';
    END IF;
END
$$;

SELECT COALESCE((
    SELECT script_checksum = :'expected_checksum'
      FROM db_change_history
     WHERE script_name = '03_patch/20260824_003_create_recipient_request_tables.sql'
), FALSE) AS checksum_matches \gset

\if :checksum_matches
\else
  \echo 'Phase 3A checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Phase 3A recipient request verification passed'
