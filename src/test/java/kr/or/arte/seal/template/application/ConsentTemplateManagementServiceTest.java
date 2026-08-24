package kr.or.arte.seal.template.application;

import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.application.activity.ApprovedDataItemReference;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityReference;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentItemType;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class ConsentTemplateManagementServiceTest {

    private static final UUID AUTHOR = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID APPROVER = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID PUBLISHER = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID TEMPLATE_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString("22000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVITY_ID = UUID.fromString("23000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVITY_VERSION_ID = UUID.fromString("24000000-0000-0000-0000-000000000001");
    private static final UUID DATA_ITEM_ID = UUID.fromString("25000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");
    private static final String HASH = "b".repeat(64);

    private ConsentTemplateRepository repository;
    private ApprovedProcessingActivityQuery policyQuery;
    private TemplateHtmlSanitizer sanitizer;
    private TemplateContentHasher hasher;
    private AuditEventWriter auditWriter;
    private ConsentTemplateManagementService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConsentTemplateRepository.class);
        policyQuery = mock(ApprovedProcessingActivityQuery.class);
        sanitizer = mock(TemplateHtmlSanitizer.class);
        hasher = mock(TemplateContentHasher.class);
        auditWriter = mock(AuditEventWriter.class);
        service = new ConsentTemplateManagementService(repository, policyQuery, sanitizer,
                hasher, auditWriter, Clock.fixed(NOW, ZoneOffset.UTC));
        given(policyQuery.findApprovedForTemplate(any(), any())).willReturn(reference());
        given(sanitizer.sanitize(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(hasher.sha256(any())).willReturn(HASH);
    }

    @Test
    void createDerivesRetentionTextFromApprovedPolicy() {
        UUID createdId = service.create(new CreateConsentTemplateCommand(
                "TPL-EDU", "교육 프로그램 동의서", ACTIVITY_VERSION_ID,
                "교육 프로그램 개인정보 동의서", "<p>개인정보 처리 내용을 확인해 주세요.</p>",
                List.of(new ConsentItemDraftCommand("COLLECT", ConsentItemType.COLLECTION_USE,
                        "개인정보 수집 이용 동의", true, "교육 프로그램 운영 목적",
                        "사용자 입력은 무시", "동의를 거부하면 신청이 제한될 수 있습니다.",
                        null, List.of(DATA_ITEM_ID))), AUTHOR));

        ArgumentCaptor<ConsentTemplateDetail> detailCaptor = ArgumentCaptor.forClass(ConsentTemplateDetail.class);
        then(repository).should().insertTemplate(detailCaptor.capture(), any(), any());
        assertThat(createdId).isEqualTo(detailCaptor.getValue().consentTemplateId());
        assertThat(detailCaptor.getValue().version().items()).singleElement()
                .satisfies(item -> assertThat(item.retentionText()).isEqualTo("동의 제출 후 3년"));
    }

    @Test
    void reviewRequestRejectsDataItemOutsideApprovedActivity() {
        ConsentTemplateDetail detail = detail(draftWithDataItem(UUID.randomUUID()));
        given(repository.requireVersionForUpdate(AUTHOR, TEMPLATE_ID, VERSION_ID)).willReturn(detail);

        assertThatThrownBy(() -> service.requestReview(TEMPLATE_ID, VERSION_ID, AUTHOR))
                .isInstanceOf(TemplateBusinessConflictException.class)
                .hasMessage("승인 처리업무와 일치하지 않는 개인정보 항목이 있습니다.");
    }

    @Test
    void authorCannotApproveOwnTemplateVersion() {
        ConsentTemplateVersion requested = draftWithDataItem(DATA_ITEM_ID).requestReview(AUTHOR);
        given(repository.requireVersionForUpdate(AUTHOR, TEMPLATE_ID, VERSION_ID))
                .willReturn(detail(requested));

        assertThatThrownBy(() -> service.approve(TEMPLATE_ID, VERSION_ID, AUTHOR))
                .isInstanceOf(TemplateBusinessConflictException.class)
                .hasMessage("작성자와 승인자는 달라야 합니다.");
    }

    @Test
    void publishRevalidatesActivityAndWritesHashAndAudit() {
        ConsentTemplateVersion approved = draftWithDataItem(DATA_ITEM_ID)
                .requestReview(AUTHOR).approve(APPROVER);
        given(repository.requireVersionForUpdate(PUBLISHER, TEMPLATE_ID, VERSION_ID))
                .willReturn(detail(approved));

        service.publish(TEMPLATE_ID, VERSION_ID, PUBLISHER);

        ArgumentCaptor<Object> hashPayloadCaptor = ArgumentCaptor.forClass(Object.class);
        then(hasher).should().sha256(hashPayloadCaptor.capture());
        assertThat(hashPayloadCaptor.getValue()).isInstanceOf(TemplateContentHashPayload.class);
        TemplateContentHashPayload payload = (TemplateContentHashPayload) hashPayloadCaptor.getValue();
        assertThat(payload.templateCode()).isEqualTo("TPL-EDU");
        assertThat(payload.items()).singleElement()
                .satisfies(item -> assertThat(item.dataItems()).singleElement()
                        .satisfies(data -> assertThat(data.name()).isEqualTo("이메일")));
        ArgumentCaptor<ConsentTemplateVersion> versionCaptor =
                ArgumentCaptor.forClass(ConsentTemplateVersion.class);
        then(repository).should().publish(versionCaptor.capture(), any(), any());
        assertThat(versionCaptor.getValue().contentHash()).isEqualTo(HASH);
        then(auditWriter).should().append(any());
        then(policyQuery).should().findApprovedForTemplate(PUBLISHER, ACTIVITY_VERSION_ID);
    }

    @Test
    void createsNextDraftOnlyFromLatestPublishedVersion() {
        ConsentTemplateVersion published = draftWithDataItem(DATA_ITEM_ID)
                .requestReview(AUTHOR).approve(APPROVER).publish(PUBLISHER, HASH);
        given(repository.requireVersionForUpdate(AUTHOR, TEMPLATE_ID, VERSION_ID))
                .willReturn(detail(published));
        given(repository.isLatestPublishedOrRetired(TEMPLATE_ID, VERSION_ID)).willReturn(true);
        given(repository.nextVersionNumber(TEMPLATE_ID)).willReturn(2);

        UUID newVersionId = service.createNextDraft(TEMPLATE_ID, VERSION_ID, AUTHOR);

        ArgumentCaptor<ConsentTemplateVersion> captor =
                ArgumentCaptor.forClass(ConsentTemplateVersion.class);
        then(repository).should().insertVersion(captor.capture(), any(), any());
        assertThat(captor.getValue().consentTemplateVersionId()).isEqualTo(newVersionId);
        assertThat(captor.getValue().versionNumber()).isEqualTo(2);
        assertThat(captor.getValue().items().get(0).itemId())
                .isNotEqualTo(published.items().get(0).itemId());
    }

    @Test
    void rejectsUnknownTemplateStatus() {
        assertThatThrownBy(() -> service.updateStatus(TEMPLATE_ID, "DELETED", 0, AUTHOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("템플릿 상태가 올바르지 않습니다.");
    }

    private ConsentTemplateDetail detail(ConsentTemplateVersion version) {
        return new ConsentTemplateDetail(TEMPLATE_ID, "TPL-EDU", "교육 프로그램 동의서",
                ACTIVITY_ID, "교육 프로그램 운영", "ACTIVE", null, 0, version);
    }

    private ConsentTemplateVersion draftWithDataItem(UUID dataItemId) {
        return ConsentTemplateVersion.draft(VERSION_ID, TEMPLATE_ID, ACTIVITY_ID,
                ACTIVITY_VERSION_ID, 1, "교육 프로그램 개인정보 동의서",
                "<p>개인정보 처리 내용을 확인해 주세요.</p>",
                List.of(new ConsentItemDefinition(UUID.randomUUID(), 1, "COLLECT",
                        ConsentItemType.COLLECTION_USE, "개인정보 수집 이용 동의", true,
                        "교육 프로그램 운영 목적", "동의 제출 후 3년",
                        "동의를 거부하면 신청이 제한될 수 있습니다.", null,
                        List.of(dataItemId))), AUTHOR);
    }

    private ApprovedProcessingActivityReference reference() {
        return new ApprovedProcessingActivityReference(ACTIVITY_ID, ACTIVITY_VERSION_ID,
                UUID.randomUUID(), "교육 프로그램 운영", "교육 프로그램 운영 목적",
                "CONSENT", UUID.randomUUID(), "동의 제출 후 3년",
                List.of(new ApprovedDataItemReference(DATA_ITEM_ID, 1, "이메일", true)));
    }
}
