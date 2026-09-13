package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.domain.retention.DestructionMethod;
import kr.or.arte.seal.policy.domain.retention.RetentionPeriodUnit;
import kr.or.arte.seal.policy.domain.retention.RetentionPolicyVersion;
import kr.or.arte.seal.policy.domain.retention.RetentionStartEvent;
import kr.or.arte.seal.policy.persistence.RetentionPolicyMapper;
import kr.or.arte.seal.policy.persistence.RetentionPolicyInsert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyManagementServiceTest {
    private static final UUID POLICY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID REVIEWER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Mock RetentionPolicyMapper mapper;
    @Mock AuditEventWriter auditEventWriter;

    @Test
    void createRejectsOrganizationOutsideActorScope() {
        RetentionPolicyManagementService service = service();
        given(mapper.canAccessOrganization(AUTHOR_ID, ORGANIZATION_ID)).willReturn(false);

        assertThatThrownBy(() -> service.create(command()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(mapper, never()).insertPolicy(any());
    }

    @Test
    void createStoresPolicyAndInitialDraftAndWritesAudit() {
        RetentionPolicyManagementService service = service();
        given(mapper.canAccessOrganization(AUTHOR_ID, ORGANIZATION_ID)).willReturn(true);

        service.create(command());

        verify(mapper).insertPolicy(any());
        verify(mapper).insertVersion(any());
        verify(auditEventWriter).append(any());
    }

    @Test
    void createGeneratesPolicyCode() {
        RetentionPolicyManagementService service = service();
        given(mapper.canAccessOrganization(AUTHOR_ID, ORGANIZATION_ID)).willReturn(true);

        service.create(command());

        ArgumentCaptor<RetentionPolicyInsert> policy = ArgumentCaptor.forClass(RetentionPolicyInsert.class);
        verify(mapper).insertPolicy(policy.capture());
        assertThat(policy.getValue().policyCode()).matches("RP-[A-F0-9]{16}");
    }

    @Test
    void authorCannotApproveOwnPolicyVersion() {
        RetentionPolicyManagementService service = service();
        given(mapper.findVisibleVersionForUpdate(AUTHOR_ID, POLICY_ID, VERSION_ID))
                .willReturn(List.of(draft().requestReview(AUTHOR_ID)));

        assertThatThrownBy(() -> service.approve(POLICY_ID, VERSION_ID, AUTHOR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("작성자와 승인자");

        verify(mapper, never()).updateVersionTransition(any(), any(), any());
    }

    @Test
    void reviewerApprovalUpdatesVersionAndCurrentApprovedPointer() {
        RetentionPolicyManagementService service = service();
        given(mapper.findVisibleVersionForUpdate(REVIEWER_ID, POLICY_ID, VERSION_ID))
                .willReturn(List.of(draft().requestReview(AUTHOR_ID)));
        given(mapper.updateVersionTransition(any(), any(), any())).willReturn(1);
        given(mapper.updateCurrentApprovedVersion(POLICY_ID, VERSION_ID, REVIEWER_ID)).willReturn(1);

        service.approve(POLICY_ID, VERSION_ID, REVIEWER_ID);

        verify(mapper).supersedePreviousApprovedVersions(POLICY_ID, VERSION_ID, REVIEWER_ID);
        verify(mapper).updateCurrentApprovedVersion(POLICY_ID, VERSION_ID, REVIEWER_ID);
        verify(auditEventWriter).append(any());
    }

    private RetentionPolicyManagementService service() {
        return new RetentionPolicyManagementService(mapper, auditEventWriter,
                Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC));
    }

    private RetentionPolicyCreateCommand command() {
        return new RetentionPolicyCreateCommand("교육사업 보유정책", ORGANIZATION_ID,
                RetentionStartEvent.CONSENT_SUBMITTED_AT, 3, RetentionPeriodUnit.YEAR,
                30, DestructionMethod.DELETE, AUTHOR_ID);
    }

    private RetentionPolicyVersion draft() {
        return RetentionPolicyVersion.draft(VERSION_ID, POLICY_ID, 1,
                RetentionStartEvent.CONSENT_SUBMITTED_AT, 3, RetentionPeriodUnit.YEAR,
                30, DestructionMethod.DELETE, AUTHOR_ID);
    }
}
