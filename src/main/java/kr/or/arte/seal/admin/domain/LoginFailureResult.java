package kr.or.arte.seal.admin.domain;

import java.time.Instant;
import java.util.Optional;

public record LoginFailureResult(int consecutiveFailures, Optional<Instant> lockedUntil) {

    public LoginFailureResult {
        if (consecutiveFailures < 1) {
            throw new IllegalArgumentException("consecutiveFailures must be positive");
        }
        lockedUntil = lockedUntil == null ? Optional.empty() : lockedUntil;
    }
}
