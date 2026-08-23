\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $$
DECLARE
    function_is_security_definer BOOLEAN;
    function_search_path TEXT[];
BEGIN
    SELECT procedure.prosecdef, procedure.proconfig
      INTO function_is_security_definer, function_search_path
      FROM pg_proc procedure
      JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
     WHERE namespace.nspname = 'seal'
       AND procedure.proname = 'clear_draft_consent_items'
       AND pg_get_function_identity_arguments(procedure.oid) =
           'p_consent_template_version_id uuid, p_actor_id uuid';

    IF NOT FOUND OR NOT function_is_security_definer THEN
        RAISE EXCEPTION 'scoped draft replacement function is missing or not security definer';
    END IF;

    IF function_search_path IS NULL
       OR NOT ('search_path=seal, pg_temp' = ANY(function_search_path)) THEN
        RAISE EXCEPTION 'draft replacement function search_path is not fixed';
    END IF;

    IF NOT has_function_privilege(
        'seal_app', 'seal.clear_draft_consent_items(uuid,uuid)', 'EXECUTE') THEN
        RAISE EXCEPTION 'seal_app cannot execute scoped draft replacement function';
    END IF;

    IF has_table_privilege('seal_app', 'seal.consent_item_definition', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.consent_item_data_selection', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app must not directly delete template items';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM seal.db_change_history
         WHERE script_name = '03_patch/20260824_002_add_template_draft_replace_function.sql'
           AND script_checksum ~ '^[0-9a-f]{64}$'
    ) THEN
        RAISE EXCEPTION 'draft replacement patch history is missing';
    END IF;
END
$$;

\echo 'Scoped template draft replacement verification passed'
