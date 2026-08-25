package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.recipient.crypto.PersonalDataContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MailOutboxQueryServiceTest {

    @Test
    void destroyedMailIsReturnedWithoutDecryptingRemovedBody() {
        UUID actorId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        MailOutboxRecord destroyed = new MailOutboxRecord(outboxId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, null, null, "개인정보 동의", null,
                MailOutboxStatus.valueOf("DESTROYED"), Instant.parse("2026-08-25T00:00:00Z"), null, 1, null, 3);
        MailOutboxRepository repository = new MailOutboxRepository() {
            @Override public List<MailOutboxRecord> claimDue(Instant now, int limit, Instant leaseExpiredBefore) {
                return List.of();
            }
            @Override public void accepted(UUID id, int attempt, long versionNo, Instant now) { }
            @Override public void retry(UUID id, int attempt, long versionNo, Instant next, String code, Instant now) { }
            @Override public void failed(UUID id, int attempt, long versionNo, String code, Instant now) { }
            @Override public Optional<MailOutboxRecord> findScoped(UUID requester, UUID requestedId) {
                return Optional.of(destroyed);
            }
        };
        MailOutboxQueryService service = new MailOutboxQueryService(repository, cipherThatMustNotBeCalled(),
                Clock.fixed(Instant.parse("2026-08-25T01:00:00Z"), ZoneOffset.UTC), noAudit());

        MailOutboxDetail detail = service.require(actorId, outboxId);

        assertThat(detail.status().name()).isEqualTo("DESTROYED");
        assertThat(detail.html()).isNull();
    }

    private static PersonalDataCipher cipherThatMustNotBeCalled() {
        return new PersonalDataCipher() {
            @Override public EncryptedValue encrypt(PersonalDataContext context, String plaintext) {
                throw new AssertionError("파기된 메일을 암호화해서는 안 됩니다.");
            }
            @Override public String decrypt(PersonalDataContext context, EncryptedValue value) {
                throw new AssertionError("파기된 메일을 복호화해서는 안 됩니다.");
            }
        };
    }

    private static AuditEventWriter noAudit() {
        return command -> { };
    }
}
