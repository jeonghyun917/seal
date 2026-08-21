\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'user_role' AND column_name = 'active'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'seal' AND table_name = 'user_scope' AND column_name = 'active'
    ) THEN
        RAISE EXCEPTION 'assignment lifecycle columns are missing';
    END IF;

    IF has_table_privilege('seal_app', 'seal.user_role', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.user_scope', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app must not delete role or scope assignments';
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.user_role', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.user_scope', 'SELECT,INSERT,UPDATE') THEN
        RAISE EXCEPTION 'seal_app assignment lifecycle privileges are incomplete';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM seal.db_change_history
         WHERE script_name = '03_patch/20260821_002_add_assignment_lifecycle.sql'
    ) THEN
        RAISE EXCEPTION 'assignment lifecycle patch history is missing';
    END IF;
END
$$;

\echo 'Assignment lifecycle verification passed'
