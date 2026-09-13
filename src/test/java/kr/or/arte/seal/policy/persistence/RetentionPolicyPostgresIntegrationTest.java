package kr.or.arte.seal.policy.persistence;

import kr.or.arte.seal.policy.application.retention.RetentionPolicyCreateCommand;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyManagementService;
import kr.or.arte.seal.policy.application.retention.RetentionPolicyQuery;
import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class RetentionPolicyPostgresIntegrationTest {
    @Autowired RetentionPolicyManagementService managementService;
    @Autowired RetentionPolicyQuery query;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsRequestsReviewAndApprovesWithDifferentAdministratorAgainstPostgresql() {
        UUID adminUserId = jdbcTemplate.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit = TRUE", UUID.class);
        UUID reviewerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.admin_user (admin_user_id, login_id, display_name,
                    organization_unit_id, password_hash, must_change_password, created_by, updated_by)
                SELECT ?, ?, '통합테스트 승인자', ?, password_hash, FALSE, ?, ?
                  FROM seal.admin_user
                 WHERE admin_user_id = ?
                """, reviewerId, "it-reviewer-" + reviewerId.toString().substring(0, 8),
                organizationId, adminUserId, adminUserId, adminUserId);
        jdbcTemplate.update("""
                INSERT INTO seal.user_scope (user_scope_id, admin_user_id, scope_type,
                    scope_target_id, include_descendants, assigned_by)
                VALUES (?, ?, 'ORGANIZATION', ?, TRUE, ?)
                """, UUID.randomUUID(), reviewerId, organizationId, adminUserId);

        UUID policyId = managementService.create(new RetentionPolicyCreateCommand(
                "통합테스트 보유정책", organizationId,
                RetentionStartEvent.CONSENT_SUBMITTED_AT, 3, RetentionPeriodUnit.YEAR,
                30, DestructionMethod.DELETE, adminUserId));

        assertThat(query.findVisiblePolicy(adminUserId, policyId).policyCode())
                .matches("RP-[A-F0-9]{16}");
        var versions = query.findVisibleVersions(adminUserId, policyId);
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).workflowStatus()).isEqualTo("DRAFT");

        managementService.requestReview(policyId, versions.get(0).retentionPolicyVersionId(), adminUserId);

        assertThat(query.findVisibleVersions(adminUserId, policyId).get(0).workflowStatus())
                .isEqualTo("REVIEW_REQUESTED");

        managementService.approve(policyId, versions.get(0).retentionPolicyVersionId(), reviewerId);

        assertThat(query.findVisibleVersions(reviewerId, policyId).get(0).workflowStatus())
                .isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT current_approved_version_id = ?
                  FROM seal.retention_policy
                 WHERE retention_policy_id = ?
                """, Boolean.class, versions.get(0).retentionPolicyVersionId(), policyId)).isTrue();
    }
}
