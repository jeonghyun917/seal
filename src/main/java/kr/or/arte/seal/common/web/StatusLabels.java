package kr.or.arte.seal.common.web;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class StatusLabels {
    private static final Map<String, String> LABELS = createLabels();

    private StatusLabels() {
    }

    public static String label(String domain, String code) {
        if (code == null || code.isBlank()) {
            return "-";
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        String normalizedDomain = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
        return LABELS.getOrDefault(key(normalizedDomain, normalizedCode), code.trim());
    }

    private static Map<String, String> createLabels() {
        Map<String, String> labels = new HashMap<>();
        add(labels, "account", "ACTIVE", "활성", "LOCKED", "잠금", "INACTIVE", "비활성");
        add(labels, "active", "ACTIVE", "활성", "INACTIVE", "비활성");
        add(labels, "template", "ACTIVE", "사용", "INACTIVE", "중지");
        add(labels, "workflow", "DRAFT", "초안", "REVIEW_REQUESTED", "검토 요청", "APPROVED", "승인",
                "PUBLISHED", "발행", "RETIRED", "종료", "SUPERSEDED", "대체됨");
        add(labels, "request", "DRAFT", "초안", "READY", "발송 준비", "ACTIVE", "진행 중",
                "COMPLETED", "완료", "EXPIRED", "만료", "CANCELLED", "취소");
        add(labels, "recipient", "ACTIVE", "활성", "INACTIVE", "비활성", "DESTROYED", "파기 완료");
        add(labels, "mail", "QUEUED", "발송 대기", "PROCESSING", "발송 처리 중", "SMTP_ACCEPTED", "SMTP 접수",
                "RETRY_SCHEDULED", "재시도 예약", "SEND_FAILED", "발송 실패", "CANCELLED", "취소",
                "DESTROYED", "파기 완료");
        add(labels, "evidence", "QUEUED", "생성 대기", "PROCESSING", "생성 중", "FINALIZED", "생성 완료",
                "FAILED", "생성 실패", "DESTROYED", "파기 완료", "VERIFY_INVALID_HMAC", "HMAC 검증 실패",
                "VERIFY_INVALID_HASH", "해시 검증 실패", "VERIFY_MISSING_FILE", "파일 누락");
        add(labels, "rights", "RECEIVED", "접수", "IDENTITY_VERIFIED", "본인확인 완료", "IN_REVIEW", "검토 중",
                "APPROVED", "승인", "REJECTED", "반려", "COMPLETED", "처리 완료", "CANCELLED", "취소",
                "OVERDUE", "처리기한 초과");
        add(labels, "retention", "DUE", "파기 예정", "PENDING_APPROVAL", "승인 대기", "APPROVED", "승인 완료",
                "EXECUTING", "파기 실행 중", "EXECUTED", "파기 실행 완료", "CONFIRMED", "결과 확인 완료",
                "FAILED", "실패", "HELD", "보존 보류", "CANCELLED", "취소",
                "LEGAL_HOLD_ACTIVE", "활성 보존 보류");
        add(labels, "import", "VALIDATED", "검증 완료", "IMPORTED", "반입 완료", "EXPIRED", "만료");
        add(labels, "import-row", "VALID", "정상", "DUPLICATE", "중복", "ERROR", "오류",
                "IMPORTED", "반입 완료", "PURGED", "파기 완료");
        add(labels, "malware", "CLEAN", "이상 없음", "INFECTED", "악성코드 탐지", "DISABLED", "검사 비활성");
        add(labels, "response", "SUBMITTED", "제출 완료", "DECLINED_ALL", "전체 미동의", "WITHDRAWN", "철회");
        add(labels, "link", "ISSUED", "발급", "ACCESSED", "접속", "COMPLETED", "완료",
                "EXPIRED", "만료", "REVOKED", "폐기");
        add(labels, "destruction", "DELETE", "개인정보 삭제", "ANONYMIZE", "익명 처리", "FILE_SECURE_DELETE", "파일 완전 삭제");
        add(labels, "rights-type", "ACCESS", "열람", "CORRECTION", "정정", "ERASURE", "삭제",
                "RESTRICTION", "처리제한", "WITHDRAWAL", "동의철회");
        add(labels, "hold-reason", "AUDIT", "감사", "DISPUTE", "분쟁", "LITIGATION", "소송", "OTHER", "기타");
        add(labels, "import-error", "AFFILIATION_TOO_LONG", "소속기관은 200자 이내로 입력해 주세요.",
                "JOB_TITLE_TOO_LONG", "직책은 100자 이내로 입력해 주세요.",
                "NAME_REQUIRED", "이름을 입력해 주세요.", "NAME_TOO_LONG", "이름은 100자 이내로 입력해 주세요.",
                "EMAIL_INVALID", "이메일 주소 형식을 확인해 주세요.",
                "COLLECTION_BASIS_REQUIRED", "수신자 정보 등록 근거를 입력해 주세요.",
                "COLLECTION_BASIS_LENGTH_INVALID", "등록 근거는 5~500자로 입력해 주세요.",
                "DUPLICATE_EMAIL", "이미 등록되었거나 파일 안에서 중복된 이메일입니다.");
        return Map.copyOf(labels);
    }

    private static void add(Map<String, String> labels, String domain, String... codeAndLabel) {
        for (int i = 0; i < codeAndLabel.length; i += 2) {
            labels.put(key(domain, codeAndLabel[i]), codeAndLabel[i + 1]);
        }
    }

    private static String key(String domain, String code) {
        return domain + '\u0000' + code;
    }
}
