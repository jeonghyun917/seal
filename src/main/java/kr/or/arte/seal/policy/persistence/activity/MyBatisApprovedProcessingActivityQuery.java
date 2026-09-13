package kr.or.arte.seal.policy.persistence.activity;

import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class MyBatisApprovedProcessingActivityQuery implements ApprovedProcessingActivityQuery {
    private final ProcessingActivityMapper mapper;

    public MyBatisApprovedProcessingActivityQuery(ProcessingActivityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovedProcessingActivityReference findApprovedForTemplate(UUID actorId, UUID versionId) {
        List<ApprovedProcessingActivityRow> rows = mapper.findApprovedForTemplate(actorId, versionId);
        if (rows.size() != 1) {
            throw new IllegalArgumentException("동의서에 사용할 수 있는 승인된 개인정보 이용 업무 버전이 아닙니다.");
        }
        return toReference(rows.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedProcessingActivityReference> findAllApprovedForTemplate(UUID actorId) {
        return mapper.findAllApprovedForTemplate(actorId).stream().map(this::toReference).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedProcessingActivityReference> searchApprovedForTemplate(
            UUID actorId, String keyword, int limit, int offset) {
        String searchKeyword = keyword == null ? "" : keyword.strip();
        int safeLimit = Math.max(1, Math.min(101, limit));
        int safeOffset = Math.max(0, offset);
        return mapper.searchApprovedForTemplate(actorId, searchKeyword, safeLimit, safeOffset)
                .stream().map(this::toReference).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovedProcessingActivityReference findReferenceForExistingTemplate(
            UUID actorId, UUID versionId) {
        List<ApprovedProcessingActivityRow> rows = mapper.findReferenceForExistingTemplate(
                actorId, versionId);
        if (rows.size() != 1) {
            throw new IllegalArgumentException("동의서 양식의 개인정보 이용 업무 참조를 찾을 수 없습니다.");
        }
        return toReference(rows.get(0));
    }

    private ApprovedProcessingActivityReference toReference(ApprovedProcessingActivityRow row) {
        return new ApprovedProcessingActivityReference(row.activityId(), row.versionId(),
                row.ownerOrganizationId(), row.activityCode(), row.activityName(), row.processingPurpose(),
                row.legalBasisType(), row.retentionPolicyVersionId(), row.retentionDisplayText(),
                mapper.findApprovedDataItems(row.versionId()));
    }
}
