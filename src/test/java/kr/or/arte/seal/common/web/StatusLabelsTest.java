package kr.or.arte.seal.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatusLabelsTest {
    @ParameterizedTest
    @MethodSource("labels")
    void translatesStatusCodesByBusinessDomain(String domain, String code, String expected) {
        assertThat(StatusLabels.label(domain, code)).isEqualTo(expected);
    }

    @Test
    void preservesUnknownStatusForOperationalDiagnostics() {
        assertThat(StatusLabels.label("mail", "NEW_PROVIDER_STATE")).isEqualTo("NEW_PROVIDER_STATE");
    }

    @Test
    void rendersMissingStatusAsDash() {
        assertThat(StatusLabels.label("mail", null)).isEqualTo("-");
        assertThat(StatusLabels.label("mail", " ")).isEqualTo("-");
    }

    static Stream<Arguments> labels() {
        return Stream.of(
                args("account", "ACTIVE", "활성"), args("account", "LOCKED", "잠금"), args("account", "INACTIVE", "비활성"),
                args("active", "ACTIVE", "활성"), args("active", "INACTIVE", "비활성"),
                args("template", "ACTIVE", "사용"), args("template", "INACTIVE", "중지"),
                args("workflow", "DRAFT", "초안"), args("workflow", "REVIEW_REQUESTED", "검토 요청"),
                args("workflow", "APPROVED", "승인"), args("workflow", "PUBLISHED", "발행"),
                args("workflow", "RETIRED", "종료"), args("workflow", "SUPERSEDED", "대체됨"),
                args("request", "DRAFT", "초안"), args("request", "READY", "발송 준비"),
                args("request", "ACTIVE", "진행 중"), args("request", "COMPLETED", "완료"),
                args("request", "EXPIRED", "만료"), args("request", "CANCELLED", "취소"),
                args("recipient", "ACTIVE", "활성"), args("recipient", "INACTIVE", "비활성"), args("recipient", "DESTROYED", "파기 완료"),
                args("mail", "QUEUED", "발송 대기"), args("mail", "PROCESSING", "발송 처리 중"),
                args("mail", "SMTP_ACCEPTED", "SMTP 접수"), args("mail", "RETRY_SCHEDULED", "재시도 예약"),
                args("mail", "SEND_FAILED", "발송 실패"), args("mail", "CANCELLED", "취소"), args("mail", "DESTROYED", "파기 완료"),
                args("evidence", "QUEUED", "생성 대기"), args("evidence", "PROCESSING", "생성 중"),
                args("evidence", "FINALIZED", "생성 완료"), args("evidence", "FAILED", "생성 실패"), args("evidence", "DESTROYED", "파기 완료"),
                args("rights", "RECEIVED", "접수"), args("rights", "IDENTITY_VERIFIED", "본인확인 완료"),
                args("rights", "IN_REVIEW", "검토 중"), args("rights", "APPROVED", "승인"),
                args("rights", "REJECTED", "반려"), args("rights", "COMPLETED", "처리 완료"), args("rights", "CANCELLED", "취소"),
                args("retention", "DUE", "파기 예정"), args("retention", "PENDING_APPROVAL", "승인 대기"),
                args("retention", "APPROVED", "승인 완료"), args("retention", "EXECUTING", "파기 실행 중"),
                args("retention", "EXECUTED", "파기 실행 완료"), args("retention", "CONFIRMED", "결과 확인 완료"),
                args("retention", "FAILED", "실패"), args("retention", "HELD", "보존 보류"), args("retention", "CANCELLED", "취소"),
                args("import", "VALIDATED", "검증 완료"), args("import", "IMPORTED", "반입 완료"), args("import", "EXPIRED", "만료"),
                args("import-row", "VALID", "정상"), args("import-row", "DUPLICATE", "중복"), args("import-row", "ERROR", "오류"),
                args("import-row", "IMPORTED", "반입 완료"), args("import-row", "PURGED", "파기 완료"),
                args("malware", "CLEAN", "이상 없음"), args("malware", "INFECTED", "악성코드 탐지"), args("malware", "DISABLED", "검사 비활성"),
                args("response", "SUBMITTED", "제출 완료"), args("response", "DECLINED_ALL", "전체 미동의"),
                args("link", "ISSUED", "발급"), args("link", "ACCESSED", "접속"), args("link", "COMPLETED", "완료"),
                args("link", "EXPIRED", "만료"), args("link", "REVOKED", "폐기"));
    }

    private static Arguments args(String domain, String code, String expected) {
        return Arguments.of(domain, code, expected);
    }
}
