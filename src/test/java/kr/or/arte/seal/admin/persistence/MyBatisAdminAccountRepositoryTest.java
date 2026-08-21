package kr.or.arte.seal.admin.persistence;

import kr.or.arte.seal.admin.domain.AdminAccount;
import kr.or.arte.seal.admin.domain.LoginFailureResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MyBatisAdminAccountRepositoryTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void duplicateJoinRowsBecomeOneAccountWithDistinctAuthorities() {
        CapturingMapper mapper = new CapturingMapper(List.of(
                row("SYSTEM_ADMIN", "USER_MANAGE"),
                row("SYSTEM_ADMIN", "AUDIT_VIEW"),
                row("AUDITOR", "AUDIT_VIEW")
        ));

        AdminAccount account = new MyBatisAdminAccountRepository(mapper)
                .findByLoginId("admin")
                .orElseThrow();

        assertThat(account.roles()).containsExactlyInAnyOrder("SYSTEM_ADMIN", "AUDITOR");
        assertThat(account.permissions()).containsExactlyInAnyOrder("USER_MANAGE", "AUDIT_VIEW");
    }

    @Test
    void failureWithoutLockWritesSqlNullForLockedUntil() {
        CapturingMapper mapper = new CapturingMapper(List.of(row(null, null)));
        MyBatisAdminAccountRepository repository = new MyBatisAdminAccountRepository(mapper);

        boolean updated = repository.recordAuthenticationFailure(
                USER_ID,
                new LoginFailureResult(2, Optional.empty()),
                7
        );

        assertThat(updated).isTrue();
        assertThat(mapper.failureCount).isEqualTo(2);
        assertThat(mapper.lockedUntil).isNull();
        assertThat(mapper.expectedVersion).isEqualTo(7);
    }

    private static AdminAccountRow row(String roleCode, String permissionCode) {
        return new AdminAccountRow(
                USER_ID,
                "admin",
                "관리자",
                ORGANIZATION_ID,
                "$2a$12$123456789012345678901u12345678901234567890123456789012",
                "ACTIVE",
                1,
                null,
                true,
                null,
                7,
                roleCode,
                permissionCode
        );
    }

    private static final class CapturingMapper implements AdminAccountMapper {
        private final List<AdminAccountRow> rows;
        private int failureCount;
        private Instant lockedUntil;
        private long expectedVersion;

        private CapturingMapper(List<AdminAccountRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<AdminAccountRow> findByLoginId(String loginId) {
            return rows;
        }

        @Override
        public int updateAuthenticationFailure(
                UUID adminUserId,
                int consecutiveFailures,
                Instant lockedUntil,
                long expectedVersion
        ) {
            this.failureCount = consecutiveFailures;
            this.lockedUntil = lockedUntil;
            this.expectedVersion = expectedVersion;
            return 1;
        }

        @Override
        public int updateAuthenticationSuccess(UUID adminUserId, Instant loggedInAt, long expectedVersion) {
            return 1;
        }
    }
}
