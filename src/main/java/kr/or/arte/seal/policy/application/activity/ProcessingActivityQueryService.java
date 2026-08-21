package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.policy.persistence.PolicyQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ProcessingActivityQueryService implements ProcessingActivityQuery {
    private final PolicyQueryMapper mapper;
    public ProcessingActivityQueryService(PolicyQueryMapper mapper) { this.mapper = mapper; }
    @Override @Transactional(readOnly = true)
    public List<ProcessingActivitySummary> findVisibleActivities(UUID adminUserId) {
        return mapper.findVisibleProcessingActivities(adminUserId);
    }
}
