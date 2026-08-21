package kr.or.arte.seal.admin.application.organization;

import kr.or.arte.seal.admin.persistence.organization.OrganizationMapper;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrganizationManagementServiceTest {
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private OrganizationMapper mapper;
    private AuditEventWriter auditEventWriter;
    private OrganizationManagementService service;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(OrganizationMapper.class);
        auditEventWriter = org.mockito.Mockito.mock(AuditEventWriter.class);
        service = new OrganizationManagementService(mapper, auditEventWriter,
                Clock.fixed(Instant.parse("2026-08-21T07:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void cannotCreateOrganizationBelowParentOutsideActorScope() {
        given(mapper.canAccessOrganization(ACTOR_ID, PARENT_ID)).willReturn(false);

        assertThatThrownBy(() -> service.create(new OrganizationCreateCommand(
                "TEAM-A", "교육사업팀", PARENT_ID, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(404));

        verify(mapper, never()).insert(any());
    }

    @Test
    void createsChildOrganizationAndWritesAuditEvent() {
        given(mapper.canAccessOrganization(ACTOR_ID, PARENT_ID)).willReturn(true);
        given(mapper.insert(any())).willReturn(1);

        UUID createdId = service.create(new OrganizationCreateCommand(
                "TEAM-A", "교육사업팀", PARENT_ID, ACTOR_ID));

        ArgumentCaptor<OrganizationInsert> insert = ArgumentCaptor.forClass(OrganizationInsert.class);
        verify(mapper).insert(insert.capture());
        assertThat(insert.getValue().organizationUnitId()).isEqualTo(createdId);
        assertThat(insert.getValue().organizationName()).isEqualTo("교육사업팀");
        verify(auditEventWriter).append(any());
    }

    @Test
    void cannotDeactivateOrganizationWithActiveDependencies() {
        given(mapper.findVisibleOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(List.of(
                new OrganizationDetail(ORGANIZATION_ID, "TEAM-A", "교육사업팀", PARENT_ID,
                        "본부", false, true, 2)));
        given(mapper.canAccessOrganization(ACTOR_ID, PARENT_ID)).willReturn(true);
        given(mapper.wouldCreateCycle(ORGANIZATION_ID, PARENT_ID)).willReturn(false);
        given(mapper.countActiveDependencies(ORGANIZATION_ID)).willReturn(1);

        assertThatThrownBy(() -> service.update(new OrganizationUpdateCommand(
                ORGANIZATION_ID, "교육사업팀", PARENT_ID, false, 2, ACTOR_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(409));

        verify(mapper, never()).update(any());
    }

    @Test
    void updatesVisibleOrganizationWithOptimisticVersionAndAudit() {
        given(mapper.findVisibleOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(List.of(
                new OrganizationDetail(ORGANIZATION_ID, "TEAM-A", "교육사업팀", PARENT_ID,
                        "본부", false, true, 2)));
        given(mapper.canAccessOrganization(ACTOR_ID, PARENT_ID)).willReturn(true);
        given(mapper.wouldCreateCycle(ORGANIZATION_ID, PARENT_ID)).willReturn(false);
        given(mapper.update(any())).willReturn(1);

        service.update(new OrganizationUpdateCommand(
                ORGANIZATION_ID, "교육운영팀", PARENT_ID, true, 2, ACTOR_ID));

        ArgumentCaptor<OrganizationUpdate> update = ArgumentCaptor.forClass(OrganizationUpdate.class);
        verify(mapper).update(update.capture());
        assertThat(update.getValue().organizationName()).isEqualTo("교육운영팀");
        assertThat(update.getValue().versionNo()).isEqualTo(2);
        verify(auditEventWriter).append(any());
    }
}
