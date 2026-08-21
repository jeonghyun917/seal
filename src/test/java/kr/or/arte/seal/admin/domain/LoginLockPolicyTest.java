package kr.or.arte.seal.admin.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LoginLockPolicyTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private final LoginLockPolicy policy = new LoginLockPolicy(5, Duration.ofMinutes(15));

    @Test
    void fifthConsecutiveFailureCreatesFifteenMinuteLock() {
        LoginFailureResult result = policy.recordFailure(4, NOW);

        assertThat(result.consecutiveFailures()).isEqualTo(5);
        assertThat(result.lockedUntil()).contains(Instant.parse("2026-08-21T00:15:00Z"));
    }

    @Test
    void fourthConsecutiveFailureDoesNotLock() {
        LoginFailureResult result = policy.recordFailure(3, NOW);

        assertThat(result.consecutiveFailures()).isEqualTo(4);
        assertThat(result.lockedUntil()).isEmpty();
    }

    @Test
    void temporaryLockEndsAtConfiguredExpiryInstant() {
        Instant lockedUntil = Instant.parse("2026-08-21T00:15:00Z");

        assertThat(policy.isTemporarilyLocked(lockedUntil, lockedUntil.minusMillis(1))).isTrue();
        assertThat(policy.isTemporarilyLocked(lockedUntil, lockedUntil)).isFalse();
    }
}
