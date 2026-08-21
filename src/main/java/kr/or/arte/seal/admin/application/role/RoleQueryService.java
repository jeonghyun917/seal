package kr.or.arte.seal.admin.application.role;

import kr.or.arte.seal.admin.persistence.reference.AdminReferenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleQueryService implements RoleQuery {
    private final AdminReferenceMapper mapper;

    public RoleQueryService(AdminReferenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummary> findRoles() {
        return mapper.findRoles();
    }
}
