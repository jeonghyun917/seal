package kr.or.arte.seal.rights.persistence;

import kr.or.arte.seal.recipient.application.CreateRecipientCommand;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.rights.application.RightsRequestService;
import kr.or.arte.seal.rights.domain.RightsRequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class RightsRequestScopePostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RecipientManagementService recipients;
    @Autowired RightsRequestService rights;

    @Test
    void rejectsRecipientOutsideActorsOrganizationScope() {
        UUID root = id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
        UUID system = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
        UUID allowedOrganization = child(root, "RIGHTS-ALLOW");
        UUID deniedOrganization = child(root, "RIGHTS-DENY");
        UUID limitedActor = limitedActor(system, allowedOrganization);
        UUID deniedRecipient = recipients.create(new CreateRecipientCommand(system, deniedOrganization,
                "범위 밖 대상자", "outside-" + UUID.randomUUID() + "@example.com", "권한 경계 테스트"));
        LocalDate today = LocalDate.of(2026, 8, 25);

        assertThatThrownBy(() -> rights.create(allowedOrganization, deniedRecipient, null,
                RightsRequestType.ACCESS, today, today.plusDays(30), "범위 밖 대상자 열람 요청", limitedActor))
                .isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.rights_request WHERE created_by=?",
                Integer.class, limitedActor)).isZero();
    }

    private UUID child(UUID root, String prefix) {
        UUID id = UUID.randomUUID();
        String suffix = id.toString().substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO seal.organization_unit(organization_unit_id,organization_code,organization_name,parent_id) VALUES(?,?,?,?)",
                id, prefix + "-" + suffix, prefix + " 조직", root);
        return id;
    }

    private UUID limitedActor(UUID system, UUID organization) {
        UUID id = UUID.randomUUID();
        String login = "rights-limited-" + id.toString().substring(0, 8);
        jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash,must_change_password,created_by) "
                        + "SELECT ?,?,?,?,password_hash,FALSE,? FROM seal.admin_user WHERE admin_user_id=?",
                id, login, "권리요청 제한 관리자", organization, system, system);
        jdbc.update("INSERT INTO seal.user_scope(user_scope_id,admin_user_id,scope_type,scope_target_id,include_descendants,assigned_by) "
                        + "VALUES(?,?,'ORGANIZATION',?,FALSE,?)",
                UUID.randomUUID(), id, organization, system);
        return id;
    }

    private UUID id(String sql) {
        return jdbc.queryForObject(sql, UUID.class);
    }
}
