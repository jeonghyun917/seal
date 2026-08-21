package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import kr.or.arte.seal.admin.domain.LoginFailureResult;
import kr.or.arte.seal.admin.domain.LoginLockPolicy;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AdminAuthenticationService {

    private final AdminAccountRepository accountRepository;
    private final AuditEventWriter auditEventWriter;
    private final PasswordEncoder passwordEncoder;
    private final LoginLockPolicy lockPolicy;
    private final String dummyPasswordHash;

    public AdminAuthenticationService(
            AdminAccountRepository accountRepository,
            AuditEventWriter auditEventWriter,
            PasswordEncoder passwordEncoder,
            LoginLockPolicy lockPolicy,
            String dummyPasswordHash
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.auditEventWriter = Objects.requireNonNull(auditEventWriter, "auditEventWriter");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.lockPolicy = Objects.requireNonNull(lockPolicy, "lockPolicy");
        this.dummyPasswordHash = Objects.requireNonNull(dummyPasswordHash, "dummyPasswordHash");
    }

    @Transactional
    public AuthenticationResult authenticate(String loginId, String password, Instant now) {
        Objects.requireNonNull(now, "now");
        String normalizedLoginId = normalizeLoginId(loginId);
        CharSequence passwordCandidate = password == null ? "" : password;
        Optional<AdminAccount> found = accountRepository.findByLoginId(normalizedLoginId);

        if (found.isEmpty()) {
            passwordEncoder.matches(passwordCandidate, dummyPasswordHash);
            appendAudit(now, null, "ANONYMOUS", "FAILURE", "INVALID_CREDENTIALS");
            return AuthenticationResult.failed(AuthenticationStatus.INVALID_CREDENTIALS);
        }

        AdminAccount account = found.orElseThrow();
        if (account.status() != AdminAccountStatus.ACTIVE) {
            appendAudit(now, account, "ADMIN", "DENIED", "ACCOUNT_UNAVAILABLE");
            return AuthenticationResult.failed(AuthenticationStatus.ACCOUNT_UNAVAILABLE);
        }

        if (lockPolicy.isTemporarilyLocked(account.lockedUntil(), now)) {
            appendAudit(now, account, "ADMIN", "DENIED", "TEMPORARILY_LOCKED");
            return AuthenticationResult.failed(AuthenticationStatus.TEMPORARILY_LOCKED);
        }

        if (!passwordEncoder.matches(passwordCandidate, account.passwordHash())) {
            LoginFailureResult failure = lockPolicy.recordFailure(account.consecutiveFailures(), now);
            requireUpdated(accountRepository.recordAuthenticationFailure(
                    account.adminUserId(), failure, account.versionNo()
            ));
            String reason = failure.lockedUntil().isPresent()
                    ? "TEMPORARY_LOCK_CREATED"
                    : "INVALID_CREDENTIALS";
            appendAudit(now, account, "ADMIN", "FAILURE", reason);
            return AuthenticationResult.failed(AuthenticationStatus.INVALID_CREDENTIALS);
        }

        requireUpdated(accountRepository.recordAuthenticationSuccess(
                account.adminUserId(), now, account.versionNo()
        ));
        AdminAccount authenticated = account.withAuthenticationSuccess(now);
        appendAudit(now, authenticated, "ADMIN", "SUCCESS", null);
        return AuthenticationResult.authenticated(authenticated);
    }

    private static String normalizeLoginId(String loginId) {
        return loginId == null ? "" : loginId.strip().toLowerCase(Locale.ROOT);
    }

    private static void requireUpdated(boolean updated) {
        if (!updated) {
            throw new ConcurrentModificationException("administrator account changed concurrently");
        }
    }

    private void appendAudit(
            Instant occurredAt,
            AdminAccount account,
            String actorType,
            String result,
            String reasonCode
    ) {
        auditEventWriter.append(new AuditEventCommand(
                occurredAt,
                actorType,
                account == null ? null : account.adminUserId(),
                "ADMIN_LOGIN",
                "ADMIN_USER",
                account == null ? null : account.adminUserId().toString(),
                result,
                reasonCode,
                UUID.randomUUID(),
                Map.of()
        ));
    }
}
