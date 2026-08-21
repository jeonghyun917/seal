package kr.or.arte.seal.admin.application.user;

import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.admin.persistence.user.AdminUserManagementMapper;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminUserManagementServiceTest {
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ROLE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private AdminUserManagementMapper mapper;
    private PasswordEncoder passwordEncoder;
    private AuditEventWriter auditEventWriter;
    private AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(AdminUserManagementMapper.class);
        passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        auditEventWriter = org.mockito.Mockito.mock(AuditEventWriter.class);
        service = new AdminUserManagementService(mapper, passwordEncoder,
                new PasswordPolicy(12, 128), auditEventWriter,
                Clock.fixed(Instant.parse("2026-08-21T07:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void cannotCreateAdministratorOutsideActorOrganizationScope() {
        given(mapper.canAccessOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(false);

        assertThatThrownBy(() -> service.create(createCommand()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(404));

        verify(mapper, never()).insert(any());
    }

    @Test
    void createsAdministratorWithEncodedTemporaryPasswordAssignmentsAndAudit() {
        given(mapper.canAccessOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(true);
        given(mapper.countActiveRoles(Set.of(ROLE_ID))).willReturn(1);
        given(passwordEncoder.encode("temporary-123!")).willReturn("encoded-password-hash");
        given(mapper.insert(any())).willReturn(1);

        UUID createdId = service.create(createCommand());

        ArgumentCaptor<AdminUserInsert> insert = ArgumentCaptor.forClass(AdminUserInsert.class);
        verify(mapper).insert(insert.capture());
        assertThat(insert.getValue().adminUserId()).isEqualTo(createdId);
        assertThat(insert.getValue().passwordHash()).isEqualTo("encoded-password-hash");
        verify(mapper).replaceRoles(createdId, Set.of(ROLE_ID), ACTOR_ID);
        verify(mapper).replaceOrganizationScopes(createdId, Set.of(ORGANIZATION_ID), ACTOR_ID);
        ArgumentCaptor<AuditEventCommand> audit = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditEventWriter).append(audit.capture());
        assertThat(audit.getValue().metadata().toString()).doesNotContain("temporary-123!");
    }

    @Test
    void administratorCannotDeactivateOwnAccount() {
        given(mapper.findVisibleUser(ACTOR_ID, ACTOR_ID)).willReturn(List.of(
                new AdminUserDetail(ACTOR_ID, "seal-admin", "관리자", ORGANIZATION_ID,
                        "본부", "ACTIVE", 0, null, false, 3)));

        assertThatThrownBy(() -> service.update(new AdminUserUpdateCommand(
                ACTOR_ID, "관리자", ORGANIZATION_ID, "INACTIVE",
                Set.of(ROLE_ID), Set.of(ORGANIZATION_ID), 3, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(409));
    }

    @Test
    void unlockResetsFailureStateUsingOptimisticVersion() {
        given(mapper.findVisibleUser(ACTOR_ID, USER_ID)).willReturn(List.of(
                new AdminUserDetail(USER_ID, "locked-admin", "잠금 관리자", ORGANIZATION_ID,
                        "본부", "LOCKED", 5, Instant.parse("2026-08-21T07:15:00Z"), false, 4)));
        given(mapper.unlock(USER_ID, 4, ACTOR_ID)).willReturn(1);

        service.unlock(USER_ID, ACTOR_ID);

        verify(mapper).unlock(USER_ID, 4, ACTOR_ID);
        verify(auditEventWriter).append(any());
    }

    @Test
    void passwordResetStoresOnlyEncodedPasswordAndWritesAuditWithoutSecret() {
        given(mapper.findVisibleUser(ACTOR_ID, USER_ID)).willReturn(List.of(
                new AdminUserDetail(USER_ID, "locked-admin", "잠금 관리자", ORGANIZATION_ID,
                        "본부", "LOCKED", 5, null, false, 4)));
        given(passwordEncoder.encode("temporary-456!")).willReturn("encoded-reset-hash");
        given(mapper.resetPassword(USER_ID, "encoded-reset-hash", 4, ACTOR_ID)).willReturn(1);

        service.resetPassword(USER_ID, "temporary-456!", ACTOR_ID);

        verify(mapper).resetPassword(USER_ID, "encoded-reset-hash", 4, ACTOR_ID);
        ArgumentCaptor<AuditEventCommand> audit = ArgumentCaptor.forClass(AuditEventCommand.class);
        verify(auditEventWriter).append(audit.capture());
        assertThat(audit.getValue().metadata().toString()).doesNotContain("temporary-456!");
    }

    @Test
    void cannotRemoveSystemAdminRoleFromLastSystemAdministrator() {
        given(mapper.findVisibleUser(ACTOR_ID, USER_ID)).willReturn(List.of(
                new AdminUserDetail(USER_ID, "system-admin", "시스템 관리자", ORGANIZATION_ID,
                        "본부", "ACTIVE", 0, null, false, 2)));
        given(mapper.canAccessOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(true);
        given(mapper.countActiveRoles(Set.of(ROLE_ID))).willReturn(1);
        given(mapper.hasSystemAdminRole(USER_ID)).willReturn(true);
        given(mapper.includesSystemAdminRole(Set.of(ROLE_ID))).willReturn(false);
        given(mapper.countActiveSystemAdministrators()).willReturn(1);
        given(mapper.update(any())).willReturn(1);

        assertThatThrownBy(() -> service.update(new AdminUserUpdateCommand(
                USER_ID, "시스템 관리자", ORGANIZATION_ID, "ACTIVE",
                Set.of(ROLE_ID), Set.of(ORGANIZATION_ID), 2, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(409));

        verify(mapper, never()).update(any());
    }

    private static AdminUserCreateCommand createCommand() {
        return new AdminUserCreateCommand("new-admin", "신규 관리자", ORGANIZATION_ID,
                "temporary-123!", Set.of(ROLE_ID), Set.of(ORGANIZATION_ID), ACTOR_ID);
    }
}
