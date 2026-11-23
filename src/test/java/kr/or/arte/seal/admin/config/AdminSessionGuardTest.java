package kr.or.arte.seal.admin.config;

import kr.or.arte.seal.admin.application.*;
import kr.or.arte.seal.admin.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSessionGuardTest {
    private final UUID id = UUID.randomUUID(), org = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);

    @Test void rejectsDisabledAccountAndRevokedPermissionAndResetPassword() {
        var original = account(AdminAccountStatus.ACTIVE, "hash-before", false, Set.of("EVIDENCE_DOWNLOAD"), 1);
        var authentication = new DatabaseAuthenticationProvider((u, p, now) -> AuthenticationResult.authenticated(original), clock)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("admin", "unused"));
        var repository = mock(AdminPasswordRepository.class);
        var guard = new AdminSessionGuard(repository, clock);
        for (var changed : List.of(
                account(AdminAccountStatus.INACTIVE, "hash-before", false, Set.of("EVIDENCE_DOWNLOAD"), 2),
                account(AdminAccountStatus.ACTIVE, "hash-before", false, Set.of(), 2),
                account(AdminAccountStatus.ACTIVE, "hash-after", true, Set.of("EVIDENCE_DOWNLOAD"), 2))) {
            when(repository.findById(id)).thenReturn(Optional.of(changed));
            assertThat(guard.isInvalid(authentication)).isTrue();
        }
    }

    @Test void unrelatedLoginVersionChangeDoesNotInvalidateCurrentSession() {
        var account = account(AdminAccountStatus.ACTIVE, "hash", false, Set.of("EVIDENCE_VIEW"), 1);
        var authentication = new DatabaseAuthenticationProvider((u, p, now) -> AuthenticationResult.authenticated(account), clock)
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated("admin", "unused"));
        var repository = mock(AdminPasswordRepository.class);
        when(repository.findById(id)).thenReturn(Optional.of(account(AdminAccountStatus.ACTIVE, "hash", false, Set.of("EVIDENCE_VIEW"), 9)));
        assertThat(new AdminSessionGuard(repository, clock).isInvalid(authentication)).isFalse();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThat(new AdminSessionGuard(repository, clock).isInvalid(authentication)).isTrue();
        assertThat(authentication.getPrincipal().toString()).doesNotContain("passwordHash=");
    }

    private AdminAccount account(AdminAccountStatus status, String hash, boolean change, Set<String> permissions, long version) {
        return new AdminAccount(id, "admin", "관리자", org, hash, status, 0, null, change,
                clock.instant(), Set.of("ADMIN"), permissions, version);
    }
}
