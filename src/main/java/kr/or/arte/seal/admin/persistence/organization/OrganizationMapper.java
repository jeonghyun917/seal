package kr.or.arte.seal.admin.persistence.organization;

import kr.or.arte.seal.admin.application.organization.OrganizationDetail;
import kr.or.arte.seal.admin.application.organization.OrganizationInsert;
import kr.or.arte.seal.admin.application.organization.OrganizationSummary;
import kr.or.arte.seal.admin.application.organization.OrganizationUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    List<OrganizationSummary> findVisibleOrganizations(@Param("adminUserId") UUID adminUserId);

    List<OrganizationDetail> findVisibleOrganization(@Param("adminUserId") UUID adminUserId,
            @Param("organizationUnitId") UUID organizationUnitId);

    boolean canAccessOrganization(@Param("adminUserId") UUID adminUserId,
            @Param("organizationUnitId") UUID organizationUnitId);

    int insert(OrganizationInsert organization);

    boolean wouldCreateCycle(@Param("organizationUnitId") UUID organizationUnitId,
            @Param("parentId") UUID parentId);

    int countActiveDependencies(@Param("organizationUnitId") UUID organizationUnitId);

    int update(OrganizationUpdate organization);
}
