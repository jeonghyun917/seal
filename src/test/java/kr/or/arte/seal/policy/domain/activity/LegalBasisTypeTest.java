package kr.or.arte.seal.policy.domain.activity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalBasisTypeTest {
    @Test
    void exposesKoreanDisplayNamesWithoutChangingStoredCodes() {
        assertThat(LegalBasisType.CONSENT.name()).isEqualTo("CONSENT");
        assertThat(LegalBasisType.CONSENT.getDisplayName()).isEqualTo("정보주체 동의");
        assertThat(LegalBasisType.LAW.getDisplayName()).isEqualTo("법령상 의무");
        assertThat(LegalBasisType.PUBLIC_TASK.getDisplayName()).isEqualTo("공공업무 수행");
        assertThat(LegalBasisType.CONTRACT.getDisplayName()).isEqualTo("계약 체결·이행");
        assertThat(LegalBasisType.OTHER.getDisplayName()).isEqualTo("기타");
    }
}
