package kr.or.arte.seal.policy.application.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.UUID;

import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;
import org.junit.jupiter.api.Test;

class RetentionPolicyPresentationTest {
    @Test
    void exposesAutomaticallyRecordedRetentionStartEvents() {
        assertThat(RetentionStartEvent.values())
                .extracting(Enum::name, RetentionStartEvent::getLabel)
                .contains(
                        tuple("MAIL_FIRST_SENT_AT", "메일 최초 발송일"),
                        tuple("REQUEST_EXPIRES_AT", "동의 요청 만료일"),
                        tuple("CONSENT_SUBMITTED_AT", "수신자 응답 제출일"));
    }

    @Test
    void translatesStoredPolicyCodesForDetailDisplay() {
        RetentionPolicyVersionView version = new RetentionPolicyVersionView(
                UUID.randomUUID(), 1, "APPROVED", "CONSENT_SUBMITTED_AT",
                1, "YEAR", 0, "DELETE", "작성자", null, "승인자", null);

        assertThat(version.startEventLabel()).isEqualTo("수신자 응답 제출일");
        assertThat(version.periodUnitLabel()).isEqualTo("년");
        assertThat(version.destructionMethodLabel()).isEqualTo("개인정보 삭제");
    }

    @Test
    void translatesStoredPeriodUnitForListDisplay() {
        RetentionPolicySummary policy = new RetentionPolicySummary(
                UUID.randomUUID(), "RP-001", "보유정책", "조직",
                "ACTIVE", 1, "CONSENT_SUBMITTED_AT", 1, "YEAR");

        assertThat(policy.periodUnitLabel()).isEqualTo("년");
    }
}
