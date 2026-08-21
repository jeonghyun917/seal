package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPasswordChangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private PasswordEncoder encoder;
    private InMemoryPasswordRepository repository;
    private RecordingAuditWriter auditWriter;
    private AdminPasswordChangeService service;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder(4);
        repository = new InMemoryPasswordRepository(account(encoder.encode("current-password")));
        auditWriter = new RecordingAuditWriter();
        service = new AdminPasswordChangeService(
                repository,
                new PasswordPolicy(12, 128),
                encoder,
                auditWriter,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void correctCurrentPasswordAndValidConfirmationReplaceHashAndClearTemporaryFlag() {
        PasswordChangeResult result = service.change(
                USER_ID,
                "current-password",
                "new-password-12",
                "new-password-12"
        );

        assertThat(result.status()).isEqualTo(PasswordChangeStatus.CHANGED);
        assertThat(repository.updated).isTrue();
        assertThat(repository.mustChangePassword).isFalse();
        assertThat(encoder.matches("new-password-12", repository.newPasswordHash)).isTrue();
        assertThat(auditWriter.last().result()).isEqualTo("SUCCESS");
    }

    @Test
    void wrongCurrentPasswordDoesNotUpdateHash() {
        PasswordChangeResult result = service.change(
                USER_ID,
                "wrong-password",
                "new-password-12",
                "new-password-12"
        );

        assertThat(result.status()).isEqualTo(PasswordChangeStatus.CURRENT_PASSWORD_INVALID);
        assertThat(repository.updated).isFalse();
        assertThat(auditWriter.last().reasonCode()).isEqualTo("CURRENT_PASSWORD_INVALID");
    }

    @Test
    void mismatchedConfirmationDoesNotUpdateHash() {
        PasswordChangeResult result = service.change(
                USER_ID,
                "current-password",
                "new-password-12",
                "different-password"
        );

        assertThat(result.status()).isEqualTo(PasswordChangeStatus.CONFIRMATION_MISMATCH);
        assertThat(repository.updated).isFalse();
    }

    @Test
    void passwordPolicyViolationDoesNotUpdateHash() {
        PasswordChangeResult result = service.change(
                USER_ID,
                "current-password",
                "short",
                "short"
        );

        assertThat(result.status()).isEqualTo(PasswordChangeStatus.POLICY_VIOLATION);
        assertThat(result.passwordViolations()).containsExactly("TOO_SHORT");
        assertThat(repository.updated).isFalse();
    }

    private static AdminAccount account(String passwordHash) {
        return new AdminAccount(
                USER_ID,
                "seal-admin",
                "최초 관리자",
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                passwordHash,
                AdminAccountStatus.ACTIVE,
                0,
                null,
                true,
                null,
                Set.of("SYSTEM_ADMIN"),
                Set.of("USER_MANAGE"),
                4
        );
    }

    private static final class InMemoryPasswordRepository implements AdminPasswordRepository {
        private final AdminAccount account;
        private boolean updated;
        private String newPasswordHash;
        private boolean mustChangePassword = true;

        private InMemoryPasswordRepository(AdminAccount account) {
            this.account = account;
        }

        @Override
        public Optional<AdminAccount> findById(UUID adminUserId) {
            return account.adminUserId().equals(adminUserId) ? Optional.of(account) : Optional.empty();
        }

        @Override
        public boolean updatePassword(
                UUID adminUserId,
                String passwordHash,
                boolean mustChangePassword,
                long expectedVersion
        ) {
            if (!account.adminUserId().equals(adminUserId) || account.versionNo() != expectedVersion) {
                return false;
            }
            this.updated = true;
            this.newPasswordHash = passwordHash;
            this.mustChangePassword = mustChangePassword;
            return true;
        }
    }

    private static final class RecordingAuditWriter implements AuditEventWriter {
        private final List<AuditEventCommand> events = new ArrayList<>();

        @Override
        public void append(AuditEventCommand command) {
            events.add(command);
        }

        AuditEventCommand last() {
            return events.get(events.size() - 1);
        }
    }
}
