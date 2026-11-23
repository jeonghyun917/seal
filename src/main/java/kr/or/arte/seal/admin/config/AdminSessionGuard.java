package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.AdminPasswordRepository;
import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.TreeSet;

@Service
public class AdminSessionGuard {
    private final AdminPasswordRepository accounts;
    private final Clock clock;

    public AdminSessionGuard(AdminPasswordRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public boolean isInvalid(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AdminPrincipal principal)) return false;
        if (principal.securityStamp() == null) return true;
        return accounts.findById(principal.adminUserId()).map(account ->
                account.status() != AdminAccountStatus.ACTIVE
                || (account.lockedUntil() != null && account.lockedUntil().isAfter(clock.instant()))
                || !MessageDigest.isEqual(principal.securityStamp().getBytes(StandardCharsets.US_ASCII),
                        stamp(account).getBytes(StandardCharsets.US_ASCII))).orElse(true);
    }

    static String stamp(AdminAccount account) {
        // Do not use versionNo: normal login/failure bookkeeping changes it too.
        // Only the digest, never the password hash, is retained in the session.
        String state = String.join("\n", account.adminUserId().toString(), account.loginId(),
                account.organizationUnitId().toString(), account.passwordHash(),
                Boolean.toString(account.mustChangePassword()), new TreeSet<>(account.roles()).toString(),
                new TreeSet<>(account.permissions()).toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(state.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
