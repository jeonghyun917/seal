package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.policy.persistence.PolicyQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class RetentionPolicyQueryService implements RetentionPolicyQuery {
    private final PolicyQueryMapper mapper;
    public RetentionPolicyQueryService(PolicyQueryMapper mapper) { this.mapper = mapper; }
    @Override @Transactional(readOnly = true)
    public List<RetentionPolicySummary> findVisiblePolicies(UUID adminUserId) {
        return mapper.findVisibleRetentionPolicies(adminUserId);
    }
}
