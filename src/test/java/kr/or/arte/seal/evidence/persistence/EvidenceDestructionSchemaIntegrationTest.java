package kr.or.arte.seal.evidence.persistence;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

/** Uses only the explicitly configured disposable review database, never application credentials. */
@EnabledIfEnvironmentVariable(named = "SEAL_REVIEW_DB_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:15439/seal_dev.*")
class EvidenceDestructionSchemaIntegrationTest {
    @Test
    void destructionDoesNotInventAFinalizationTime() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("SEAL_REVIEW_DB_URL"), "review_admin", "");
             var sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            // Copies the actual deployed CHECK constraints, without foreign keys or production records.
            sql.execute("CREATE TEMP TABLE evidence_lifecycle_fixture (LIKE seal.evidence_package INCLUDING CONSTRAINTS INCLUDING DEFAULTS) ON COMMIT DROP");
            for (String state : new String[]{"QUEUED", "PROCESSING", "FAILED"}) {
                sql.executeUpdate("INSERT INTO evidence_lifecycle_fixture(evidence_package_id,consent_response_id,consent_request_id,organization_unit_id,generation_status,queued_at) "
                        + "VALUES(gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),gen_random_uuid(),'" + state + "',CURRENT_TIMESTAMP)");
            }
            assertThat(sql.executeUpdate("UPDATE evidence_lifecycle_fixture SET generation_status='DESTROYED',destroyed_at=CURRENT_TIMESTAMP"))
                    .isEqualTo(3);
            try (var rows = sql.executeQuery("SELECT count(*) FROM evidence_lifecycle_fixture WHERE finalized_at IS NULL AND destroyed_at IS NOT NULL")) {
                rows.next();
                assertThat(rows.getInt(1)).isEqualTo(3);
            }
            connection.rollback();
        }
    }
}
