package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.application.activity.ProcessingActivityCreateCommand;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityManagementService;
import kr.or.arte.seal.policy.application.activity.ProcessingActivityQuery;
import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingDataItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class ProcessingActivityPostgresIntegrationTest {
    @Autowired ProcessingActivityManagementService management;
    @Autowired ProcessingActivityQuery query;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsReviewsAndApprovesActivityWithGeneralItemsAgainstPostgresql() {
        UUID authorId = jdbcTemplate.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit = TRUE", UUID.class);
        UUID reviewerId = createReviewer(authorId, organizationId);
        UUID retentionVersionId = createApprovedRetentionPolicy(authorId, reviewerId, organizationId);
        String code = "IT-PA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        UUID activityId = management.create(new ProcessingActivityCreateCommand(code,
                "통합테스트 처리업무", organizationId,
                "통합테스트 개인정보 처리업무의 참여자를 등록하고 운영하기 위한 목적입니다.",
                LegalBasisType.CONSENT, "정보주체의 개인정보 수집·이용 동의",
                retentionVersionId, List.of(
                        new ProcessingDataItem("성명", "참여자 식별과 출석 확인", true),
                        new ProcessingDataItem("이메일", "교육 일정과 결과 안내", true)), authorId));

        UUID versionId = jdbcTemplate.queryForObject("""
                SELECT processing_activity_version_id FROM seal.processing_activity_version
                 WHERE processing_activity_id = ? AND version_number = 1
                """, UUID.class, activityId);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.processing_data_item
                 WHERE processing_activity_version_id = ? AND data_category = 'GENERAL'
                """, Integer.class, versionId)).isEqualTo(2);

        management.requestReview(activityId, versionId, authorId);
        management.approve(activityId, versionId, reviewerId);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT current_approved_version_id = ? FROM seal.processing_activity
                 WHERE processing_activity_id = ?
                """, Boolean.class, versionId, activityId)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT workflow_status = 'APPROVED' FROM seal.processing_activity_version
                 WHERE processing_activity_version_id = ?
                """, Boolean.class, versionId)).isTrue();
        assertThat(query.findVisibleActivity(authorId, activityId).activityCode()).isEqualTo(code);
        assertThat(query.findVisibleVersions(authorId, activityId)).singleElement()
                .satisfies(version -> assertThat(version.workflowStatus().name()).isEqualTo("APPROVED"));
        assertThat(query.findVisibleDataItems(authorId, activityId)).hasSize(2);
        assertThat(query.findApprovedRetentionPolicies(authorId))
                .anyMatch(option -> option.retentionPolicyVersionId().equals(retentionVersionId));
    }

    private UUID createReviewer(UUID authorId, UUID organizationId) {
        UUID reviewerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.admin_user (admin_user_id, login_id, display_name,
                    organization_unit_id, password_hash, account_status, must_change_password,
                    created_by, updated_by)
                SELECT ?, ?, '통합테스트 검토자', ?, password_hash, 'ACTIVE', FALSE, ?, ?
                  FROM seal.admin_user WHERE admin_user_id = ?
                """, reviewerId, "it-reviewer-" + reviewerId.toString().substring(0, 8),
                organizationId, authorId, authorId, authorId);
        jdbcTemplate.update("""
                INSERT INTO seal.user_scope (user_scope_id, admin_user_id, scope_type,
                    scope_target_id, include_descendants, assigned_by, active)
                VALUES (?, ?, 'ORGANIZATION', ?, TRUE, ?, TRUE)
                """, UUID.randomUUID(), reviewerId, organizationId, authorId);
        return reviewerId;
    }

    private UUID createApprovedRetentionPolicy(UUID authorId, UUID reviewerId, UUID organizationId) {
        UUID policyId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy (retention_policy_id, policy_code, policy_name,
                    owner_organization_unit_id, policy_status, created_by, updated_by)
                VALUES (?, ?, '통합테스트 보유정책', ?, 'ACTIVE', ?, ?)
                """, policyId, "IT-RP-" + policyId.toString().substring(0, 8).toUpperCase(),
                organizationId, authorId, authorId);
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy_version (retention_policy_version_id,
                    retention_policy_id, version_number, workflow_status, start_event,
                    period_value, period_unit, grace_period_days, destruction_method,
                    created_by, review_requested_by, review_requested_at,
                    approved_by, approved_at, updated_by)
                VALUES (?, ?, 1, 'APPROVED', 'CONSENT_SUBMITTED_AT', 3, 'YEAR', 0, 'DELETE',
                    ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                """, versionId, policyId, authorId, authorId, reviewerId, reviewerId);
        jdbcTemplate.update("""
                UPDATE seal.retention_policy SET current_approved_version_id = ?
                 WHERE retention_policy_id = ?
                """, versionId, policyId);
        return versionId;
    }
}
