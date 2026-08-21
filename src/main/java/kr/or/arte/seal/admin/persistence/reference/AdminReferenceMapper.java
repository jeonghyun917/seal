package kr.or.arte.seal.admin.persistence.reference;

import kr.or.arte.seal.admin.application.role.RoleSummary;
import kr.or.arte.seal.admin.application.user.AdminUserSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AdminReferenceMapper {
    List<AdminUserSummary> findVisibleUsers(@Param("adminUserId") UUID adminUserId);
    List<RoleSummary> findRoles();
}
