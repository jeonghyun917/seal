package kr.or.arte.seal.policy.domain.activity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessingActivityVersionTest {
    private static final UUID ACTIVITY_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
    private static final UUID POLICY_VERSION_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID REVIEWER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void draftRequiresAtLeastOneGeneralDataItem() {
        assertThatThrownBy(() -> draft(LegalBasisType.CONSENT, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("개인정보 항목");
    }

    @Test
    void authorCannotApproveOwnRequestedVersion() {
        ProcessingActivityVersion requested = draft(LegalBasisType.CONSENT, items())
                .requestReview(AUTHOR_ID);

        assertThatThrownBy(() -> requested.approve(AUTHOR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("작성자와 승인자");
    }

    @Test
    void approvedVersionIsImmutable() {
        ProcessingActivityVersion approved = draft(LegalBasisType.CONSENT, items())
                .requestReview(AUTHOR_ID)
                .approve(REVIEWER_ID);

        assertThat(approved.workflowStatus()).isEqualTo(ProcessingWorkflowStatus.APPROVED);
        assertThatThrownBy(() -> approved.changeTerms(
                "교육 참여자를 관리하기 위한 변경 목적입니다.", LegalBasisType.CONSENT,
                "정보주체의 개인정보 수집·이용 동의", POLICY_VERSION_ID, items()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 버전");
    }

    @Test
    void onlyConsentBasisCanBeUsedForConsentRequest() {
        ProcessingActivityVersion consent = draft(LegalBasisType.CONSENT, items())
                .requestReview(AUTHOR_ID).approve(REVIEWER_ID);
        ProcessingActivityVersion publicTask = draft(LegalBasisType.PUBLIC_TASK, items())
                .requestReview(AUTHOR_ID).approve(REVIEWER_ID);

        assertThat(consent.canCreateConsentRequest()).isTrue();
        assertThat(publicTask.canCreateConsentRequest()).isFalse();
        assertThat(draft(LegalBasisType.CONSENT, items()).canCreateConsentRequest()).isFalse();
    }

    private static ProcessingActivityVersion draft(LegalBasisType basis, List<ProcessingDataItem> dataItems) {
        return ProcessingActivityVersion.draft(VERSION_ID, ACTIVITY_ID, 1,
                "교육 프로그램 참여자를 등록하고 운영하기 위한 목적입니다.", basis,
                "정보주체 동의 또는 관계 법령에 따른 처리 근거", POLICY_VERSION_ID,
                dataItems, AUTHOR_ID);
    }

    private static List<ProcessingDataItem> items() {
        return List.of(new ProcessingDataItem("성명", "교육 참여자 식별과 출석 확인", true));
    }
}
