package kr.or.arte.seal.admin.application;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.LoginFailureResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository {

    Optional<AdminAccount> findByLoginId(String normalizedLoginId);

    boolean recordAuthenticationFailure(
            UUID adminUserId,
            LoginFailureResult failure,
            long expectedVersion
    );

    boolean recordAuthenticationSuccess(UUID adminUserId, Instant loggedInAt, long expectedVersion);
}
