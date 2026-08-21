package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.policy.persistence.PolicyQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RetentionPolicyQueryService implements RetentionPolicyQuery {
    private final PolicyQueryMapper mapper;
    public RetentionPolicyQueryService(PolicyQueryMapper mapper) { this.mapper = mapper; }
    @Override @Transactional(readOnly = true)
    public List<RetentionPolicySummary> findVisiblePolicies(UUID adminUserId) {
        return mapper.findVisibleRetentionPolicies(adminUserId);
    }

    @Override @Transactional(readOnly = true)
    public RetentionPolicyDetail findVisiblePolicy(UUID adminUserId, UUID retentionPolicyId) {
        List<RetentionPolicyDetail> rows = mapper.findVisibleRetentionPolicy(adminUserId, retentionPolicyId);
        if (rows.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return rows.get(0);
    }

    @Override @Transactional(readOnly = true)
    public List<RetentionPolicyVersionView> findVisibleVersions(UUID adminUserId, UUID retentionPolicyId) {
        return mapper.findVisibleRetentionPolicyVersions(adminUserId, retentionPolicyId);
    }
}
