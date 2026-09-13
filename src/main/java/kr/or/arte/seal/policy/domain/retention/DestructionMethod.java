package kr.or.arte.seal.policy.domain.retention;

public enum DestructionMethod {
    DELETE("개인정보 삭제"),
    ANONYMIZE("익명 처리"),
    FILE_SECURE_DELETE("파일 완전 삭제");

    private final String label;

    DestructionMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
