package kr.or.arte.seal.admin.domain.organization;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class OrganizationPolicy {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,30}$");

    private OrganizationPolicy() {
    }

    public static void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("조직코드는 영문 대문자, 숫자, 하이픈 2~30자로 입력해야 합니다.");
        }
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("조직명은 1~100자로 입력해야 합니다.");
        }
    }

    public static void validateChange(UUID organizationId, boolean root, UUID parentId, boolean active) {
        Objects.requireNonNull(organizationId, "organizationId");
        if (organizationId.equals(parentId)) {
            throw new IllegalArgumentException("자기 자신을 상위 조직으로 지정할 수 없습니다.");
        }
        if (root && parentId != null) {
            throw new IllegalStateException("루트 조직에는 상위 조직을 지정할 수 없습니다.");
        }
        if (root && !active) {
            throw new IllegalStateException("루트 조직은 비활성화할 수 없습니다.");
        }
    }
}
