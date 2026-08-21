package kr.or.arte.seal.admin.persistence.organization;

import kr.or.arte.seal.admin.application.organization.OrganizationCreateCommand;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.application.organization.OrganizationUpdateCommand;
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
class OrganizationPostgresIntegrationTest {
    @Autowired OrganizationManagement organizationManagement;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsUpdatesAndDeactivatesOrganizationAgainstPostgresql() {
        UUID adminUserId = jdbcTemplate.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID rootOrganizationId = jdbcTemplate.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit = TRUE", UUID.class);
        String code = "IT-ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        UUID organizationId = organizationManagement.create(new OrganizationCreateCommand(
                code, "통합테스트 조직", rootOrganizationId, adminUserId));
        var created = organizationManagement.findVisibleOrganization(adminUserId, organizationId);

        assertThat(created.organizationCode()).isEqualTo(code);
        assertThat(created.active()).isTrue();

        organizationManagement.update(new OrganizationUpdateCommand(organizationId,
                "통합테스트 변경 조직", rootOrganizationId, false, created.versionNo(), adminUserId));

        var updated = organizationManagement.findVisibleOrganization(adminUserId, organizationId);
        assertThat(updated.organizationName()).isEqualTo("통합테스트 변경 조직");
        assertThat(updated.active()).isFalse();
        assertThat(updated.versionNo()).isEqualTo(created.versionNo() + 1);
    }
}
