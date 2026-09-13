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
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class MailOutboxQueryServiceTest {

    @Test
    void pagedSearchPassesTrimmedQueryToScopedRepository() {
        UUID actorId = UUID.randomUUID();
        MailOutboxRepository repository = mock(MailOutboxRepository.class);
        when(repository.search(actorId, MailOutboxStatus.QUEUED, "교육 동의", 21, 20))
                .thenReturn(List.of());
        MailOutboxQueryService service = new MailOutboxQueryService(repository, mock(PersonalDataCipher.class),
                Clock.systemUTC(), noAudit());

        service.searchPage(actorId, MailOutboxStatus.QUEUED, "  교육 동의  ", 2, 20);

        verify(repository).search(actorId, MailOutboxStatus.QUEUED, "교육 동의", 21, 20);
    }

    @Test
    void acceptedMailIncludesSmtpAcceptedTime() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Instant acceptedAt = Instant.parse("2026-08-28T05:48:00Z");
        String html = "<p>메일</p>";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(html.getBytes(StandardCharsets.UTF_8)));
        MailOutboxRecord accepted = new MailOutboxRecord(outboxId, UUID.randomUUID(), UUID.randomUUID(),
                organizationId, new byte[] {1}, new byte[] {2}, "key", "개인정보 동의", hash,
                MailOutboxStatus.SMTP_ACCEPTED, Instant.parse("2026-08-28T05:46:00Z"), null,
                acceptedAt, 1, null, 3);
        MailOutboxRepository repository = mock(MailOutboxRepository.class);
        when(repository.findScoped(actorId, outboxId)).thenReturn(Optional.of(accepted));
        PersonalDataCipher cipher = mock(PersonalDataCipher.class);
        when(cipher.decrypt(any(PersonalDataContext.class), any(EncryptedValue.class))).thenReturn(html);
        MailOutboxQueryService service = new MailOutboxQueryService(repository, cipher,
                Clock.fixed(acceptedAt, ZoneOffset.UTC), noAudit());

        MailOutboxDetail detail = service.require(actorId, outboxId);

        assertThat(detail.smtpAcceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void destroyedMailIsReturnedWithoutDecryptingRemovedBody() {
        UUID actorId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        MailOutboxRecord destroyed = new MailOutboxRecord(outboxId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, null, null, "개인정보 동의", null,
                MailOutboxStatus.valueOf("DESTROYED"), Instant.parse("2026-08-25T00:00:00Z"), null,
                null, 1, null, 3);
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
