package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.admin.domain.PasswordValidationResult;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminPasswordChangeService {

    private final AdminPasswordRepository repository;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventWriter auditEventWriter;
    private final Clock clock;

    public AdminPasswordChangeService(
            AdminPasswordRepository repository,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            AuditEventWriter auditEventWriter,
            Clock clock
    ) {
        this.repository = repository;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.auditEventWriter = auditEventWriter;
        this.clock = clock;
    }

    @Transactional
    public PasswordChangeResult change(
            UUID adminUserId,
            String currentPassword,
            String newPassword,
            String newPasswordConfirmation
    ) {
        Instant now = clock.instant();
        AdminAccount account = repository.findById(adminUserId).orElse(null);
        if (account == null) {
            return PasswordChangeResult.of(PasswordChangeStatus.ACCOUNT_NOT_FOUND);
        }

        if (!passwordEncoder.matches(valueOrEmpty(currentPassword), account.passwordHash())) {
            appendAudit(now, account, "FAILURE", "CURRENT_PASSWORD_INVALID");
            return PasswordChangeResult.of(PasswordChangeStatus.CURRENT_PASSWORD_INVALID);
        }

        if (!valueOrEmpty(newPassword).equals(valueOrEmpty(newPasswordConfirmation))) {
            appendAudit(now, account, "FAILURE", "CONFIRMATION_MISMATCH");
            return PasswordChangeResult.of(PasswordChangeStatus.CONFIRMATION_MISMATCH);
        }

        PasswordValidationResult validation = passwordPolicy.validate(
                account.loginId(),
                valueOrEmpty(newPassword)
        );
        if (!validation.valid()) {
            Set<String> violations = validation.violations().stream()
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());
            appendAudit(now, account, "FAILURE", "PASSWORD_POLICY_VIOLATION");
            return new PasswordChangeResult(PasswordChangeStatus.POLICY_VIOLATION, violations);
        }

        boolean updated = repository.updatePassword(
                account.adminUserId(),
                passwordEncoder.encode(newPassword),
                false,
                account.versionNo()
        );
        if (!updated) {
            appendAudit(now, account, "FAILURE", "CONCURRENT_UPDATE");
            return PasswordChangeResult.of(PasswordChangeStatus.CONCURRENT_UPDATE);
        }

        appendAudit(now, account, "SUCCESS", null);
        return PasswordChangeResult.of(PasswordChangeStatus.CHANGED);
    }

    private void appendAudit(Instant now, AdminAccount account, String result, String reasonCode) {
        auditEventWriter.append(new AuditEventCommand(
                now,
                "ADMIN",
                account.adminUserId(),
                "ADMIN_PASSWORD_CHANGE",
                "ADMIN_USER",
                account.adminUserId().toString(),
                result,
                reasonCode,
                UUID.randomUUID(),
                Map.of()
        ));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
