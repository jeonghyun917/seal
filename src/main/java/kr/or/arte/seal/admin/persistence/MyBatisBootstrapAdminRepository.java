package kr.or.arte.seal.admin.persistence;

import kr.or.arte.seal.admin.application.BootstrapAdminCommand;
import kr.or.arte.seal.admin.application.BootstrapAdminRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisBootstrapAdminRepository implements BootstrapAdminRepository {

    private final BootstrapAdminMapper mapper;

    public MyBatisBootstrapAdminRepository(BootstrapAdminMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long countAdministrators() {
        return mapper.countAdministrators();
    }

    @Override
    @Transactional
    public void create(BootstrapAdminCommand command) {
        requireOne(mapper.insertAdministrator(command), "administrator");
        requireOne(mapper.assignSystemAdminRole(command), "SYSTEM_ADMIN role");
        requireOne(mapper.assignRootOrganizationScope(command), "root organization scope");
    }

    private static void requireOne(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new IllegalStateException("bootstrap " + operation + " did not affect exactly one row");
        }
    }
}
