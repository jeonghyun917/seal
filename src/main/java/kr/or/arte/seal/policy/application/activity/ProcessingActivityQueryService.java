package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.policy.persistence.PolicyQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProcessingActivityQueryService implements ProcessingActivityQuery {
    private final PolicyQueryMapper mapper;
    public ProcessingActivityQueryService(PolicyQueryMapper mapper) { this.mapper = mapper; }
    @Override @Transactional(readOnly = true)
    public List<ProcessingActivitySummary> findVisibleActivities(UUID adminUserId) {
        return mapper.findVisibleProcessingActivities(adminUserId);
    }
    @Override @Transactional(readOnly = true)
    public ProcessingActivityDetail findVisibleActivity(UUID adminUserId, UUID activityId) {
        var rows = mapper.findVisibleProcessingActivity(adminUserId, activityId);
        if (rows.size() != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return rows.get(0);
    }
    @Override @Transactional(readOnly = true)
    public List<ProcessingActivityVersionView> findVisibleVersions(UUID adminUserId, UUID activityId) {
        findVisibleActivity(adminUserId, activityId);
        return mapper.findVisibleProcessingActivityVersions(adminUserId, activityId);
    }
    @Override @Transactional(readOnly = true)
    public List<ProcessingDataItemView> findVisibleDataItems(UUID adminUserId, UUID activityId) {
        findVisibleActivity(adminUserId, activityId);
        return mapper.findVisibleProcessingDataItems(adminUserId, activityId);
    }
    @Override @Transactional(readOnly = true)
    public List<ApprovedRetentionPolicyOption> findApprovedRetentionPolicies(UUID adminUserId) {
        return mapper.findApprovedRetentionPolicies(adminUserId);
    }
}
