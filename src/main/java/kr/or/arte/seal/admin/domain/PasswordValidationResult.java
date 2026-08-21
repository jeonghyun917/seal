package kr.or.arte.seal.admin.domain;

import java.util.Set;

public record PasswordValidationResult(Set<PasswordViolation> violations) {

    public PasswordValidationResult {
        violations = Set.copyOf(violations);
    }

    public boolean valid() {
        return violations.isEmpty();
    }
}
