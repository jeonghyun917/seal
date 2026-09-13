package kr.or.arte.seal.delivery.application;

import kr.or.arte.seal.recipient.crypto.*;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailOutboxWorkerTest {
    @Test
    void temporaryFailureSchedulesFirstRetryAfterOneMinute() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);
        var worker = new MailOutboxWorker(repo,
                message -> { throw new MailTransportException(true, "SMTP_TEMP"); },
                fixture.cipher, true, Clock.fixed(repo.now, ZoneOffset.UTC));

        worker.processBatch(10);

        assertThat(repo.retryAt).isEqualTo(repo.now.plus(Duration.ofMinutes(1)));
        assertThat(repo.status).isEqualTo(MailOutboxStatus.RETRY_SCHEDULED);
    }

    @Test
    void disabledWorkerDoesNotClaimOrSend() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);
        var worker = new MailOutboxWorker(repo, message -> { throw new AssertionError("should not send"); },
                fixture.cipher, false, Clock.fixed(repo.now, ZoneOffset.UTC));

        assertThat(worker.processBatch(10)).isZero();
        assertThat(repo.claimed).isFalse();
    }

    @Test
    void permanentFailureDoesNotRetry() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);
        var worker = new MailOutboxWorker(repo,
                message -> { throw new MailTransportException(false, "SMTP_550"); },
                fixture.cipher, true, Clock.fixed(repo.now, ZoneOffset.UTC));

        worker.processBatch(10);

        assertThat(repo.status).isEqualTo(MailOutboxStatus.SEND_FAILED);
        assertThat(repo.retryAt).isNull();
    }

    @Test
    void secondAndThirdTemporaryFailuresUseFiveAndThirtyMinuteDelays() {
        var fixture = new Fixture();
        var second = new FakeRepo(fixture.cipher, 1);
        new MailOutboxWorker(second, message -> { throw new MailTransportException(true, "SMTP_451"); },
                fixture.cipher, true, Clock.fixed(second.now, ZoneOffset.UTC)).processBatch(10);
        assertThat(second.retryAt).isEqualTo(second.now.plus(Duration.ofMinutes(5)));

        var third = new FakeRepo(fixture.cipher, 2);
        new MailOutboxWorker(third, message -> { throw new MailTransportException(true, "SMTP_451"); },
                fixture.cipher, true, Clock.fixed(third.now, ZoneOffset.UTC)).processBatch(10);
        assertThat(third.retryAt).isEqualTo(third.now.plus(Duration.ofMinutes(30)));
    }

    @Test
    void claimsStaleProcessingWithTenMinuteLeaseAndRecordsSuccess() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);

        new MailOutboxWorker(repo, message -> {}, fixture.cipher, true,
                Clock.fixed(repo.now, ZoneOffset.UTC)).processBatch(10);

        assertThat(repo.leaseExpiredBefore).isEqualTo(repo.now.minus(Duration.ofMinutes(10)));
        assertThat(repo.status).isEqualTo(MailOutboxStatus.SMTP_ACCEPTED);
    }

    @Test
    void smtpSuccessFollowedByDatabaseFailureIsNotMisclassifiedAsContentFailure() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);
        repo.failAcceptedRecording = true;
        var worker = new MailOutboxWorker(repo, message -> {}, fixture.cipher, true,
                Clock.fixed(repo.now, ZoneOffset.UTC));

        assertThatThrownBy(() -> worker.processBatch(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB accepted failure");
        assertThat(repo.failedCalled).isFalse();
    }

    @Test
    void mailMessageSupportsAStableMessageId() {
        assertThat(Arrays.stream(MailMessage.class.getRecordComponents()).map(component -> component.getName()))
                .contains("messageId");
    }

    @Test
    void staleReissuedLinkIsCancelledBeforeSmtpTransport() {
        var fixture = new Fixture();
        var repo = new FakeRepo(fixture.cipher);
        repo.currentLink = false;
        var worker = new MailOutboxWorker(repo, message -> { throw new AssertionError("stale link sent"); },
                fixture.cipher, true, Clock.fixed(repo.now, ZoneOffset.UTC));
        worker.processBatch(10);

        assertThat(repo.status).isEqualTo(MailOutboxStatus.CANCELLED);
    }

    private static final class Fixture {
        final PersonalDataCipher cipher;

        Fixture() {
            byte[] key = new byte[32];
            Arrays.fill(key, (byte) 9);
            cipher = new AesGcmPersonalDataCipher(Map.of("v1", new SecretKeySpec(key, "AES")),
                    "v1", new SecureRandom());
        }
    }

    private static final class FakeRepo implements MailOutboxRepository {
        final Instant now = Instant.parse("2026-08-24T00:00:00Z");
        final UUID id = UUID.randomUUID(), org = UUID.randomUUID();
        final MailOutboxRecord row;
        Instant retryAt;
        MailOutboxStatus status;
        boolean claimed;
        boolean failedCalled;
        boolean failAcceptedRecording; boolean currentLink = true;
        Instant leaseExpiredBefore;

        FakeRepo(PersonalDataCipher cipher) { this(cipher, 0); }

        FakeRepo(PersonalDataCipher cipher, int attempt) {
            var email = cipher.encrypt(new PersonalDataContext("MAIL_OUTBOX", id, org, "recipientEmail"),
                    "a@example.com");
            var html = cipher.encrypt(new PersonalDataContext("MAIL_OUTBOX", id, org, "html"), "<p>hello</p>");
            row = new MailOutboxRecord(id, UUID.randomUUID(), UUID.randomUUID(), org,
                    email.bytes(), html.bytes(), email.keyId(), "subject", "a".repeat(64),
                    MailOutboxStatus.QUEUED, now, now, null, attempt, null, 0);
        }

        public List<MailOutboxRecord> claimDue(Instant ignored, int limit, Instant lease) {
            claimed = true;
            leaseExpiredBefore = lease;
            return List.of(row);
        }

        public void accepted(UUID id, int attempt, long versionNo, Instant now) {
            if (failAcceptedRecording) throw new IllegalStateException("DB accepted failure");
            status = MailOutboxStatus.SMTP_ACCEPTED;
        }

        public void retry(UUID id, int attempt, long versionNo, Instant next, String code, Instant now) {
            status = MailOutboxStatus.RETRY_SCHEDULED;
            retryAt = next;
        }

        public boolean isCurrentActiveLink(UUID outboxId, UUID linkId, Instant now) { return currentLink; }
        public void cancelClaim(UUID id, long versionNo, Instant now) { status = MailOutboxStatus.CANCELLED; }
        public void failed(UUID id, int attempt, long versionNo, String code, Instant now) {
            failedCalled = true;
            status = MailOutboxStatus.SEND_FAILED;
        }
    }
}
