package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.AesGcmPersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.EmailNormalizer;
import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.HmacEmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipientManagementServiceTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-24T03:00:00Z");

    private FakeRepository repository;
    private PersonalDataCipher cipher;
    private HmacEmailLookupHasher hasher;
    private List<AuditEventCommand> audits;
    private RecipientManagementService service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        cipher = new AesGcmPersonalDataCipher(Map.of("v1",
                new SecretKeySpec(new byte[32], "AES")), "v1", new SecureRandom());
        hasher = new HmacEmailLookupHasher(new byte[32]);
        audits = new ArrayList<>();
        AuditEventWriter auditWriter = audits::add;
        service = new RecipientManagementService(repository, cipher, hasher, auditWriter,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createEncryptsPersonalDataAndWritesPiiFreeAudit() {
        UUID id = service.create(new CreateRecipientCommand(ACTOR_ID, ORG_ID,
                "홍길동", " Hong.Gil@Example.COM ", "교육 참여 신청서 수집"));

        RecipientStoredRecord stored = repository.rows.get(id);
        assertThat(new String(stored.nameCiphertext())).doesNotContain("홍길동");
        assertThat(new String(stored.emailCiphertext())).doesNotContain("Hong.Gil");
        assertThat(stored.emailLookupHmac()).isEqualTo(hasher.hmac("hong.gil@example.com"));
        assertThat(stored.status()).isEqualTo(RecipientStatus.ACTIVE);
        assertThat(audits).singleElement().satisfies(audit -> {
            assertThat(audit.actionCode()).isEqualTo("RECIPIENT_CREATE");
            assertThat(audit.metadata().toString()).doesNotContain("홍길동", "example.com");
        });
    }

    @Test
    void duplicateActiveEmailIsRejectedWithoutExposingPlainEmail() {
        service.create(new CreateRecipientCommand(ACTOR_ID, ORG_ID,
                "기존", "same@example.com", "교육 참여 신청서 수집"));

        assertThatThrownBy(() -> service.create(new CreateRecipientCommand(ACTOR_ID, ORG_ID,
                "중복", "SAME@example.com", "교육 참여 신청서 수집")))
                .isInstanceOf(DuplicateRecipientException.class)
                .hasMessageNotContaining("same@example.com");
    }

    @Test
    void updateReencryptsBothFieldsWithActiveKeyAndPreservesBlankInput() {
        UUID id = service.create(new CreateRecipientCommand(ACTOR_ID, ORG_ID,
                "홍길동", "old@example.com", "교육 참여 신청서 수집"));

        service.update(id, new UpdateRecipientCommand(ACTOR_ID, "홍길순", " ",
                "교육 이수자 명단 정정", 0));

        RecipientStoredRecord stored = repository.rows.get(id);
        assertThat(decrypt(stored, "name", stored.nameCiphertext())).isEqualTo("홍길순");
        assertThat(decrypt(stored, "email", stored.emailCiphertext())).isEqualTo("old@example.com");
        assertThat(stored.versionNo()).isEqualTo(1);
    }

    @Test
    void queryMasksValuesOnlyAfterScopedRecordLookup() {
        UUID id = service.create(new CreateRecipientCommand(ACTOR_ID, ORG_ID,
                "홍길동", "hong@example.com", "교육 참여 신청서 수집"));
        RecipientQueryService query = new RecipientQueryService(repository, cipher, hasher);

        RecipientDetail detail = query.find(ACTOR_ID, id);

        assertThat(detail.maskedName()).isEqualTo("홍*동");
        assertThat(detail.maskedEmail()).isEqualTo("h***@e******.com");
    }

    private String decrypt(RecipientStoredRecord stored, String field, byte[] value) {
        return cipher.decrypt(new PersonalDataContext("RECIPIENT",
                        stored.recipientId(), stored.organizationUnitId(), field),
                new EncryptedValue(stored.encryptionKeyId(), value));
    }

    private static final class FakeRepository implements RecipientRepository {
        private final Map<UUID, RecipientStoredRecord> rows = new HashMap<>();

        @Override
        public boolean canAccessActiveOrganization(UUID actorId, UUID organizationId) {
            return ACTOR_ID.equals(actorId) && ORG_ID.equals(organizationId);
        }

        @Override
        public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
                String emailLookupHmac, UUID excludedRecipientId) {
            return rows.values().stream().filter(row -> row.organizationUnitId().equals(organizationId))
                    .filter(row -> row.status() == RecipientStatus.ACTIVE)
                    .filter(row -> row.emailLookupHmac().equals(emailLookupHmac))
                    .filter(row -> excludedRecipientId == null || !row.recipientId().equals(excludedRecipientId))
                    .findFirst();
        }

        @Override
        public void insert(RecipientStoredRecord record, UUID actorId, Instant now) {
            rows.put(record.recipientId(), record);
        }

        @Override
        public Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId) {
            return ACTOR_ID.equals(actorId) ? Optional.ofNullable(rows.get(recipientId)) : Optional.empty();
        }

        @Override
        public boolean update(RecipientStoredRecord record, UUID actorId, Instant now, long expectedVersion) {
            RecipientStoredRecord current = rows.get(record.recipientId());
            if (current == null || current.versionNo() != expectedVersion) return false;
            rows.put(record.recipientId(), record);
            return true;
        }

        @Override
        public List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                RecipientStatus status) {
            if (!ACTOR_ID.equals(actorId)) return List.of();
            return rows.values().stream()
                    .filter(row -> emailLookupHmac == null || row.emailLookupHmac().equals(emailLookupHmac))
                    .filter(row -> status == null || row.status() == status).toList();
        }
    }
}
