package kr.or.arte.seal.audit.application.query;

import kr.or.arte.seal.audit.persistence.query.AuditEventQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEventQueryService implements AuditEventQuery {
    private final AuditEventQueryMapper mapper;

    public AuditEventQueryService(AuditEventQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventSummary> findRecentVisibleEvents(UUID adminUserId) {
        return mapper.findRecentVisibleEvents(adminUserId);
    }
}
