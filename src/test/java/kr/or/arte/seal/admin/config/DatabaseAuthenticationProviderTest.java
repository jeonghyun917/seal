package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.AdminAuthenticator;
import kr.or.arte.seal.admin.application.AuthenticationResult;
import kr.or.arte.seal.admin.application.AuthenticationStatus;
import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAuthenticationProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void authenticatedRolesAndPermissionsBecomeSpringAuthorities() {
        AdminAuthenticator authenticator = (loginId, password, now) ->
                AuthenticationResult.authenticated(account());
        DatabaseAuthenticationProvider provider = new DatabaseAuthenticationProvider(authenticator, CLOCK);

        Authentication authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin", "correct-password")
        );

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(AdminPrincipal.class);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_SYSTEM_ADMIN", "ROLE_AUDITOR", "USER_MANAGE", "AUDIT_VIEW");
    }

    @Test
    void invalidCredentialsAndTemporaryLockExposeSameFailure() {
        BadCredentialsException invalid = failureFrom(AuthenticationStatus.INVALID_CREDENTIALS);
        BadCredentialsException locked = failureFrom(AuthenticationStatus.TEMPORARILY_LOCKED);

        assertThat(invalid.getMessage()).isEqualTo("아이디 또는 비밀번호를 확인해 주세요.");
        assertThat(locked.getMessage()).isEqualTo(invalid.getMessage());
    }

    @Test
    void providerSupportsOnlyUsernamePasswordTokens() {
        DatabaseAuthenticationProvider provider = new DatabaseAuthenticationProvider(
                (loginId, password, now) -> AuthenticationResult.failed(AuthenticationStatus.INVALID_CREDENTIALS),
                CLOCK
        );

        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }

    private static BadCredentialsException failureFrom(AuthenticationStatus status) {
        AdminAuthenticator authenticator = (loginId, password, now) -> AuthenticationResult.failed(status);
        DatabaseAuthenticationProvider provider = new DatabaseAuthenticationProvider(authenticator, CLOCK);

        return (BadCredentialsException) org.assertj.core.api.Assertions.catchThrowable(() ->
                provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("admin", "password"))
        );
    }

    private static AdminAccount account() {
        return new AdminAccount(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                "admin",
                "관리자",
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                "password-hash-not-exposed-to-principal",
                AdminAccountStatus.ACTIVE,
                0,
                null,
                true,
                NOW,
                Set.of("SYSTEM_ADMIN", "AUDITOR"),
                Set.of("USER_MANAGE", "AUDIT_VIEW"),
                1
        );
    }
}
