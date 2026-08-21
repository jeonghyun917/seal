package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import kr.or.arte.seal.admin.domain.LoginFailureResult;
import kr.or.arte.seal.admin.domain.LoginLockPolicy;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static kr.or.arte.seal.admin.application.AuthenticationStatus.AUTHENTICATED;
import static kr.or.arte.seal.admin.application.AuthenticationStatus.INVALID_CREDENTIALS;
import static kr.or.arte.seal.admin.application.AuthenticationStatus.TEMPORARILY_LOCKED;
import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private PasswordEncoder passwordEncoder;
    private InMemoryAdminAccountRepository repository;
    private RecordingAuditEventWriter auditWriter;
    private AdminAuthenticationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        repository = new InMemoryAdminAccountRepository();
        auditWriter = new RecordingAuditEventWriter();
        repository.add(account(passwordEncoder.encode("correct-password"), 2, null));
        service = serviceUsing(passwordEncoder);
    }

    @Test
    void validPasswordReturnsAuthenticatedAndClearsFailures() {
        AuthenticationResult result = service.authenticate("ADMIN", "correct-password", NOW);

        assertThat(result.status()).isEqualTo(AUTHENTICATED);
        assertThat(result.account()).get().extracting(AdminAccount::loginId).isEqualTo("admin");
        assertThat(repository.required("admin").consecutiveFailures()).isZero();
        assertThat(repository.required("admin").lastLoginAt()).isEqualTo(NOW);
        assertThat(auditWriter.last().actionCode()).isEqualTo("ADMIN_LOGIN");
        assertThat(auditWriter.last().result()).isEqualTo("SUCCESS");
    }

    @Test
    void wrongPasswordRecordsFailureAndReturnsGenericResult() {
        AuthenticationResult result = service.authenticate("admin", "wrong-password", NOW);

        assertThat(result.status()).isEqualTo(INVALID_CREDENTIALS);
        assertThat(result.account()).isEmpty();
        assertThat(repository.required("admin").consecutiveFailures()).isEqualTo(3);
        assertThat(auditWriter.last().reasonCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void fifthWrongPasswordPersistsTemporaryLock() {
        repository.replace(account(passwordEncoder.encode("correct-password"), 4, null));

        AuthenticationResult result = service.authenticate("admin", "wrong-password", NOW);

        assertThat(result.status()).isEqualTo(INVALID_CREDENTIALS);
        assertThat(repository.required("admin").consecutiveFailures()).isEqualTo(5);
        assertThat(repository.required("admin").lockedUntil())
                .isEqualTo(Instant.parse("2026-08-21T00:15:00Z"));
        assertThat(auditWriter.last().reasonCode()).isEqualTo("TEMPORARY_LOCK_CREATED");
    }

    @Test
    void activeTemporaryLockRejectsBeforePasswordComparison() {
        repository.replace(account("comparison-must-not-run", 5, NOW.plusSeconds(60)));
        PasswordEncoder mustNotRun = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "unused";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                throw new AssertionError("password comparison must not run during a temporary lock");
            }
        };

        AuthenticationResult result = serviceUsing(mustNotRun)
                .authenticate("admin", "correct-password", NOW);

        assertThat(result.status()).isEqualTo(TEMPORARILY_LOCKED);
        assertThat(repository.required("admin").consecutiveFailures()).isEqualTo(5);
        assertThat(auditWriter.last().result()).isEqualTo("DENIED");
    }

    @Test
    void unknownLoginUsesSamePublicFailureResultAsWrongPassword() {
        AuthenticationResult wrongPassword = service.authenticate("admin", "wrong-password", NOW);
        AuthenticationResult unknownAccount = service.authenticate("unknown", "wrong-password", NOW);

        assertThat(unknownAccount.status()).isEqualTo(wrongPassword.status()).isEqualTo(INVALID_CREDENTIALS);
        assertThat(unknownAccount.account()).isEmpty();
    }

    private AdminAuthenticationService serviceUsing(PasswordEncoder encoder) {
        return new AdminAuthenticationService(
                repository,
                auditWriter,
                encoder,
                new LoginLockPolicy(5, Duration.ofMinutes(15)),
                passwordEncoder == null ? "unused" : passwordEncoder.encode("timing-only-password")
        );
    }

    private static AdminAccount account(String passwordHash, int failures, Instant lockedUntil) {
        return new AdminAccount(
                USER_ID,
                "admin",
                "관리자",
                ORGANIZATION_ID,
                passwordHash,
                AdminAccountStatus.ACTIVE,
                failures,
                lockedUntil,
                true,
                null,
                Set.of("SYSTEM_ADMIN"),
                Set.of("USER_MANAGE", "AUDIT_VIEW"),
                3
        );
    }

    private static final class InMemoryAdminAccountRepository implements AdminAccountRepository {
        private final Map<String, AdminAccount> accounts = new LinkedHashMap<>();

        void add(AdminAccount account) {
            accounts.put(account.loginId(), account);
        }

        void replace(AdminAccount account) {
            accounts.put(account.loginId(), account);
        }

        AdminAccount required(String loginId) {
            return accounts.get(loginId);
        }

        @Override
        public Optional<AdminAccount> findByLoginId(String normalizedLoginId) {
            return Optional.ofNullable(accounts.get(normalizedLoginId));
        }

        @Override
        public boolean recordAuthenticationFailure(
                UUID adminUserId,
                LoginFailureResult failure,
                long expectedVersion
        ) {
            AdminAccount current = findById(adminUserId);
            if (current.versionNo() != expectedVersion) {
                return false;
            }
            replace(current.withAuthenticationFailure(failure));
            return true;
        }

        @Override
        public boolean recordAuthenticationSuccess(UUID adminUserId, Instant loggedInAt, long expectedVersion) {
            AdminAccount current = findById(adminUserId);
            if (current.versionNo() != expectedVersion) {
                return false;
            }
            replace(current.withAuthenticationSuccess(loggedInAt));
            return true;
        }

        private AdminAccount findById(UUID adminUserId) {
            return accounts.values().stream()
                    .filter(account -> account.adminUserId().equals(adminUserId))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static final class RecordingAuditEventWriter implements AuditEventWriter {
        private final List<AuditEventCommand> commands = new ArrayList<>();

        @Override
        public void append(AuditEventCommand command) {
            commands.add(command);
        }

        AuditEventCommand last() {
            return commands.get(commands.size() - 1);
        }
    }
}
