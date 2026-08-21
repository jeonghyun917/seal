package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;

import java.util.Optional;

public record AuthenticationResult(AuthenticationStatus status, Optional<AdminAccount> account) {

    public AuthenticationResult {
        account = account == null ? Optional.empty() : account;
        if (status == AuthenticationStatus.AUTHENTICATED && account.isEmpty()) {
            throw new IllegalArgumentException("authenticated result requires an account");
        }
        if (status != AuthenticationStatus.AUTHENTICATED && account.isPresent()) {
            throw new IllegalArgumentException("failed result must not expose an account");
        }
    }

    public static AuthenticationResult authenticated(AdminAccount account) {
        return new AuthenticationResult(AuthenticationStatus.AUTHENTICATED, Optional.of(account));
    }

    public static AuthenticationResult failed(AuthenticationStatus status) {
        return new AuthenticationResult(status, Optional.empty());
    }
}
