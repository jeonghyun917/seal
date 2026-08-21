package kr.or.arte.seal.admin.application.user;

import java.util.List;
import java.util.UUID;

public interface AdminUserQuery {
    List<AdminUserSummary> findVisibleUsers(UUID adminUserId);
}
