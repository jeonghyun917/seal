package kr.or.arte.seal.policy.domain.activity;

public enum LegalBasisType {
    CONSENT("정보주체 동의"),
    LAW("법령상 의무"),
    PUBLIC_TASK("공공업무 수행"),
    CONTRACT("계약 체결·이행"),
    OTHER("기타");

    private final String displayName;

    LegalBasisType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
