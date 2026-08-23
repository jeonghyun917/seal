\set ON_ERROR_STOP on
\encoding UTF8
\connect seal_dev

DO $$
DECLARE
    template_table_count INTEGER;
    template_permission_count INTEGER;
    system_admin_permission_count INTEGER;
BEGIN
    SELECT count(*)
      INTO template_table_count
      FROM information_schema.tables
     WHERE table_schema = 'seal'
       AND table_name IN (
           'consent_template',
           'consent_template_version',
           'consent_item_definition',
           'consent_item_data_selection',
           'template_review'
       );
    IF template_table_count <> 5 THEN
        RAISE EXCEPTION 'consent template tables are incomplete: %', template_table_count;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_template_version_template_activity'
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_template_version_processing_activity'
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_selection_data_version'
    ) OR NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_template_current_published_version'
    ) THEN
        RAISE EXCEPTION 'consent template cross-object foreign keys are incomplete';
    END IF;

    SELECT count(*)
      INTO template_permission_count
      FROM seal.permission
     WHERE permission_code IN (
           'CONSENT_TEMPLATE_VIEW',
           'CONSENT_TEMPLATE_EDIT',
           'CONSENT_TEMPLATE_REVIEW',
           'CONSENT_TEMPLATE_APPROVE',
           'CONSENT_TEMPLATE_PUBLISH'
     );
    IF template_permission_count <> 5 THEN
        RAISE EXCEPTION 'consent template permissions are incomplete: %', template_permission_count;
    END IF;

    SELECT count(*)
      INTO system_admin_permission_count
      FROM seal.role_permission rp
      JOIN seal.admin_role r ON r.role_id = rp.role_id
      JOIN seal.permission p ON p.permission_id = rp.permission_id
     WHERE r.role_code = 'SYSTEM_ADMIN'
       AND p.permission_code LIKE 'CONSENT_TEMPLATE_%'
       AND rp.active;
    IF system_admin_permission_count <> 5 THEN
        RAISE EXCEPTION 'system administrator template permissions are incomplete: %',
            system_admin_permission_count;
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.consent_template', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.consent_template_version', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.consent_item_definition', 'SELECT,INSERT,UPDATE')
       OR NOT has_table_privilege('seal_app', 'seal.consent_item_data_selection', 'SELECT,INSERT,UPDATE') THEN
        RAISE EXCEPTION 'seal_app template DML privileges are incomplete';
    END IF;

    IF has_table_privilege('seal_app', 'seal.consent_template', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.consent_template_version', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.consent_item_definition', 'DELETE')
       OR has_table_privilege('seal_app', 'seal.consent_item_data_selection', 'DELETE') THEN
        RAISE EXCEPTION 'seal_app must not delete consent template data';
    END IF;

    IF NOT has_table_privilege('seal_app', 'seal.template_review', 'SELECT,INSERT')
       OR has_table_privilege('seal_app', 'seal.template_review', 'UPDATE')
       OR has_table_privilege('seal_app', 'seal.template_review', 'DELETE') THEN
        RAISE EXCEPTION 'template review append-only privileges are invalid';
    END IF;

    IF has_schema_privilege('seal_app', 'seal', 'CREATE') THEN
        RAISE EXCEPTION 'seal_app must not create objects in seal schema';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM seal.db_change_history
         WHERE script_name = '03_patch/20260824_001_create_consent_template_tables.sql'
           AND script_checksum ~ '^[0-9a-f]{64}$'
    ) THEN
        RAISE EXCEPTION 'consent template patch history is missing';
    END IF;
END
$$;

\echo 'Consent template schema verification passed'
