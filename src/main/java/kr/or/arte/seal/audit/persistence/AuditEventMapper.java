package kr.or.arte.seal.audit.persistence;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditEventMapper {

    int insert(AuditEventRow row);
}
