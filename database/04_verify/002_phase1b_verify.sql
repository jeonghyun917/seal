\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $$
DECLARE
    required_table TEXT;
BEGIN
    FOREACH required_table IN ARRAY ARRAY[
        'retention_policy',
        'retention_policy_version',
        'processing_activity',
        'processing_activity_version',
        'processing_data_item',
        'user_processing_activity_scope'
    ] LOOP
        IF to_regclass('seal.' || required_table) IS NULL THEN
            RAISE EXCEPTION 'required Phase 1B table is missing: %', required_table;
        END IF;
    END LOOP;

    IF NOT has_table_privilege('seal_app', 'seal.retention_policy', 'SELECT,INSERT,UPDATE') THEN
        RAISE EXCEPTION 'seal_app policy DML privileges are missing';
    END IF;

    IF has_table_privilege('seal_app', 'seal.retention_policy', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app must not delete retention policies';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM seal.db_change_history
         WHERE script_name = '03_patch/20260821_001_create_policy_tables.sql'
    ) THEN
        RAISE EXCEPTION 'Phase 1B patch history is missing';
    END IF;
END
$$;

\echo 'Phase 1B database verification passed'
