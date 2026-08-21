package kr.or.arte.seal.admin.persistence.organization;

import kr.or.arte.seal.admin.application.organization.OrganizationSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    List<OrganizationSummary> findVisibleOrganizations(@Param("adminUserId") UUID adminUserId);
}
