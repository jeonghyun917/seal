package kr.or.arte.seal.admin.domain;

import org.junit.jupiter.api.Test;

import static kr.or.arte.seal.admin.domain.PasswordViolation.SAME_AS_LOGIN_ID;
import static kr.or.arte.seal.admin.domain.PasswordViolation.TOO_LONG;
import static kr.or.arte.seal.admin.domain.PasswordViolation.TOO_SHORT;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(12, 128);

    @Test
    void elevenCharactersAreRejected() {
        PasswordValidationResult result = policy.validate("administrator", "safe-pass-1");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).containsExactly(TOO_SHORT);
    }

    @Test
    void twelveCharactersAreAccepted() {
        PasswordValidationResult result = policy.validate("administrator", "safe-pass-12");

        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void oneHundredTwentyNineCharactersAreRejected() {
        PasswordValidationResult result = policy.validate("administrator", "a".repeat(129));

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).containsExactly(TOO_LONG);
    }

    @Test
    void loginIdEqualityIsRejectedWithoutCaseDistinction() {
        PasswordValidationResult result = policy.validate("admin-account", "ADMIN-ACCOUNT");

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).containsExactly(SAME_AS_LOGIN_ID);
    }

    @Test
    void candidateWhitespaceIsNotSilentlyTrimmed() {
        PasswordValidationResult result = policy.validate("admin-account", " admin-account ");

        assertThat(result.valid()).isTrue();
    }
}
