package kr.or.arte.seal.admin.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record LoginLockPolicy(int maxFailures, Duration lockDuration) {

    public LoginLockPolicy {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        Objects.requireNonNull(lockDuration, "lockDuration");
        if (lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("lockDuration must be positive");
        }
    }

    public LoginFailureResult recordFailure(int currentFailures, Instant now) {
        if (currentFailures < 0) {
            throw new IllegalArgumentException("currentFailures must not be negative");
        }
        Objects.requireNonNull(now, "now");

        int nextFailures = Math.addExact(currentFailures, 1);
        Optional<Instant> lockedUntil = nextFailures >= maxFailures
                ? Optional.of(now.plus(lockDuration))
                : Optional.empty();
        return new LoginFailureResult(nextFailures, lockedUntil);
    }

    public boolean isTemporarilyLocked(Instant lockedUntil, Instant now) {
        Objects.requireNonNull(now, "now");
        return lockedUntil != null && now.isBefore(lockedUntil);
    }
}
