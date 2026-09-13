package kr.or.arte.seal.policy.domain.activity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessingActivityVersion(
        UUID processingActivityVersionId,
        UUID processingActivityId,
        int versionNumber,
        ProcessingWorkflowStatus workflowStatus,
        String processingPurpose,
        LegalBasisType legalBasisType,
        String legalBasisDetail,
        UUID retentionPolicyVersionId,
        List<ProcessingDataItem> dataItems,
        UUID createdBy,
        UUID reviewRequestedBy,
        UUID approvedBy,
        String rejectionComment,
        long versionNo
) {
    public ProcessingActivityVersion {
        Objects.requireNonNull(processingActivityVersionId, "processingActivityVersionId");
        Objects.requireNonNull(processingActivityId, "processingActivityId");
        Objects.requireNonNull(workflowStatus, "workflowStatus");
        Objects.requireNonNull(legalBasisType, "legalBasisType");
        Objects.requireNonNull(retentionPolicyVersionId, "retentionPolicyVersionId");
        Objects.requireNonNull(createdBy, "createdBy");
        if (versionNumber < 1) throw new IllegalArgumentException("버전 번호는 1 이상이어야 합니다.");
        if (versionNo < 0) throw new IllegalArgumentException("낙관적 잠금 버전은 음수일 수 없습니다.");
        processingPurpose = validateText(processingPurpose, 10, 1000, "개인정보 이용 목적");
        legalBasisDetail = validateText(legalBasisDetail, 5, 1000, "근거 상세 설명");
        dataItems = List.copyOf(Objects.requireNonNull(dataItems, "dataItems"));
        if (dataItems.isEmpty()) throw new IllegalArgumentException("수집·이용할 개인정보 항목을 하나 이상 입력해야 합니다.");
    }

    public static ProcessingActivityVersion draft(UUID versionId, UUID activityId,
            int versionNumber, String purpose, LegalBasisType basisType, String basisDetail,
            UUID retentionPolicyVersionId, List<ProcessingDataItem> dataItems, UUID createdBy) {
        return new ProcessingActivityVersion(versionId, activityId, versionNumber,
                ProcessingWorkflowStatus.DRAFT, purpose, basisType, basisDetail,
                retentionPolicyVersionId, dataItems, createdBy, null, null, null, 0);
    }

    public ProcessingActivityVersion requestReview(UUID requesterId) {
        requireStatus(ProcessingWorkflowStatus.DRAFT, "초안만 검토 요청할 수 있습니다.");
        return copy(ProcessingWorkflowStatus.REVIEW_REQUESTED, processingPurpose, legalBasisType,
                legalBasisDetail, retentionPolicyVersionId, dataItems,
                Objects.requireNonNull(requesterId, "requesterId"), null, null);
    }

    public ProcessingActivityVersion approve(UUID approverId) {
        requireStatus(ProcessingWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 승인할 수 있습니다.");
        Objects.requireNonNull(approverId, "approverId");
        if (createdBy.equals(approverId)) throw new IllegalStateException("작성자와 승인자는 달라야 합니다.");
        return copy(ProcessingWorkflowStatus.APPROVED, processingPurpose, legalBasisType,
                legalBasisDetail, retentionPolicyVersionId, dataItems,
                reviewRequestedBy, approverId, null);
    }

    public ProcessingActivityVersion reject(UUID reviewerId, String comment) {
        requireStatus(ProcessingWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 반려할 수 있습니다.");
        Objects.requireNonNull(reviewerId, "reviewerId");
        String rejection = validateText(comment, 1, 1000, "반려 의견");
        return copy(ProcessingWorkflowStatus.DRAFT, processingPurpose, legalBasisType,
                legalBasisDetail, retentionPolicyVersionId, dataItems, null, null, rejection);
    }

    public ProcessingActivityVersion changeTerms(String purpose, LegalBasisType basisType,
            String basisDetail, UUID changedRetentionPolicyVersionId,
            List<ProcessingDataItem> changedDataItems) {
        if (workflowStatus == ProcessingWorkflowStatus.APPROVED
                || workflowStatus == ProcessingWorkflowStatus.SUPERSEDED) {
            throw new IllegalStateException("승인된 버전은 수정할 수 없습니다.");
        }
        requireStatus(ProcessingWorkflowStatus.DRAFT, "초안 상태에서만 수정할 수 있습니다.");
        return copy(ProcessingWorkflowStatus.DRAFT, purpose,
                Objects.requireNonNull(basisType, "basisType"), basisDetail,
                Objects.requireNonNull(changedRetentionPolicyVersionId, "retentionPolicyVersionId"),
                changedDataItems, null, null, rejectionComment);
    }

    public boolean canCreateConsentRequest() {
        return workflowStatus == ProcessingWorkflowStatus.APPROVED
                && legalBasisType == LegalBasisType.CONSENT;
    }

    private ProcessingActivityVersion copy(ProcessingWorkflowStatus status, String purpose,
            LegalBasisType basisType, String basisDetail, UUID policyVersionId,
            List<ProcessingDataItem> items, UUID requester, UUID approver, String rejection) {
        return new ProcessingActivityVersion(processingActivityVersionId, processingActivityId,
                versionNumber, status, purpose, basisType, basisDetail, policyVersionId,
                items, createdBy, requester, approver, rejection, versionNo);
    }

    private void requireStatus(ProcessingWorkflowStatus expected, String message) {
        if (workflowStatus != expected) throw new IllegalStateException(message);
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
