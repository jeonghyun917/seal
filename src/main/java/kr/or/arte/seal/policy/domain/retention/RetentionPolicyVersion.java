package kr.or.arte.seal.policy.domain.retention;

import java.util.Objects;
import java.util.UUID;

public record RetentionPolicyVersion(
        UUID retentionPolicyVersionId,
        UUID retentionPolicyId,
        int versionNumber,
        RetentionWorkflowStatus workflowStatus,
        RetentionStartEvent startEvent,
        int periodValue,
        RetentionPeriodUnit periodUnit,
        int gracePeriodDays,
        DestructionMethod destructionMethod,
        UUID createdBy,
        UUID reviewRequestedBy,
        UUID approvedBy,
        String rejectionComment,
        long versionNo
) {
    public RetentionPolicyVersion {
        Objects.requireNonNull(retentionPolicyVersionId, "retentionPolicyVersionId");
        Objects.requireNonNull(retentionPolicyId, "retentionPolicyId");
        Objects.requireNonNull(workflowStatus, "workflowStatus");
        Objects.requireNonNull(startEvent, "startEvent");
        Objects.requireNonNull(periodUnit, "periodUnit");
        Objects.requireNonNull(destructionMethod, "destructionMethod");
        Objects.requireNonNull(createdBy, "createdBy");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("버전 번호는 1 이상이어야 합니다.");
        }
        validateTerms(periodValue, gracePeriodDays);
        if (versionNo < 0) {
            throw new IllegalArgumentException("낙관적 잠금 버전은 음수일 수 없습니다.");
        }
    }

    public static RetentionPolicyVersion draft(
            UUID versionId,
            UUID policyId,
            int versionNumber,
            RetentionStartEvent startEvent,
            int periodValue,
            RetentionPeriodUnit periodUnit,
            int gracePeriodDays,
            DestructionMethod destructionMethod,
            UUID createdBy
    ) {
        return new RetentionPolicyVersion(versionId, policyId, versionNumber,
                RetentionWorkflowStatus.DRAFT, startEvent, periodValue, periodUnit,
                gracePeriodDays, destructionMethod, createdBy, null, null, null, 0);
    }

    public RetentionPolicyVersion requestReview(UUID requesterId) {
        requireStatus(RetentionWorkflowStatus.DRAFT, "초안만 검토 요청할 수 있습니다.");
        return copy(RetentionWorkflowStatus.REVIEW_REQUESTED, startEvent, periodValue,
                periodUnit, gracePeriodDays, destructionMethod,
                Objects.requireNonNull(requesterId, "requesterId"), null, null);
    }

    public RetentionPolicyVersion approve(UUID approverId) {
        requireStatus(RetentionWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 승인할 수 있습니다.");
        Objects.requireNonNull(approverId, "approverId");
        if (createdBy.equals(approverId)) {
            throw new IllegalStateException("작성자와 승인자는 달라야 합니다.");
        }
        return copy(RetentionWorkflowStatus.APPROVED, startEvent, periodValue,
                periodUnit, gracePeriodDays, destructionMethod,
                reviewRequestedBy, approverId, null);
    }

    public RetentionPolicyVersion reject(UUID reviewerId, String comment) {
        requireStatus(RetentionWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 반려할 수 있습니다.");
        Objects.requireNonNull(reviewerId, "reviewerId");
        if (comment == null || comment.isBlank() || comment.strip().length() > 1000) {
            throw new IllegalArgumentException("반려 의견은 1~1000자로 입력해야 합니다.");
        }
        return copy(RetentionWorkflowStatus.DRAFT, startEvent, periodValue,
                periodUnit, gracePeriodDays, destructionMethod,
                null, null, comment.strip());
    }

    public RetentionPolicyVersion changeTerms(
            RetentionStartEvent changedStartEvent,
            int changedPeriodValue,
            RetentionPeriodUnit changedPeriodUnit,
            int changedGracePeriodDays,
            DestructionMethod changedDestructionMethod
    ) {
        if (workflowStatus == RetentionWorkflowStatus.APPROVED
                || workflowStatus == RetentionWorkflowStatus.SUPERSEDED) {
            throw new IllegalStateException("승인된 버전은 수정할 수 없습니다.");
        }
        requireStatus(RetentionWorkflowStatus.DRAFT, "초안 상태에서만 수정할 수 있습니다.");
        validateTerms(changedPeriodValue, changedGracePeriodDays);
        return copy(RetentionWorkflowStatus.DRAFT,
                Objects.requireNonNull(changedStartEvent, "changedStartEvent"),
                changedPeriodValue,
                Objects.requireNonNull(changedPeriodUnit, "changedPeriodUnit"),
                changedGracePeriodDays,
                Objects.requireNonNull(changedDestructionMethod, "changedDestructionMethod"),
                null, null, rejectionComment);
    }

    private RetentionPolicyVersion copy(
            RetentionWorkflowStatus status,
            RetentionStartEvent event,
            int period,
            RetentionPeriodUnit unit,
            int grace,
            DestructionMethod method,
            UUID requester,
            UUID approver,
            String rejection
    ) {
        return new RetentionPolicyVersion(retentionPolicyVersionId, retentionPolicyId,
                versionNumber, status, event, period, unit, grace, method, createdBy,
                requester, approver, rejection, versionNo);
    }

    private void requireStatus(RetentionWorkflowStatus expected, String message) {
        if (workflowStatus != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static void validateTerms(int periodValue, int gracePeriodDays) {
        if (periodValue < 1 || periodValue > 100) {
            throw new IllegalArgumentException("보유기간 값은 1~100이어야 합니다.");
        }
        if (gracePeriodDays < 0 || gracePeriodDays > 365) {
            throw new IllegalArgumentException("파기 유예기간은 0~365일이어야 합니다.");
        }
    }
}
