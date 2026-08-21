package kr.or.arte.seal.admin.application.user;

import kr.or.arte.seal.admin.persistence.reference.AdminReferenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserQueryService implements AdminUserQuery {
    private final AdminReferenceMapper mapper;

    public AdminUserQueryService(AdminReferenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserSummary> findVisibleUsers(UUID adminUserId) {
        return mapper.findVisibleUsers(adminUserId);
    }
}
