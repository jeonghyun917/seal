package kr.or.arte.seal.audit.application.query;

import java.util.List;
import java.util.UUID;

public interface AuditEventQuery {
    List<AuditEventSummary> findRecentVisibleEvents(UUID adminUserId);
}
