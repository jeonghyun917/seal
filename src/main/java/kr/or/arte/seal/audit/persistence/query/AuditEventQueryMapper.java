package kr.or.arte.seal.audit.persistence.query;

import kr.or.arte.seal.audit.application.query.AuditEventSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AuditEventQueryMapper {
    List<AuditEventSummary> findRecentVisibleEvents(@Param("adminUserId") UUID adminUserId);
}
