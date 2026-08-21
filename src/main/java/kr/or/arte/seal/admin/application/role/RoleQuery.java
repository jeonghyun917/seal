package kr.or.arte.seal.admin.application.role;

import java.util.List;

public interface RoleQuery {
    List<RoleSummary> findRoles();
}
