package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.crypto.EmailLookupHasher;
import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecipientQueryServiceTest {

    @Test
    void destroyedRecipientIsReturnedAsTombstoneWithoutDecryptingNullPersonalData() {
        UUID actorId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        RecipientStoredRecord destroyed = new RecipientStoredRecord(recipientId, organizationId,
                null, null, null, null, "동의", RecipientStatus.valueOf("DESTROYED"), 7);
        RecipientQueryService service = new RecipientQueryService(repositoryReturning(destroyed),
                cipherThatMustNotBeCalled(), normalizedEmail -> "unused");

        RecipientDetail detail = service.find(actorId, recipientId);

        assertThat(detail.maskedName()).isEqualTo("파기 완료");
        assertThat(detail.maskedEmail()).isEqualTo("파기 완료");
        assertThat(detail.status().name()).isEqualTo("DESTROYED");
    }

    private static RecipientRepository repositoryReturning(RecipientStoredRecord record) {
        return new RecipientRepository() {
            @Override public boolean canAccessActiveOrganization(UUID actorId, UUID organizationId) { return true; }
            @Override public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID organizationId,
                    String emailLookupHmac, UUID excludedRecipientId) { return Optional.empty(); }
            @Override public void insert(RecipientStoredRecord row, UUID actorId, Instant now) { }
            @Override public Optional<RecipientStoredRecord> findScoped(UUID actorId, UUID recipientId) {
                return Optional.of(record);
            }
            @Override public boolean update(RecipientStoredRecord row, UUID actorId, Instant now,
                    long expectedVersion) { return false; }
            @Override public List<RecipientStoredRecord> searchScoped(UUID actorId, String emailLookupHmac,
                    RecipientStatus status) { return List.of(record); }
        };
    }

    private static PersonalDataCipher cipherThatMustNotBeCalled() {
        return new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
                throw new AssertionError("파기된 개인정보를 암호화해서는 안 됩니다.");
            }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                throw new AssertionError("파기된 개인정보를 복호화해서는 안 됩니다.");
            }
        };
    }
}
