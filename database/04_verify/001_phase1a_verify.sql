\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $verify$
DECLARE
    missing_tables TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'seal_owner' AND NOT rolcanlogin
    ) THEN
        RAISE EXCEPTION 'seal_owner must exist with NOLOGIN';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'seal_app' AND rolcanlogin
          AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
    ) THEN
        RAISE EXCEPTION 'seal_app must be a limited LOGIN role';
    END IF;

    IF (SELECT pg_get_userbyid(datdba) FROM pg_catalog.pg_database WHERE datname = 'seal_dev')
       IS DISTINCT FROM 'seal_owner' THEN
        RAISE EXCEPTION 'seal_dev owner must be seal_owner';
    END IF;

    SELECT string_agg(required_table, ', ' ORDER BY required_table)
      INTO missing_tables
      FROM unnest(ARRAY[
          'db_change_history', 'organization_unit', 'admin_user', 'admin_role',
          'permission', 'user_role', 'role_permission', 'user_scope', 'audit_event'
      ]) AS required_table
     WHERE to_regclass('seal.' || required_table) IS NULL;

    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'required Phase 1A tables missing: %', missing_tables;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM pg_catalog.pg_class c
          JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'seal'
           AND c.relkind IN ('r', 'p', 'S')
           AND pg_get_userbyid(c.relowner) <> 'seal_owner'
    ) THEN
        RAISE EXCEPTION 'all seal tables and sequences must be owned by seal_owner';
    END IF;

    IF NOT has_schema_privilege('seal_app', 'seal', 'USAGE')
       OR has_schema_privilege('seal_app', 'seal', 'CREATE') THEN
        RAISE EXCEPTION 'seal_app must have schema USAGE and no CREATE';
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.admin_user', 'SELECT,INSERT,UPDATE')
       OR has_table_privilege('seal_app', 'seal.admin_user', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app admin_user DML privileges differ from the approved model';
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.audit_event', 'SELECT,INSERT')
       OR has_table_privilege('seal_app', 'seal.audit_event', 'UPDATE')
       OR has_table_privilege('seal_app', 'seal.audit_event', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.audit_event', 'TRUNCATE') THEN
        RAISE EXCEPTION 'audit_event must be append-only for seal_app';
    END IF;

    IF (SELECT count(*)
          FROM seal.db_change_history
         WHERE script_name IN (
             '01_schema/001_create_schema.sql',
             '01_schema/002_create_core_tables.sql',
             '02_data/001_common_codes.sql'
         )) <> 3 THEN
        RAISE EXCEPTION 'expected all three initial change-history rows';
    END IF;

    IF EXISTS (
        SELECT 1 FROM seal.db_change_history
        WHERE script_checksum !~ '^[0-9a-f]{64}$'
           OR length(btrim(application_version)) = 0
    ) THEN
        RAISE EXCEPTION 'change history checksum or application version is invalid';
    END IF;
END
$verify$;

SELECT 'Phase 1A database verification passed' AS verification_result;
