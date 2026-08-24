package kr.or.arte.seal.request.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class RecipientRequestSchemaPostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsAllPhase3aTablesAndPermissions() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE table_schema = 'seal'
                   AND table_name IN (
                       'recipient', 'consent_request', 'consent_request_snapshot',
                       'consent_request_item', 'consent_request_item_data',
                       'consent_request_event', 'secure_link'
                   )
                """, Integer.class);
        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM seal.permission
                 WHERE permission_code IN (
                       'RECIPIENT_VIEW', 'RECIPIENT_EDIT',
                       'CONSENT_REQUEST_VIEW', 'CONSENT_REQUEST_EDIT',
                       'CONSENT_REQUEST_ACTIVATE', 'CONSENT_REQUEST_CANCEL',
                       'CONSENT_REQUEST_LINK_REISSUE'
                 )
                """, Integer.class);

        assertThat(tableCount).isEqualTo(7);
        assertThat(permissionCount).isEqualTo(7);
    }

    @Test
    void appliesLeastPrivilegeAndHasNoRawTokenColumn() {
        String recipientTable = jdbcTemplate.queryForObject(
                "SELECT to_regclass('seal.recipient')::text", String.class);
        assertThat(recipientTable).isEqualTo("recipient");
        assertThat(hasTablePrivilege("recipient", "SELECT,INSERT,UPDATE")).isTrue();
        assertThat(hasTablePrivilege("recipient", "DELETE,TRUNCATE")).isFalse();
        assertThat(hasTablePrivilege("consent_request", "SELECT,INSERT,UPDATE")).isTrue();
        assertThat(hasTablePrivilege("consent_request_snapshot", "SELECT,INSERT")).isTrue();
        assertThat(hasTablePrivilege("consent_request_snapshot", "UPDATE,DELETE")).isFalse();
        assertThat(hasTablePrivilege("consent_request_event", "UPDATE,DELETE")).isFalse();
        Integer rawTokenColumns = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM information_schema.columns
                 WHERE table_schema = 'seal'
                   AND table_name = 'secure_link'
                   AND column_name IN ('token', 'raw_token', 'token_plaintext')
                """, Integer.class);
        assertThat(rawTokenColumns).isZero();
    }

    private boolean hasTablePrivilege(String table, String privileges) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT has_table_privilege('seal_app', ?, ?)", Boolean.class,
                "seal." + table, privileges));
    }
}
