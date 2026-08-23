package kr.or.arte.seal.template.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class ConsentTemplateSchemaPostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsTemplateTablesAndPermissions() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE table_schema = 'seal'
                   AND table_name IN (
                       'consent_template',
                       'consent_template_version',
                       'consent_item_definition',
                       'consent_item_data_selection',
                       'template_review'
                   )
                """, Integer.class);

        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM seal.permission
                 WHERE permission_code IN (
                       'CONSENT_TEMPLATE_VIEW',
                       'CONSENT_TEMPLATE_EDIT',
                       'CONSENT_TEMPLATE_REVIEW',
                       'CONSENT_TEMPLATE_APPROVE',
                       'CONSENT_TEMPLATE_PUBLISH'
                 )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(5);
        assertThat(permissionCount).isEqualTo(5);
    }

    @Test
    void appliesLeastPrivilegeForTemplateTables() {
        assertThat(hasTablePrivilege("consent_template", "SELECT,INSERT,UPDATE")).isTrue();
        assertThat(hasTablePrivilege("consent_template", "DELETE")).isFalse();
        assertThat(hasTablePrivilege("consent_item_definition", "DELETE")).isFalse();
        assertThat(hasTablePrivilege("consent_item_data_selection", "DELETE")).isFalse();
        assertThat(hasTablePrivilege("template_review", "SELECT,INSERT")).isTrue();
        assertThat(hasTablePrivilege("template_review", "UPDATE,DELETE")).isFalse();
    }

    @Test
    void exposesOnlyScopedDraftItemReplacementFunction() {
        Boolean canExecute = jdbcTemplate.queryForObject("""
                SELECT has_function_privilege(
                    'seal_app',
                    'seal.clear_draft_consent_items(uuid,uuid)',
                    'EXECUTE')
                """, Boolean.class);

        assertThat(canExecute).isTrue();
    }

    @Test
    void rejectsProcessingActivityVersionFromAnotherActivity() {
        UUID actorId = UUID.randomUUID();
        UUID retentionPolicyId = UUID.randomUUID();
        UUID retentionVersionId = UUID.randomUUID();
        UUID firstActivityId = UUID.randomUUID();
        UUID firstActivityVersionId = UUID.randomUUID();
        UUID secondActivityId = UUID.randomUUID();
        UUID secondActivityVersionId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        insertActor(actorId);
        insertRetentionPolicy(retentionPolicyId, retentionVersionId, actorId);
        insertProcessingActivity(firstActivityId, firstActivityVersionId,
                retentionVersionId, actorId, "ACT-A-" + compactUpper(actorId));
        insertProcessingActivity(secondActivityId, secondActivityVersionId,
                retentionVersionId, actorId, "ACT-B-" + compactUpper(actorId));

        jdbcTemplate.update("""
                INSERT INTO seal.consent_template (
                    consent_template_id, template_code, template_name,
                    processing_activity_id, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, templateId, "TPL-" + compactUpper(actorId), "테스트 동의서 템플릿",
                firstActivityId, actorId, actorId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO seal.consent_template_version (
                    consent_template_version_id, consent_template_id,
                    processing_activity_id, processing_activity_version_id,
                    version_number, title, introduction_html, created_by, updated_by
                ) VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?)
                """, UUID.randomUUID(), templateId, firstActivityId,
                secondActivityVersionId, "개인정보 수집 이용 동의서",
                "<p>개인정보 처리 내용을 확인해 주세요.</p>", actorId, actorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private boolean hasTablePrivilege(String table, String privileges) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT has_table_privilege('seal_app', ?, ?)",
                Boolean.class, "seal." + table, privileges));
    }

    private void insertActor(UUID actorId) {
        jdbcTemplate.update("""
                INSERT INTO seal.admin_user (
                    admin_user_id, login_id, display_name, organization_unit_id,
                    password_hash, must_change_password
                ) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', repeat('x', 60), FALSE)
                """, actorId, "schema-" + compact(actorId), "스키마 테스트 관리자");
    }

    private void insertRetentionPolicy(UUID policyId, UUID versionId, UUID actorId) {
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy (
                    retention_policy_id, policy_code, policy_name,
                    owner_organization_unit_id, created_by, updated_by
                ) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', ?, ?)
                """, policyId, "RP-" + compactUpper(actorId), "테스트 보유정책", actorId, actorId);
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy_version (
                    retention_policy_version_id, retention_policy_id, version_number,
                    workflow_status, start_event, period_value, period_unit,
                    destruction_method, created_by, updated_by
                ) VALUES (?, ?, 1, 'APPROVED', 'CONSENT_SUBMITTED_AT', 3, 'YEAR',
                          'DELETE', ?, ?)
                """, versionId, policyId, actorId, actorId);
    }

    private void insertProcessingActivity(UUID activityId, UUID activityVersionId,
                                          UUID retentionVersionId, UUID actorId,
                                          String activityCode) {
        jdbcTemplate.update("""
                INSERT INTO seal.processing_activity (
                    processing_activity_id, activity_code, activity_name,
                    owner_organization_unit_id, created_by, updated_by
                ) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', ?, ?)
                """, activityId, activityCode, "테스트 개인정보 처리업무", actorId, actorId);
        jdbcTemplate.update("""
                INSERT INTO seal.processing_activity_version (
                    processing_activity_version_id, processing_activity_id, version_number,
                    workflow_status, processing_purpose, legal_basis_type,
                    legal_basis_detail, retention_policy_version_id, created_by, updated_by
                ) VALUES (?, ?, 1, 'APPROVED', ?, 'CONSENT', ?, ?, ?, ?)
                """, activityVersionId, activityId, "테스트 동의서 작성 및 운영 목적",
                "정보주체의 개인정보 수집 이용 동의", retentionVersionId, actorId, actorId);
    }

    private String compact(UUID id) {
        return id.toString().replace("-", "").substring(0, 12);
    }

    private String compactUpper(UUID id) {
        return compact(id).toUpperCase();
    }
}
