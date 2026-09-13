package kr.or.arte.seal.template.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record ConsentItemDefinition(
        UUID itemId,
        int order,
        String code,
        ConsentItemType type,
        String title,
        boolean required,
        String purpose,
        String retentionText,
        String refusalNotice,
        String thirdPartyRecipient,
        List<UUID> processingDataItemIds
) {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,30}$");

    public ConsentItemDefinition {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(type, "type");
        if (order < 1) {
            throw new IllegalArgumentException("동의 항목 순서는 1 이상이어야 합니다.");
        }
        code = validateCode(code);
        title = validateText(title, 5, 200, "동의 항목 제목");
        purpose = validateText(purpose, 5, 1000, "개인정보 이용 목적");
        retentionText = validateText(retentionText, 5, 1000, "보유기간 안내");
        refusalNotice = validateText(refusalNotice, 10, 1000, "동의 거부 안내");
        if (type == ConsentItemType.OPTIONAL && required) {
            throw new IllegalArgumentException("선택 동의 항목은 필수로 설정할 수 없습니다.");
        }
        boolean hasRecipient = thirdPartyRecipient != null && !thirdPartyRecipient.isBlank();
        if ((type == ConsentItemType.THIRD_PARTY_PROVISION) != hasRecipient) {
            throw new IllegalArgumentException("제3자 제공 항목만 제공받는 자를 입력해야 합니다.");
        }
        thirdPartyRecipient = hasRecipient
                ? validateText(thirdPartyRecipient, 1, 500, "제공받는 자") : null;
        processingDataItemIds = List.copyOf(
                Objects.requireNonNull(processingDataItemIds, "processingDataItemIds"));
        if (processingDataItemIds.isEmpty()) {
            throw new IllegalArgumentException("개인정보 항목을 하나 이상 선택해야 합니다.");
        }
        if (processingDataItemIds.stream().anyMatch(Objects::isNull)
                || processingDataItemIds.stream().distinct().count() != processingDataItemIds.size()) {
            throw new IllegalArgumentException("개인정보 항목은 중복 없이 선택해야 합니다.");
        }
    }

    private static String validateCode(String value) {
        if (value == null || !CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("동의 항목 코드는 영문 대문자, 숫자, 하이픈 2~30자로 입력해야 합니다.");
        }
        return value;
    }

    private static String validateText(String value, int minimum, int maximum, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "은(는) " + minimum + "~" + maximum + "자로 입력해야 합니다.");
        }
        String stripped = value.strip();
        if (stripped.length() < minimum || stripped.length() > maximum) {
            throw new IllegalArgumentException(label + "은(는) " + minimum + "~" + maximum + "자로 입력해야 합니다.");
        }
        return stripped;
    }
}
