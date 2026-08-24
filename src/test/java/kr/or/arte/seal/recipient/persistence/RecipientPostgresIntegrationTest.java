package kr.or.arte.seal.recipient.persistence;

import kr.or.arte.seal.recipient.application.CreateRecipientCommand;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
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
class RecipientPostgresIntegrationTest {
    @Autowired RecipientManagementService management;
    @Autowired RecipientQueryService query;
    @Autowired JdbcTemplate jdbc;

    @Test
    void storesEncryptedRecipientAndReturnsOnlyMaskedQueryValues() {
        UUID actorId = jdbc.queryForObject(
                "SELECT admin_user_id FROM seal.admin_user WHERE login_id = 'seal-admin'", UUID.class);
        UUID organizationId = jdbc.queryForObject(
                "SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit = TRUE", UUID.class);
        String email = "recipient-" + UUID.randomUUID() + "@example.com";

        UUID recipientId = management.create(new CreateRecipientCommand(actorId, organizationId,
                "통합테스트 대상자", email, "통합테스트에서 직접 수집"));

        byte[] ciphertext = jdbc.queryForObject(
                "SELECT recipient_email_ciphertext FROM seal.recipient WHERE recipient_id = ?",
                byte[].class, recipientId);
        assertThat(new String(ciphertext)).doesNotContain(email);
        assertThat(query.find(actorId, recipientId).maskedEmail()).endsWith("@e******.com");
    }
}
