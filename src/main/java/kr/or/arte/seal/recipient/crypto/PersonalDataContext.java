package kr.or.arte.seal.recipient.crypto;

import java.util.Objects;
import java.util.UUID;

public record PersonalDataContext(String aggregate, UUID objectId,
                                  UUID organizationId, String field) {
    public PersonalDataContext {
        aggregate = requireText(aggregate, "aggregate");
        objectId = Objects.requireNonNull(objectId, "objectId");
        organizationId = Objects.requireNonNull(organizationId, "organizationId");
        field = requireText(field, "field");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 값이 필요합니다.");
        }
        return value.trim();
    }
}
