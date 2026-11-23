package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.AdminAuthenticator;
import kr.or.arte.seal.admin.application.AuthenticationResult;
import kr.or.arte.seal.admin.application.AuthenticationStatus;
import kr.or.arte.seal.admin.domain.AdminAccount;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class DatabaseAuthenticationProvider implements AuthenticationProvider {

    private static final String GENERIC_FAILURE_MESSAGE = "아이디 또는 비밀번호를 확인해 주세요.";

    private final AdminAuthenticator authenticator;
    private final Clock clock;

    public DatabaseAuthenticationProvider(AdminAuthenticator authenticator, Clock clock) {
        this.authenticator = authenticator;
        this.clock = clock;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String loginId = authentication.getName();
        String password = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();
        Instant now = clock.instant();

        AuthenticationResult result = authenticator.authenticate(loginId, password, now);
        if (result.status() != AuthenticationStatus.AUTHENTICATED) {
            throw new BadCredentialsException(GENERIC_FAILURE_MESSAGE);
        }

        AdminAccount account = result.account().orElseThrow();
        AdminPrincipal principal = new AdminPrincipal(
                account.adminUserId(),
                account.loginId(),
                account.displayName(),
                account.organizationUnitId(),
                account.mustChangePassword(),
                AdminSessionGuard.stamp(account)
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                authorities(account)
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static Collection<? extends GrantedAuthority> authorities(AdminAccount account) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        account.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        account.permissions().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }
}
