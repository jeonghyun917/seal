package kr.or.arte.seal.request.application;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.link.application.SecureLinkInsert;
import kr.or.arte.seal.link.application.SecureLinkRepository;
import kr.or.arte.seal.recipient.application.RecipientRepository;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import kr.or.arte.seal.template.application.PublishedConsentTemplateReference;
import kr.or.arte.seal.template.application.PublishedTemplateView;
import kr.or.arte.seal.template.domain.ConsentItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentRequestManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T05:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID RECIPIENT = UUID.randomUUID();
    private static final UUID TEMPLATE = UUID.randomUUID();
    private static final UUID TEMPLATE_VERSION = UUID.randomUUID();
    private FakeRequestRepository requests;
    private ConsentRequestManagementService service;
    private PublishedConsentTemplateReference publishedTemplate;
    private final List<UUID> expiredSessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        requests = new FakeRequestRepository();
        RecipientRepository recipients = new StubRecipientRepository();
        publishedTemplate = template();
        PublishedConsentTemplateQuery templates = (actor, version) -> publishedTemplate;
        List<AuditEventCommand> audits = new ArrayList<>();
        service = new ConsentRequestManagementService(requests, recipients, templates,
                new RequestSnapshotFactory(new com.fasterxml.jackson.databind.ObjectMapper()), audits::add,
                new NoOpLinkRepository(), (recipientId, today) -> false,
                (requestId, now) -> expiredSessions.add(requestId), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingRequestWithoutSecondInsert() {
        UUID key = UUID.randomUUID();
        CreateConsentRequestCommand command = new CreateConsentRequestCommand(ORG, RECIPIENT,
                TEMPLATE_VERSION, NOW.plusSeconds(7200), key);

        var first = service.create(ACTOR, command);
        var second = service.create(ACTOR, command);

        assertThat(second.requestId()).isEqualTo(first.requestId());
        assertThat(second.existing()).isTrue();
        assertThat(requests.insertCount).isEqualTo(1);
    }

    @Test
    void finalizePersistsSnapshotAndMovesDraftToReady() {
        var created = service.create(ACTOR, new CreateConsentRequestCommand(ORG, RECIPIENT,
                TEMPLATE_VERSION, NOW.plusSeconds(7200), UUID.randomUUID()));

        service.finalizeRequest(ACTOR, created.requestId(), 0);

        assertThat(requests.rows.get(created.requestId()).status()).isEqualTo(ConsentRequestStatus.READY);
        assertThat(requests.snapshot).isNotNull();
        assertThat(requests.events).contains("CREATED", "FINALIZED");
    }

    @Test
    void updateDraftRevalidatesReferencesAndUsesOptimisticVersion() {
        var created = service.create(ACTOR, new CreateConsentRequestCommand(ORG, RECIPIENT,
                TEMPLATE_VERSION, NOW.plusSeconds(7200), UUID.randomUUID()));
        service.updateDraft(ACTOR, created.requestId(), new UpdateConsentRequestCommand(
                RECIPIENT, TEMPLATE_VERSION, NOW.plusSeconds(10800), 0));
        assertThat(requests.rows.get(created.requestId()).expiresAt()).isEqualTo(NOW.plusSeconds(10800));
        assertThat(requests.rows.get(created.requestId()).versionNo()).isEqualTo(1);
    }

    @Test
    void cancelExpiresAllPublicSessionsForTheRequest() {
        var created = service.create(ACTOR, new CreateConsentRequestCommand(ORG, RECIPIENT,
                TEMPLATE_VERSION, NOW.plusSeconds(7200), UUID.randomUUID()));

        service.cancel(ACTOR, created.requestId(), 0, "operator requested cancellation");

        assertThat(expiredSessions).containsExactly(created.requestId());
    }

    private PublishedConsentTemplateReference template() {
        UUID activity = UUID.randomUUID(); UUID activityVersion = UUID.randomUUID();

        UUID retentionVersion = UUID.randomUUID(); UUID dataItem = UUID.randomUUID();
        var item = new PublishedTemplateView.Item(1, "COLLECT", ConsentItemType.COLLECTION_USE,
                "개인정보 수집 이용 동의", true, "교육 프로그램 운영 목적", "동의 제출 후 3년",
                "거부 시 신청이 제한될 수 있습니다.", null, List.of("이름"));
        return new PublishedConsentTemplateReference(TEMPLATE, TEMPLATE_VERSION, activity,
                activityVersion, retentionVersion, "동의 제출 후 3년", "a".repeat(64),
                new PublishedTemplateView("교육 개인정보 동의서", "<p>내용을 확인하세요.</p>",
                        "교육 프로그램", "교육 프로그램 운영", List.of(item)), Map.of(1, List.of(dataItem)));
    }

    private static final class FakeRequestRepository implements ConsentRequestRepository {
        final Map<UUID, ConsentRequestRecord> rows = new HashMap<>();
        final List<String> events = new ArrayList<>();
        int insertCount;
        RequestSnapshot snapshot;
        @Override public Optional<ConsentRequestRecord> findByIdempotency(UUID actor, UUID org, UUID key) {
            return rows.values().stream().filter(row -> row.organizationUnitId().equals(org)
                    && row.idempotencyKey().equals(key)).findFirst();
        }
        @Override public void insert(ConsentRequestRecord row, UUID actor, Instant now) {
            rows.put(row.requestId(), row); insertCount++;
        }
        @Override public Optional<ConsentRequestRecord> findScopedForUpdate(UUID actor, UUID id) {
            return Optional.ofNullable(rows.get(id));
        }
        @Override public Optional<ConsentRequestRecord> findScoped(UUID actor, UUID id) {
            return Optional.ofNullable(rows.get(id));
        }
        @Override public void insertSnapshot(UUID requestId, UUID snapshotId, RequestSnapshot value,
                UUID actor, Instant now) { snapshot = value; }
        @Override public boolean markReady(UUID id, UUID snapshotId, UUID actor, Instant now, long version) {
            ConsentRequestRecord old = rows.get(id);
            rows.put(id, old.withStatus(ConsentRequestStatus.READY, snapshotId, old.versionNo() + 1));
            return true;
        }
        @Override public boolean updateDraft(ConsentRequestRecord changed, UUID actor, Instant now, long version) {
            rows.put(changed.requestId(), changed); return true;
        }
        @Override public boolean cancel(UUID id, UUID actor, Instant now, String reason, long version) { return true; }
        @Override public void insertEvent(UUID id, String type, String actorType, UUID actor,
                String reason, Instant now) { events.add(type); }
        @Override public List<ConsentRequestRecord> searchScoped(UUID actor, ConsentRequestStatus status) {
            return List.copyOf(rows.values());
        }
    }

    private static final class StubRecipientRepository implements RecipientRepository {
        private final RecipientStoredRecord row = new RecipientStoredRecord(RECIPIENT, ORG,
                new byte[30], new byte[30], "v1", "a".repeat(64), "교육 참여 신청서 수집",
                RecipientStatus.ACTIVE, 0);
        @Override public boolean canAccessActiveOrganization(UUID a, UUID o) { return true; }
        @Override public Optional<RecipientStoredRecord> findActiveByEmailHmac(UUID o, String h, UUID e) { return Optional.empty(); }
        @Override public void insert(RecipientStoredRecord r, UUID a, Instant n) { }
        @Override public Optional<RecipientStoredRecord> findScoped(UUID a, UUID id) { return RECIPIENT.equals(id) ? Optional.of(row) : Optional.empty(); }
        @Override public boolean update(RecipientStoredRecord r, UUID a, Instant n, long v) { return false; }
        @Override public List<RecipientStoredRecord> searchScoped(UUID a, String h, RecipientStatus s) { return List.of(row); }
    }

    private static final class NoOpLinkRepository implements SecureLinkRepository {
        @Override public void insert(SecureLinkInsert value) { }
        @Override public void revokeCurrent(UUID requestId, UUID actorId, Instant now, String reason) { }
        @Override public boolean activateRequest(UUID r, UUID l, UUID a, Instant n, long v) { return false; }
        @Override public boolean replaceCurrentLink(UUID r, UUID l, UUID a, Instant n, long v) { return false; }
    }
}
