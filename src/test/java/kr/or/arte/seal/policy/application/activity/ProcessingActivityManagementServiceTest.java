package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.domain.activity.LegalBasisType;
import kr.or.arte.seal.policy.domain.activity.ProcessingDataItem;
import kr.or.arte.seal.policy.persistence.activity.ProcessingActivityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class ProcessingActivityManagementServiceTest {
    private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID POLICY_VERSION_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private ProcessingActivityMapper mapper;
    private ProcessingActivityManagementService service;

    @BeforeEach
    void setUp() {
        mapper = org.mockito.Mockito.mock(ProcessingActivityMapper.class);
        service = new ProcessingActivityManagementService(mapper,
                org.mockito.Mockito.mock(AuditEventWriter.class),
                Clock.fixed(Instant.parse("2026-08-21T08:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsActivityAndFirstDraftWithGeneralItems() {
        given(mapper.canAccessActiveOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(true);
        given(mapper.canUseApprovedRetentionVersion(ACTOR_ID, POLICY_VERSION_ID)).willReturn(true);
        given(mapper.insertActivity(any())).willReturn(1);
        given(mapper.insertVersion(any())).willReturn(1);
        given(mapper.insertDataItems(any())).willReturn(2);

        UUID activityId = service.create(command(List.of(
                new ProcessingDataItem("성명", "참여자 식별", true),
                new ProcessingDataItem("이메일", "교육 안내 발송", true))));

        assertThat(activityId).isNotNull();
        verify(mapper).insertDataItems(org.mockito.ArgumentMatchers.argThat(items -> items.size() == 2));
    }

    @Test
    void rejectsRetentionVersionThatIsNotCurrentApprovedAndVisible() {
        given(mapper.canAccessActiveOrganization(ACTOR_ID, ORGANIZATION_ID)).willReturn(true);
        given(mapper.canUseApprovedRetentionVersion(ACTOR_ID, POLICY_VERSION_ID)).willReturn(false);

        assertThatThrownBy(() -> service.create(command(List.of(
                new ProcessingDataItem("성명", "참여자 식별", true)))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value())
                        .isEqualTo(404));

        verify(mapper, never()).insertActivity(any());
    }

    private static ProcessingActivityCreateCommand command(List<ProcessingDataItem> items) {
        return new ProcessingActivityCreateCommand("EDU-PROGRAM", "교육 프로그램 운영",
                ORGANIZATION_ID, "교육 프로그램 참여자를 등록하고 운영하기 위한 목적입니다.",
                LegalBasisType.CONSENT, "정보주체의 개인정보 수집·이용 동의",
                POLICY_VERSION_ID, items, ACTOR_ID);
    }
}
