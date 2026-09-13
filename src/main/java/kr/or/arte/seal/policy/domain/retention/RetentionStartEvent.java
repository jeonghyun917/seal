package kr.or.arte.seal.policy.domain.retention;

public enum RetentionStartEvent {
    MAIL_FIRST_SENT_AT("메일 최초 발송일"),
    REQUEST_EXPIRES_AT("동의 요청 만료일"),
    CONSENT_SUBMITTED_AT("수신자 응답 제출일"),
    REQUEST_COMPLETED_AT("동의 요청 처리 완료일"),
    PROGRAM_END_AT("프로그램 종료일 (날짜 별도 등록)"),
    PURPOSE_ACHIEVED_AT("개인정보 처리 목적 달성일 (날짜 별도 등록)");

    private final String label;

    RetentionStartEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
