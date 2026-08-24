package kr.or.arte.seal.evidence.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class Phase6SchemaPostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void createsPhase6TablesAndProtectsImmutableRecords() {
        List<String> tables = List.of("evidence_package", "evidence_file", "evidence_verification",
                "rights_request", "rights_request_event", "consent_withdrawal", "retention_anchor_event",
                "legal_hold", "retention_job", "retention_job_event");

        for (String table : tables) {
            assertThat(jdbc.queryForObject("SELECT to_regclass('seal." + table + "') IS NOT NULL", Boolean.class))
                    .as(table).isTrue();
        }
        assertThat(hasPrivilege("evidence_file", "UPDATE")).isFalse();
        assertThat(hasPrivilege("rights_request_event", "DELETE")).isFalse();
        assertThat(hasPrivilege("retention_job_event", "UPDATE")).isFalse();
    }

    private boolean hasPrivilege(String table, String privilege) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT has_table_privilege('seal_app', 'seal." + table + "', '" + privilege + "')",
                Boolean.class));
    }
}
