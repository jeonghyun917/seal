package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;

import java.util.Optional;
import java.util.UUID;

public interface AdminPasswordRepository {

    Optional<AdminAccount> findById(UUID adminUserId);

    boolean updatePassword(
            UUID adminUserId,
            String passwordHash,
            boolean mustChangePassword,
            long expectedVersion
    );
}
