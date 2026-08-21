package kr.or.arte.seal.admin.application.organization;

import java.util.List;
import java.util.UUID;

public interface OrganizationManagement {

    List<OrganizationSummary> findVisibleOrganizations(UUID adminUserId);
}
