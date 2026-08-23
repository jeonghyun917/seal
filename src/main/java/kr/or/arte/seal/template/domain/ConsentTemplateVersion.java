package kr.or.arte.seal.template.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public record ConsentTemplateVersion(
        UUID consentTemplateVersionId,
        UUID consentTemplateId,
        UUID processingActivityId,
        UUID processingActivityVersionId,
        int versionNumber,
        TemplateWorkflowStatus workflowStatus,
        String title,
        String introductionHtml,
        List<ConsentItemDefinition> items,
        UUID createdBy,
        UUID reviewRequestedBy,
        UUID approvedBy,
        UUID publishedBy,
        String rejectionComment,
        String contentHash,
        long versionNo
) {
    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    public ConsentTemplateVersion {
        Objects.requireNonNull(consentTemplateVersionId, "consentTemplateVersionId");
        Objects.requireNonNull(consentTemplateId, "consentTemplateId");
        Objects.requireNonNull(processingActivityId, "processingActivityId");
        Objects.requireNonNull(processingActivityVersionId, "processingActivityVersionId");
        Objects.requireNonNull(workflowStatus, "workflowStatus");
        Objects.requireNonNull(createdBy, "createdBy");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("버전 번호는 1 이상이어야 합니다.");
        }
        if (versionNo < 0) {
            throw new IllegalArgumentException("낙관적 잠금 버전은 음수일 수 없습니다.");
        }
        title = validateText(title, 5, 200, "동의서 제목");
        introductionHtml = validateText(introductionHtml, 10, 8000, "동의서 안내문");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("동의 항목을 하나 이상 입력해야 합니다.");
        }
        validateUniqueItems(items);
        rejectionComment = normalizeOptional(rejectionComment, 1000, "반려 의견");
        validateState(workflowStatus, reviewRequestedBy, approvedBy, publishedBy, contentHash);
    }

    public static ConsentTemplateVersion draft(
            UUID versionId,
            UUID templateId,
            UUID processingActivityId,
            UUID processingActivityVersionId,
            int versionNumber,
            String title,
            String introductionHtml,
            List<ConsentItemDefinition> items,
            UUID createdBy
    ) {
        return new ConsentTemplateVersion(versionId, templateId, processingActivityId,
                processingActivityVersionId, versionNumber, TemplateWorkflowStatus.DRAFT,
                title, introductionHtml, items, createdBy, null, null, null, null, null, 0);
    }

    public ConsentTemplateVersion requestReview(UUID requesterId) {
        requireStatus(TemplateWorkflowStatus.DRAFT, "초안만 검토 요청할 수 있습니다.");
        return copy(TemplateWorkflowStatus.REVIEW_REQUESTED, title, introductionHtml, items,
                Objects.requireNonNull(requesterId, "requesterId"), null, null, null, null);
    }

    public ConsentTemplateVersion approve(UUID approverId) {
        requireStatus(TemplateWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 승인할 수 있습니다.");
        Objects.requireNonNull(approverId, "approverId");
        if (createdBy.equals(approverId)) {
            throw new IllegalStateException("작성자와 승인자는 달라야 합니다.");
        }
        return copy(TemplateWorkflowStatus.APPROVED, title, introductionHtml, items,
                reviewRequestedBy, approverId, null, null, null);
    }

    public ConsentTemplateVersion reject(UUID reviewerId, String comment) {
        requireStatus(TemplateWorkflowStatus.REVIEW_REQUESTED, "검토 요청된 버전만 반려할 수 있습니다.");
        Objects.requireNonNull(reviewerId, "reviewerId");
        String rejection = validateText(comment, 1, 1000, "반려 의견");
        return copy(TemplateWorkflowStatus.DRAFT, title, introductionHtml, items,
                null, null, null, rejection, null);
    }

    public ConsentTemplateVersion publish(UUID publisherId, String hash) {
        requireStatus(TemplateWorkflowStatus.APPROVED, "승인된 버전만 발행할 수 있습니다.");
        Objects.requireNonNull(publisherId, "publisherId");
        validateHash(hash);
        return copy(TemplateWorkflowStatus.PUBLISHED, title, introductionHtml, items,
                reviewRequestedBy, approvedBy, publisherId, null, hash);
    }

    public ConsentTemplateVersion retire(UUID actorId) {
        requireStatus(TemplateWorkflowStatus.PUBLISHED, "발행된 버전만 종료할 수 있습니다.");
        Objects.requireNonNull(actorId, "actorId");
        return copy(TemplateWorkflowStatus.RETIRED, title, introductionHtml, items,
                reviewRequestedBy, approvedBy, publishedBy, rejectionComment, contentHash);
    }

    public ConsentTemplateVersion changeContent(
            String changedTitle,
            String changedIntroductionHtml,
            List<ConsentItemDefinition> changedItems
    ) {
        requireStatus(TemplateWorkflowStatus.DRAFT, "초안 상태에서만 수정할 수 있습니다.");
        return copy(TemplateWorkflowStatus.DRAFT, changedTitle, changedIntroductionHtml,
                changedItems, null, null, null, rejectionComment, null);
    }

    private ConsentTemplateVersion copy(
            TemplateWorkflowStatus status,
            String changedTitle,
            String changedIntroductionHtml,
            List<ConsentItemDefinition> changedItems,
            UUID requester,
            UUID approver,
            UUID publisher,
            String rejection,
            String hash
    ) {
        return new ConsentTemplateVersion(consentTemplateVersionId, consentTemplateId,
                processingActivityId, processingActivityVersionId, versionNumber, status,
                changedTitle, changedIntroductionHtml, changedItems, createdBy,
                requester, approver, publisher, rejection, hash, versionNo);
    }

    private void requireStatus(TemplateWorkflowStatus expected, String message) {
        if (workflowStatus != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static void validateUniqueItems(List<ConsentItemDefinition> items) {
        Set<Integer> orders = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (ConsentItemDefinition item : items) {
            if (item == null || !orders.add(item.order()) || !codes.add(item.code())) {
                throw new IllegalArgumentException("동의 항목 순서와 코드는 중복될 수 없습니다.");
            }
        }
    }

    private static void validateState(TemplateWorkflowStatus status, UUID requester,
                                      UUID approver, UUID publisher, String hash) {
        switch (status) {
            case DRAFT -> requireAllNull(requester, approver, publisher, hash);
            case REVIEW_REQUESTED -> {
                Objects.requireNonNull(requester, "reviewRequestedBy");
                requireAllNull(approver, publisher, hash);
            }
            case APPROVED -> {
                Objects.requireNonNull(requester, "reviewRequestedBy");
                Objects.requireNonNull(approver, "approvedBy");
                requireAllNull(publisher, hash);
            }
            case PUBLISHED, RETIRED -> {
                Objects.requireNonNull(requester, "reviewRequestedBy");
                Objects.requireNonNull(approver, "approvedBy");
                Objects.requireNonNull(publisher, "publishedBy");
                validateHash(hash);
            }
        }
    }

    private static void requireAllNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                throw new IllegalArgumentException("워크플로 상태와 승인 정보가 일치하지 않습니다.");
            }
        }
    }

    private static void validateHash(String hash) {
        if (hash == null || !SHA_256.matcher(hash).matches()) {
            throw new IllegalArgumentException("콘텐츠 해시는 소문자 SHA-256 형식이어야 합니다.");
        }
    }

    private static String normalizeOptional(String value, int maximum, String label) {
        if (value == null) {
            return null;
        }
        return validateText(value, 1, maximum, label);
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
