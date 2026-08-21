\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'role_permission' AND column_name = 'active'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'role_permission' AND column_name = 'revoked_at'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'role_permission' AND column_name = 'revoked_by'
    ) THEN
        RAISE EXCEPTION 'role permission lifecycle columns are missing';
    END IF;

    IF has_table_privilege('seal_app', 'seal.role_permission', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app must not delete role permission assignments';
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.role_permission', 'SELECT,INSERT,UPDATE') THEN
        RAISE EXCEPTION 'seal_app role permission lifecycle privileges are incomplete';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM seal.db_change_history
         WHERE script_name = '03_patch/20260821_003_add_role_permission_lifecycle.sql'
    ) THEN
        RAISE EXCEPTION 'role permission lifecycle patch history is missing';
    END IF;
END
$$;

\echo 'Role permission lifecycle verification passed'
