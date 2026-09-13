package kr.or.arte.seal.policy.domain.retention;

public enum RetentionPeriodUnit {
    DAY("일"),
    MONTH("개월"),
    YEAR("년");

    private final String label;

    RetentionPeriodUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
