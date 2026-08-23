\set ON_ERROR_STOP on
\encoding UTF8

\if :{?script_checksum}
\else
  \echo 'script_checksum psql variable is required'
  \quit 3
\endif
\if :{?app_version}
\else
  \echo 'app_version psql variable is required'
  \quit 3
\endif

\connect seal_dev

BEGIN;
SET LOCAL ROLE seal_owner;
SET LOCAL search_path = seal, public;

CREATE FUNCTION seal.clear_draft_consent_items(
    p_consent_template_version_id UUID,
    p_actor_id UUID
) RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = seal, pg_temp
AS $$
DECLARE
    removed_item_count INTEGER;
BEGIN
    PERFORM 1
      FROM consent_template_version version
      JOIN consent_template template
        ON template.consent_template_id = version.consent_template_id
      JOIN processing_activity activity
        ON activity.processing_activity_id = template.processing_activity_id
     WHERE version.consent_template_version_id = p_consent_template_version_id
       AND version.workflow_status = 'DRAFT'
       AND EXISTS (
           WITH RECURSIVE visible_scope (organization_unit_id, include_descendants) AS (
               SELECT scope_target_id, include_descendants
                 FROM user_scope
                WHERE admin_user_id = p_actor_id
                  AND scope_type = 'ORGANIZATION'
                  AND active = TRUE
               UNION
               SELECT child.organization_unit_id, TRUE
                 FROM organization_unit child
                 JOIN visible_scope parent_scope
                   ON child.parent_id = parent_scope.organization_unit_id
                WHERE parent_scope.include_descendants = TRUE
           )
           SELECT 1
             FROM visible_scope scope
            WHERE scope.organization_unit_id = activity.owner_organization_unit_id
       )
     FOR UPDATE OF version;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'draft template version is missing or outside actor scope'
            USING ERRCODE = '42501';
    END IF;

    DELETE FROM consent_item_data_selection selection
     USING consent_item_definition item
     WHERE item.consent_item_definition_id = selection.consent_item_definition_id
       AND item.consent_template_version_id = p_consent_template_version_id;

    DELETE FROM consent_item_definition
     WHERE consent_template_version_id = p_consent_template_version_id;
    GET DIAGNOSTICS removed_item_count = ROW_COUNT;

    RETURN removed_item_count;
END
$$;

REVOKE ALL ON FUNCTION seal.clear_draft_consent_items(UUID, UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION seal.clear_draft_consent_items(UUID, UUID) TO seal_app;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260824_002_add_template_draft_replace_function.sql',
    :'script_checksum',
    :'app_version',
    '범위와 초안 상태를 검증하는 동의서 항목 교체 함수 추가'
);

COMMIT;
