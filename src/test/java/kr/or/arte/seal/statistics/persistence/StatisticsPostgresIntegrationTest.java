package kr.or.arte.seal.statistics.persistence;

import kr.or.arte.seal.statistics.application.StatisticsQueryService;
import kr.or.arte.seal.statistics.application.StatisticsSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class StatisticsPostgresIntegrationTest {
    @Autowired StatisticsQueryService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void appliesAdministratorScopeAndReturnsOnlyCountDimensions() {
        UUID noScopeActor = UUID.randomUUID();
        jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash,must_change_password) SELECT ?,?,?,organization_unit_id,password_hash,FALSE FROM seal.admin_user WHERE login_id='seal-admin'",
                noScopeActor, "statistics-noscope-" + noScopeActor.toString().substring(0, 8), "통계 무범위 사용자");

        StatisticsSnapshot snapshot = service.query(noScopeActor, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(snapshot.requests()).isEmpty();
        assertThat(snapshot.responses()).isEmpty();
        assertThat(snapshot.mail()).isEmpty();
        assertThat(snapshot.evidence()).isEmpty();
        assertThat(snapshot.rights()).isEmpty();
        assertThat(snapshot.retention()).isEmpty();
        assertThat(Arrays.stream(StatisticsSnapshot.class.getRecordComponents()).map(c -> c.getName().toLowerCase()))
                .noneMatch(name -> name.contains("recipient") || name.contains("email") || name.contains("name"));
    }
}
