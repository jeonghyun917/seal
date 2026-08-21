package kr.or.arte.seal.admin.domain;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;

public record PasswordPolicy(int minimumLength, int maximumLength) {

    public PasswordPolicy {
        if (minimumLength < 1 || maximumLength < minimumLength) {
            throw new IllegalArgumentException("password length range is invalid");
        }
    }

    public PasswordValidationResult validate(String loginId, String candidate) {
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(candidate, "candidate");

        EnumSet<PasswordViolation> violations = EnumSet.noneOf(PasswordViolation.class);
        if (candidate.length() < minimumLength) {
            violations.add(PasswordViolation.TOO_SHORT);
        }
        if (candidate.length() > maximumLength) {
            violations.add(PasswordViolation.TOO_LONG);
        }
        if (candidate.toLowerCase(Locale.ROOT).equals(loginId.toLowerCase(Locale.ROOT))) {
            violations.add(PasswordViolation.SAME_AS_LOGIN_ID);
        }
        return new PasswordValidationResult(violations);
    }
}
