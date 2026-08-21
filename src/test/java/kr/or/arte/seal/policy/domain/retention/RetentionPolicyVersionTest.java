package kr.or.arte.seal.policy.domain.retention;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetentionPolicyVersionTest {

    private static final UUID POLICY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID REVIEWER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void validatesRetentionValuesWhenDraftIsCreated() {
        assertThatThrownBy(() -> draft(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("보유기간");

        assertThatThrownBy(() -> draft(3, 366))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유예기간");
    }

    @Test
    void draftCanBeRequestedForReview() {
        RetentionPolicyVersion requested = draft(3, 30).requestReview(AUTHOR_ID);

        assertThat(requested.workflowStatus()).isEqualTo(RetentionWorkflowStatus.REVIEW_REQUESTED);
        assertThat(requested.reviewRequestedBy()).isEqualTo(AUTHOR_ID);
    }

    @Test
    void authorCannotApproveOwnVersion() {
        RetentionPolicyVersion requested = draft(3, 30).requestReview(AUTHOR_ID);

        assertThatThrownBy(() -> requested.approve(AUTHOR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("작성자와 승인자");
    }

    @Test
    void reviewerCanApproveRequestedVersionAndApprovedVersionIsImmutable() {
        RetentionPolicyVersion approved = draft(3, 30)
                .requestReview(AUTHOR_ID)
                .approve(REVIEWER_ID);

        assertThat(approved.workflowStatus()).isEqualTo(RetentionWorkflowStatus.APPROVED);
        assertThat(approved.approvedBy()).isEqualTo(REVIEWER_ID);
        assertThatThrownBy(() -> approved.changeTerms(
                RetentionStartEvent.PURPOSE_ACHIEVED_AT,
                5,
                RetentionPeriodUnit.YEAR,
                0,
                DestructionMethod.DELETE
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 버전");
    }

    @Test
    void rejectionReturnsSameVersionToDraftWithReason() {
        RetentionPolicyVersion rejected = draft(3, 30)
                .requestReview(AUTHOR_ID)
                .reject(REVIEWER_ID, "보유기간 근거를 보완해 주세요.");

        assertThat(rejected.workflowStatus()).isEqualTo(RetentionWorkflowStatus.DRAFT);
        assertThat(rejected.rejectionComment()).contains("보유기간 근거");
        assertThat(rejected.approvedBy()).isNull();
    }

    private static RetentionPolicyVersion draft(int periodValue, int graceDays) {
        return RetentionPolicyVersion.draft(
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                POLICY_ID,
                1,
                RetentionStartEvent.CONSENT_SUBMITTED_AT,
                periodValue,
                RetentionPeriodUnit.YEAR,
                graceDays,
                DestructionMethod.DELETE,
                AUTHOR_ID
        );
    }
}
