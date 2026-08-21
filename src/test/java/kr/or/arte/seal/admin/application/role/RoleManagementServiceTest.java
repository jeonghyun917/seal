package kr.or.arte.seal.admin.application.role;

import kr.or.arte.seal.admin.persistence.role.RoleManagementMapper;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class RoleManagementServiceTest {
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ROLE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID REQUIRED_PERMISSION_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_PERMISSION_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private RoleManagementMapper mapper;
    private RoleManagementService service;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(RoleManagementMapper.class);
        AuditEventWriter auditWriter = org.mockito.Mockito.mock(AuditEventWriter.class);
        service = new RoleManagementService(mapper, auditWriter,
                Clock.fixed(Instant.parse("2026-08-21T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void systemAdminRoleCannotLoseRequiredPermission() {
        given(mapper.findRole(ROLE_ID)).willReturn(List.of(
                new RoleDetail(ROLE_ID, "SYSTEM_ADMIN", "시스템 관리자", true, true, 2)));
        given(mapper.findRequiredSystemAdminPermissionIds()).willReturn(Set.of(REQUIRED_PERMISSION_ID));
        given(mapper.countPermissions(Set.of(OTHER_PERMISSION_ID))).willReturn(1);

        assertThatThrownBy(() -> service.update(new RoleUpdateCommand(
                ROLE_ID, "시스템 관리자", true, Set.of(OTHER_PERMISSION_ID), 2, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(409));

        verify(mapper, never()).update(any());
    }

    @Test
    void roleWithActiveUsersCannotBeDeactivated() {
        given(mapper.findRole(ROLE_ID)).willReturn(List.of(
                new RoleDetail(ROLE_ID, "OPERATOR", "업무 담당자", false, true, 1)));
        given(mapper.countActiveAssignedUsers(ROLE_ID)).willReturn(1);
        given(mapper.countPermissions(Set.of(OTHER_PERMISSION_ID))).willReturn(1);

        assertThatThrownBy(() -> service.update(new RoleUpdateCommand(
                ROLE_ID, "업무 담당자", false, Set.of(OTHER_PERMISSION_ID), 1, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(409));
    }

    @Test
    void createsCustomRoleWithPermissionAssignments() {
        given(mapper.countPermissions(Set.of(OTHER_PERMISSION_ID))).willReturn(1);
        given(mapper.insert(any())).willReturn(1);

        UUID roleId = service.create(new RoleCreateCommand("REQUEST_OPERATOR", "요청 담당자",
                Set.of(OTHER_PERMISSION_ID), ACTOR_ID));

        verify(mapper).replacePermissions(roleId, Set.of(OTHER_PERMISSION_ID), ACTOR_ID);
        assertThat(roleId).isNotNull();
    }
}
