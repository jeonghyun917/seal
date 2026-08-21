package kr.or.arte.seal.admin.domain.organization;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationPolicyTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void rejectsLowercaseOrganizationCode() {
        assertThatThrownBy(() -> OrganizationPolicy.validateCode("team-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조직코드");
    }

    @Test
    void rootOrganizationCannotBeDeactivated() {
        assertThatThrownBy(() -> OrganizationPolicy.validateChange(
                ORGANIZATION_ID,
                true,
                null,
                false
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("루트 조직");
    }

    @Test
    void organizationCannotUseItselfAsParent() {
        assertThatThrownBy(() -> OrganizationPolicy.validateChange(
                ORGANIZATION_ID,
                false,
                ORGANIZATION_ID,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상위 조직");
    }
}
