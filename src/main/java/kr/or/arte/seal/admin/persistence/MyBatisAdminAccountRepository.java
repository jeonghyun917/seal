package kr.or.arte.seal.admin.persistence;

import kr.or.arte.seal.admin.application.AdminAccountRepository;
import kr.or.arte.seal.admin.application.AdminPasswordRepository;
import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.AdminAccountStatus;
import kr.or.arte.seal.admin.domain.LoginFailureResult;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Repository
public class MyBatisAdminAccountRepository implements AdminAccountRepository, AdminPasswordRepository {

    private final AdminAccountMapper mapper;

    public MyBatisAdminAccountRepository(AdminAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AdminAccount> findByLoginId(String normalizedLoginId) {
        return aggregate(mapper.findByLoginId(normalizedLoginId));
    }

    @Override
    public Optional<AdminAccount> findById(UUID adminUserId) {
        return aggregate(mapper.findById(adminUserId));
    }

    private static Optional<AdminAccount> aggregate(List<AdminAccountRow> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        AdminAccountRow first = rows.get(0);
        Set<String> roles = new TreeSet<>();
        Set<String> permissions = new TreeSet<>();
        for (AdminAccountRow row : rows) {
            if (row.roleCode() != null) {
                roles.add(row.roleCode());
            }
            if (row.permissionCode() != null) {
                permissions.add(row.permissionCode());
            }
        }

        return Optional.of(new AdminAccount(
                first.adminUserId(),
                first.loginId(),
                first.displayName(),
                first.organizationUnitId(),
                first.passwordHash(),
                AdminAccountStatus.valueOf(first.accountStatus()),
                first.consecutiveFailures(),
                first.lockedUntil(),
                first.mustChangePassword(),
                first.lastLoginAt(),
                roles,
                permissions,
                first.versionNo()
        ));
    }

    @Override
    public boolean recordAuthenticationFailure(
            UUID adminUserId,
            LoginFailureResult failure,
            long expectedVersion
    ) {
        return mapper.updateAuthenticationFailure(
                adminUserId,
                failure.consecutiveFailures(),
                failure.lockedUntil().orElse(null),
                expectedVersion
        ) == 1;
    }

    @Override
    public boolean recordAuthenticationSuccess(UUID adminUserId, Instant loggedInAt, long expectedVersion) {
        return mapper.updateAuthenticationSuccess(adminUserId, loggedInAt, expectedVersion) == 1;
    }

    @Override
    public boolean updatePassword(
            UUID adminUserId,
            String passwordHash,
            boolean mustChangePassword,
            long expectedVersion
    ) {
        return mapper.updatePassword(
                adminUserId,
                passwordHash,
                mustChangePassword,
                expectedVersion
        ) == 1;
    }
}
