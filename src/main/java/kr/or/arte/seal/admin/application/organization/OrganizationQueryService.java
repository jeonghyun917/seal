package kr.or.arte.seal.admin.application.organization;

import kr.or.arte.seal.admin.persistence.organization.OrganizationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationQueryService implements OrganizationManagement {

    private final OrganizationMapper organizationMapper;

    public OrganizationQueryService(OrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSummary> findVisibleOrganizations(UUID adminUserId) {
        return organizationMapper.findVisibleOrganizations(adminUserId);
    }
}
