package kr.or.arte.seal.admin.persistence.role;

import kr.or.arte.seal.admin.application.role.RoleCreateCommand;
import kr.or.arte.seal.admin.application.role.RoleManagement;
import kr.or.arte.seal.admin.application.role.RoleUpdateCommand;
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
class RoleManagementPostgresIntegrationTest {
    @Autowired RoleManagement management;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsUpdatesRevokesAndRestoresPermissionsAgainstPostgresql() {
        UUID actorId = jdbcTemplate.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID roleViewId = permissionId("ROLE_VIEW");
        UUID auditViewId = permissionId("AUDIT_VIEW");
        String roleCode = "IT_ROLE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        UUID roleId = management.create(new RoleCreateCommand(
                roleCode, "통합테스트 역할", Set.of(roleViewId, auditViewId), actorId));
        var created = management.findRole(roleId);
        assertThat(management.findAssignedPermissionIds(roleId))
                .containsExactlyInAnyOrder(roleViewId, auditViewId);

        management.update(new RoleUpdateCommand(roleId, "통합테스트 변경 역할", true,
                Set.of(roleViewId), created.versionNo(), actorId));
        var updated = management.findRole(roleId);
        assertThat(updated.roleName()).isEqualTo("통합테스트 변경 역할");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.role_permission
                 WHERE role_id = ? AND permission_id = ? AND active = FALSE
                   AND revoked_at IS NOT NULL AND revoked_by = ?
                """, Integer.class, roleId, auditViewId, actorId)).isEqualTo(1);

        management.update(new RoleUpdateCommand(roleId, updated.roleName(), true,
                Set.of(roleViewId, auditViewId), updated.versionNo(), actorId));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.role_permission
                 WHERE role_id = ? AND active = TRUE
                   AND revoked_at IS NULL AND revoked_by IS NULL
                """, Integer.class, roleId)).isEqualTo(2);
    }

    private UUID permissionId(String permissionCode) {
        return jdbcTemplate.queryForObject(
                "SELECT permission_id FROM seal.permission WHERE permission_code = ?",
                UUID.class, permissionCode);
    }
}
