package kr.or.arte.seal.policy.domain.activity;

public record ProcessingDataItem(String itemName, String necessityReason, boolean required) {
    public ProcessingDataItem {
        if (itemName == null || itemName.isBlank() || itemName.strip().length() > 100) {
            throw new IllegalArgumentException("개인정보 항목명은 1~100자로 입력해야 합니다.");
        }
        if (necessityReason == null || necessityReason.isBlank()
                || necessityReason.strip().length() > 500) {
            throw new IllegalArgumentException("개인정보 항목의 필요한 이유는 1~500자로 입력해야 합니다.");
        }
        itemName = itemName.strip();
        necessityReason = necessityReason.strip();
    }
}
