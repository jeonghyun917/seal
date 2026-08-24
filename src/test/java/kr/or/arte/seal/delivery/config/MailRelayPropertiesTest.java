package kr.or.arte.seal.delivery.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailRelayPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void enabledRelayAcceptsArteSenderWithSslAndNoAuthentication() {
        MailRelayProperties properties = new MailRelayProperties(
                true,
                "relay.govkorea.com",
                465,
                true,
                false,
                "privacy@arte.or.kr",
                "개인정보 동의 안내");

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void enabledRelayAcceptsPort25WithoutSslOrAuthentication() {
        MailRelayProperties properties = new MailRelayProperties(
                true,
                "relay.govkorea.com",
                25,
                false,
                false,
                "seal@arte.or.kr",
                "개인정보 동의 안내");

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void enabledRelayRejectsSenderOutsideArteDomain() {
        MailRelayProperties properties = new MailRelayProperties(
                true,
                "relay.govkorea.com",
                465,
                true,
                false,
                "privacy@example.com",
                "개인정보 동의 안내");

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getMessage().contains("@arte.or.kr"));
    }

    @Test
    void enabledRelayRejectsAuthentication() {
        MailRelayProperties properties = new MailRelayProperties(
                true,
                "relay.govkorea.com",
                25,
                false,
                true,
                "privacy@arte.or.kr",
                "개인정보 동의 안내");

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getMessage().contains("무인증"));
    }

    @Test
    void enabledRelayRejectsPortAndSslModeMismatch() {
        MailRelayProperties properties = new MailRelayProperties(
                true,
                "relay.govkorea.com",
                25,
                true,
                false,
                "seal@arte.or.kr",
                "개인정보 동의 안내");

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getMessage().contains("25/평문"));
    }
}
