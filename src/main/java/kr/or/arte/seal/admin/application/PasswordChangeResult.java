package kr.or.arte.seal.admin.application;

import java.util.Set;

public record PasswordChangeResult(PasswordChangeStatus status, Set<String> passwordViolations) {

    public PasswordChangeResult {
        passwordViolations = Set.copyOf(passwordViolations);
    }

    public static PasswordChangeResult of(PasswordChangeStatus status) {
        return new PasswordChangeResult(status, Set.of());
    }
}
