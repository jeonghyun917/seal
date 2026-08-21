package kr.or.arte.seal.admin.persistence.user;

import kr.or.arte.seal.admin.application.user.AdminUserCreateCommand;
import kr.or.arte.seal.admin.application.user.AdminUserManagement;
import kr.or.arte.seal.admin.application.user.AdminUserUpdateCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class AdminUserManagementPostgresIntegrationTest {
    @Autowired AdminUserManagement management;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsAssignsUpdatesAndUnlocksAdministratorAgainstPostgresql() {
        UUID actorId = jdbcTemplate.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID organizationId = jdbcTemplate.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit = TRUE", UUID.class);
        UUID roleId = jdbcTemplate.queryForObject(
                "SELECT role_id FROM seal.admin_role WHERE role_code = 'OPERATOR'", UUID.class);
        UUID secondRoleId = jdbcTemplate.queryForObject(
                "SELECT role_id FROM seal.admin_role WHERE role_code = 'AUDITOR'", UUID.class);
        UUID childOrganizationId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.organization_unit (organization_unit_id, organization_code,
                    organization_name, parent_id, root_unit, active)
                VALUES (?, ?, '통합테스트 하위 조직', ?, FALSE, TRUE)
                """, childOrganizationId,
                "IT-UORG-" + childOrganizationId.toString().substring(0, 8).toUpperCase(),
                organizationId);
        String loginId = "it-admin-" + UUID.randomUUID().toString().substring(0, 8);

        UUID userId = management.create(new AdminUserCreateCommand(loginId, "통합 관리자",
                organizationId, "temporary-123!", Set.of(roleId, secondRoleId),
                Set.of(organizationId, childOrganizationId), actorId));

        var created = management.findVisibleUser(actorId, userId);
        assertThat(created.loginId()).isEqualTo(loginId);
        assertThat(management.findAssignedRoleIds(actorId, userId)).containsExactlyInAnyOrder(roleId, secondRoleId);
        assertThat(management.findAssignedOrganizationScopeIds(actorId, userId))
                .containsExactlyInAnyOrder(organizationId, childOrganizationId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash LIKE '$2%' FROM seal.admin_user WHERE admin_user_id = ?",
                Boolean.class, userId)).isTrue();

        management.update(new AdminUserUpdateCommand(userId, "통합 변경 관리자",
                organizationId, "LOCKED", Set.of(roleId), Set.of(organizationId),
                created.versionNo(), actorId));
        var locked = management.findVisibleUser(actorId, userId);
        assertThat(locked.accountStatus()).isEqualTo("LOCKED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM seal.user_role WHERE admin_user_id = ? AND active = FALSE",
                Integer.class, userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM seal.user_scope WHERE admin_user_id = ? AND active = FALSE",
                Integer.class, userId)).isEqualTo(1);

        management.update(new AdminUserUpdateCommand(userId, "통합 변경 관리자",
                organizationId, "LOCKED", Set.of(roleId, secondRoleId),
                Set.of(organizationId, childOrganizationId), locked.versionNo(), actorId));
        var reassigned = management.findVisibleUser(actorId, userId);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.user_role
                 WHERE admin_user_id = ? AND active = TRUE AND revoked_at IS NULL AND revoked_by IS NULL
                """, Integer.class, userId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.user_scope
                 WHERE admin_user_id = ? AND active = TRUE AND revoked_at IS NULL AND revoked_by IS NULL
                """, Integer.class, userId)).isEqualTo(2);

        management.unlock(userId, actorId);
        var unlocked = management.findVisibleUser(actorId, userId);
        assertThat(unlocked.accountStatus()).isEqualTo("ACTIVE");
        assertThat(unlocked.consecutiveFailures()).isZero();
    }
}
