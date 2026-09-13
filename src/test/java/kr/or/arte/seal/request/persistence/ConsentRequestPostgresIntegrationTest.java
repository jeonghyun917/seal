package kr.or.arte.seal.request.persistence;

import kr.or.arte.seal.recipient.application.CreateRecipientCommand;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.delivery.application.MailOutboxQueryService;
import kr.or.arte.seal.delivery.application.MailOutboxStatus;
import kr.or.arte.seal.consent.application.ConsentDecision;
import kr.or.arte.seal.consent.application.ConsentResponseStatus;
import kr.or.arte.seal.consent.application.ConsentSubmissionService;
import kr.or.arte.seal.consent.application.PublicConsentEntryService;
import kr.or.arte.seal.consent.application.PublicConsentEntryStatus;
import kr.or.arte.seal.consent.application.PublicConsentQueryService;
import kr.or.arte.seal.consent.application.PublicConsentSessionExpiryService;
import kr.or.arte.seal.request.application.ConsentRequestManagementService;
import kr.or.arte.seal.request.application.CreateConsentRequestCommand;
import kr.or.arte.seal.request.application.RequestBusinessConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class ConsentRequestPostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RecipientManagementService recipients;
    @Autowired ConsentRequestManagementService requests;
    @Autowired kr.or.arte.seal.request.application.ConsentRequestQueryService requestQuery;
    @Autowired kr.or.arte.seal.retention.application.RetentionJobService retentionJobs;
    @Autowired ConsentDeliveryService delivery;
    @Autowired MailOutboxQueryService mailOutbox;
    @Autowired PublicConsentEntryService publicEntry;
    @Autowired PublicConsentQueryService publicQuery;
    @Autowired ConsentSubmissionService submissions;
    @Autowired PublicConsentSessionExpiryService sessionExpiry;

    @Test
    void listQueriesExcludeOtherActivitiesBeforePagination() {
        UUID system=id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
        UUID org=id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
        UUID creator=fixtureCreator();
        Fixture allowed=publishedFixture(org,creator,system), denied=publishedFixture(org,creator,system);
        UUID actor=UUID.randomUUID();
        jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash,must_change_password,created_by) SELECT ?,?,?,?,password_hash,FALSE,? FROM seal.admin_user WHERE admin_user_id=?",
                actor,"activity-limited-"+actor.toString().substring(0,8),"처리업무 제한 관리자",org,system,system);
        jdbc.update("INSERT INTO seal.user_scope(user_scope_id,admin_user_id,scope_type,scope_target_id,include_descendants,assigned_by) VALUES(?,?,'ORGANIZATION',?,FALSE,?)",
                UUID.randomUUID(),actor,org,system);
        jdbc.update("INSERT INTO seal.user_processing_activity_scope(admin_user_id,processing_activity_id,assigned_by) VALUES(?,?,?)",
                actor,allowed.activityId(),system);
        UUID recipient=recipients.create(new CreateRecipientCommand(system,org,"목록 범위 대상자",
                "scope-"+UUID.randomUUID()+"@example.com","처리업무 범위 테스트"));
        var visible=requests.create(system,new CreateConsentRequestCommand(org,recipient,allowed.templateVersionId(),Instant.now().plusSeconds(7200),UUID.randomUUID()));
        var hidden=requests.create(system,new CreateConsentRequestCommand(org,recipient,denied.templateVersionId(),Instant.now().plusSeconds(7200),UUID.randomUUID()));
        requests.finalizeRequest(system,visible.requestId(),0); requests.finalizeRequest(system,hidden.requestId(),0);
        delivery.activateAndQueue(system,visible.requestId(),1,"http://localhost:9090",null);
        delivery.activateAndQueue(system,hidden.requestId(),1,"http://localhost:9090",null);
        UUID visibleJob=UUID.randomUUID(), hiddenJob=UUID.randomUUID();
        jdbc.update("INSERT INTO seal.retention_job(retention_job_id,consent_request_id,recipient_id,retention_policy_version_id,anchor_date,due_date,job_status,destruction_method,requested_at) VALUES(?,?,?,?,CURRENT_DATE,CURRENT_DATE,'DUE','DELETE',CURRENT_TIMESTAMP)",
                visibleJob,visible.requestId(),recipient,allowed.retentionVersionId());
        jdbc.update("INSERT INTO seal.retention_job(retention_job_id,consent_request_id,recipient_id,retention_policy_version_id,anchor_date,due_date,job_status,destruction_method,requested_at) VALUES(?,?,?,?,CURRENT_DATE,CURRENT_DATE,'DUE','DELETE',CURRENT_TIMESTAMP)",
                hiddenJob,hidden.requestId(),recipient,denied.retentionVersionId());
        jdbc.update("INSERT INTO seal.legal_hold(legal_hold_id,consent_request_id,hold_reason_code,reason_text,active,held_by,held_at) VALUES(?,?,?,? ,TRUE,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID(),visible.requestId(),"VISIBLE_HOLD","허용 처리업무",system);
        jdbc.update("INSERT INTO seal.legal_hold(legal_hold_id,consent_request_id,hold_reason_code,reason_text,active,held_by,held_at) VALUES(?,?,?,? ,TRUE,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID(),hidden.requestId(),"HIDDEN_HOLD","비허용 처리업무",system);

        assertThat(requestQuery.searchPage(actor,null,null,1,20).items()).extracting(item->item.requestId())
                .contains(visible.requestId()).doesNotContain(hidden.requestId());
        assertThat(mailOutbox.searchPage(actor,null,null,1,20).items()).extracting(item->item.requestId())
                .contains(visible.requestId()).doesNotContain(hidden.requestId());
        assertThat(retentionJobs.searchPage(actor,null,1,20).items()).extracting(item->item.id())
                .contains(visibleJob).doesNotContain(hiddenJob);
        assertThat(retentionJobs.activeLegalHolds(actor)).extracting(item->item.consentRequestId())
                .contains(visible.requestId()).doesNotContain(hidden.requestId());
        assertThat(retentionJobs.require(actor,visibleJob).id()).isEqualTo(visibleJob);
        assertThatThrownBy(()->retentionJobs.require(actor,hiddenJob)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void createsAndFinalizesRequestWithImmutableSnapshotRows() {
        UUID actor = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
        UUID org = id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
        UUID creator = fixtureCreator();
        Fixture fixture = publishedFixture(org, creator, actor);
        UUID recipient = recipients.create(new CreateRecipientCommand(actor, org, "요청 대상자",
                "request-" + UUID.randomUUID() + "@example.com", "교육 프로그램 신청서 직접 수집"));

        var created = requests.create(actor, new CreateConsentRequestCommand(org, recipient,
                fixture.templateVersionId(), Instant.now().plusSeconds(7200), UUID.randomUUID()));
        requests.finalizeRequest(actor, created.requestId(), 0);
        var issued = delivery.activateAndQueue(actor, created.requestId(), 1, "http://localhost:9090", null);
        var reissued = delivery.reissueAndQueue(actor, created.requestId(), 2, "http://localhost:9090", null);

        assertThat(jdbc.queryForObject("SELECT request_status FROM seal.consent_request WHERE consent_request_id=?",
                String.class, created.requestId())).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.consent_request_snapshot WHERE consent_request_id=?",
                Integer.class, created.requestId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT token_hmac FROM seal.secure_link WHERE consent_request_id=? AND link_status='ISSUED'",
                String.class, created.requestId())).hasSize(64).isNotEqualTo(reissued.issuedLink().token());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.secure_link WHERE consent_request_id=? AND link_status='REVOKED'",
                Integer.class, created.requestId())).isEqualTo(1);
        publicEntry.start(reissued.issuedLink().token());
        jdbc.update("INSERT INTO seal.legal_hold(legal_hold_id,consent_request_id,hold_reason_code,reason_text,held_by,held_at) VALUES(?,?,'RIGHTS_RESTRICTION','integration restriction',?,CURRENT_TIMESTAMP)",
                UUID.randomUUID(), created.requestId(), actor);
        assertThatThrownBy(() -> delivery.reissueAndQueue(actor, created.requestId(), 3,
                "http://localhost:9090", null))
                .isInstanceOf(RequestBusinessConflictException.class);
        requests.cancel(actor, created.requestId(), 3, "통합테스트 관리자 취소");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.public_consent_session WHERE consent_request_id=? AND session_status='EXPIRED' AND flow_hmac IS NULL AND flow_destroyed_at IS NOT NULL",
                Integer.class, created.requestId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.secure_link WHERE consent_request_id=? AND link_status IN ('ISSUED','ACCESSED')",
                Integer.class, created.requestId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.consent_request_item_data data JOIN seal.consent_request_item item ON item.consent_request_item_id=data.consent_request_item_id JOIN seal.consent_request_snapshot snapshot ON snapshot.consent_request_snapshot_id=item.consent_request_snapshot_id WHERE snapshot.consent_request_id=?",
                Integer.class, created.requestId())).isEqualTo(1);
        var mailRows = mailOutbox.search(actor, null);
        assertThat(mailRows).filteredOn(row -> row.requestId().equals(created.requestId()))
                .extracting(row -> row.status()).containsExactlyInAnyOrder(MailOutboxStatus.CANCELLED, MailOutboxStatus.QUEUED);
        assertThat(mailOutbox.require(actor, reissued.outboxId()).html())
                .contains("http://localhost:9090/consent/").doesNotContain("<form").doesNotContain("<script");
    }

    @Test
    void publicFlowDoesNotMutateOnGetAndCompletesExactlyOnceOnPost() {
        UUID actor = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
        UUID org = id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
        UUID creator = fixtureCreator();
        Fixture fixture = publishedFixture(org, creator, actor);
        UUID recipient = recipients.create(new CreateRecipientCommand(actor, org, "공개동의 대상자",
                "public-" + UUID.randomUUID() + "@example.com", "교육 프로그램 신청서 직접 수집"));
        var request = requests.create(actor, new CreateConsentRequestCommand(org, recipient,
                fixture.templateVersionId(), Instant.now().plusSeconds(7200), UUID.randomUUID()));
        requests.finalizeRequest(actor, request.requestId(), 0);
        var deliveryResult = delivery.activateAndQueue(actor, request.requestId(), 1,
                "http://localhost:9090", null);
        String token = deliveryResult.issuedLink().token();

        assertThat(publicEntry.inspect(token).status()).isEqualTo(PublicConsentEntryStatus.VALID);
        assertThat(jdbc.queryForObject("SELECT link_status FROM seal.secure_link WHERE secure_link_id=?",
                String.class, deliveryResult.issuedLink().linkId())).isEqualTo("ISSUED");

        var abandoned = publicEntry.start(token);
        publicEntry.start(token);
        jdbc.update("UPDATE seal.public_consent_session SET created_at=CURRENT_TIMESTAMP-INTERVAL '2 minutes',expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE consent_request_id=? AND session_status='ACTIVE'",
                request.requestId());
        assertThat(sessionExpiry.expireDue(100)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.public_consent_session WHERE consent_request_id=? AND session_status='EXPIRED' AND flow_hmac IS NULL AND flow_destroyed_at IS NOT NULL",
                Integer.class, request.requestId())).isEqualTo(2);
        var flow = publicEntry.start(token);
        assertThat(jdbc.queryForObject("SELECT link_status FROM seal.secure_link WHERE secure_link_id=?",
                String.class, deliveryResult.issuedLink().linkId())).isEqualTo("ACCESSED");
        var page = publicQuery.load(flow.flowId());
        var decisions = new java.util.LinkedHashMap<UUID, ConsentDecision>();
        page.items().forEach(item -> decisions.put(item.itemId(), ConsentDecision.AGREE));
        UUID idempotency = UUID.randomUUID();
        var submitted = submissions.submit(flow.flowId(), decisions, idempotency);
        var repeated = submissions.submit(flow.flowId(), decisions, idempotency);

        assertThat(submitted.status()).isEqualTo(ConsentResponseStatus.SUBMITTED);
        assertThat(repeated.existing()).isTrue();
        assertThat(repeated.responseId()).isEqualTo(submitted.responseId());
        assertThat(jdbc.queryForObject("SELECT request_status FROM seal.consent_request WHERE consent_request_id=?",
                String.class, request.requestId())).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.consent_response WHERE consent_request_id=?",
                Integer.class, request.requestId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.public_consent_session WHERE consent_request_id=? AND session_status='EXPIRED' AND flow_hmac IS NULL AND flow_destroyed_at IS NOT NULL",
                Integer.class, request.requestId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.public_consent_session WHERE consent_request_id=? AND session_status='COMPLETED' AND flow_hmac IS NOT NULL",
                Integer.class, request.requestId())).isEqualTo(1);
        assertThat(abandoned.flowId()).isNotEqualTo(flow.flowId());
    }

    private Fixture publishedFixture(UUID org, UUID creator, UUID approver) {
        UUID retention = UUID.randomUUID(), retentionVersion = UUID.randomUUID();
        UUID activity = UUID.randomUUID(), activityVersion = UUID.randomUUID(), dataItem = UUID.randomUUID();
        UUID template = UUID.randomUUID(), templateVersion = UUID.randomUUID(), consentItem = UUID.randomUUID();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO seal.retention_policy(retention_policy_id,policy_code,policy_name,owner_organization_unit_id,created_by,updated_by) VALUES(?,?,?,?,?,?)",
                retention, "IT-RP-" + suffix, "통합테스트 보유정책", org, creator, creator);
        jdbc.update("INSERT INTO seal.retention_policy_version(retention_policy_version_id,retention_policy_id,version_number,workflow_status,start_event,period_value,period_unit,destruction_method,created_by,review_requested_by,review_requested_at,approved_by,approved_at,updated_by) VALUES(?,?,1,'APPROVED','CONSENT_SUBMITTED_AT',3,'YEAR','DELETE',?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)",
                retentionVersion, retention, creator, creator, approver, approver);
        jdbc.update("UPDATE seal.retention_policy SET current_approved_version_id=? WHERE retention_policy_id=?", retentionVersion, retention);
        jdbc.update("INSERT INTO seal.processing_activity(processing_activity_id,activity_code,activity_name,owner_organization_unit_id,created_by,updated_by) VALUES(?,?,?,?,?,?)",
                activity, "IT-PA-" + suffix, "통합테스트 처리업무", org, creator, creator);
        jdbc.update("INSERT INTO seal.processing_activity_version(processing_activity_version_id,processing_activity_id,version_number,workflow_status,processing_purpose,legal_basis_type,legal_basis_detail,retention_policy_version_id,created_by,review_requested_by,review_requested_at,approved_by,approved_at,updated_by) VALUES(?,?,1,'APPROVED','통합테스트 교육 프로그램 운영 및 참여자 관리','CONSENT','정보주체의 명시적 동의',?,?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)",
                activityVersion, activity, retentionVersion, creator, creator, approver, approver);
        jdbc.update("UPDATE seal.processing_activity SET current_approved_version_id=? WHERE processing_activity_id=?", activityVersion, activity);
        jdbc.update("INSERT INTO seal.processing_data_item(processing_data_item_id,processing_activity_version_id,item_order,item_name,necessity_reason,required) VALUES(?,?,1,'이메일','동의 링크 발송을 위해 필요',TRUE)", dataItem, activityVersion);
        jdbc.update("INSERT INTO seal.consent_template(consent_template_id,template_code,template_name,processing_activity_id,created_by,updated_by) VALUES(?,?,?,?,?,?)",
                template, "IT-TPL-" + suffix, "통합테스트 동의서", activity, creator, creator);
        jdbc.update("INSERT INTO seal.consent_template_version(consent_template_version_id,consent_template_id,processing_activity_id,processing_activity_version_id,version_number,workflow_status,title,introduction_html,review_requested_by,review_requested_at,approved_by,approved_at,published_by,published_at,content_hash,created_by,updated_by) VALUES(?,?,?,?,1,'PUBLISHED','교육 프로그램 개인정보 동의서','<p>개인정보 처리 내용을 확인해 주세요.</p>',?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?,?,?)",
                templateVersion, template, activity, activityVersion, creator, approver, creator,
                "a".repeat(64), creator, creator);
        jdbc.update("UPDATE seal.consent_template SET current_published_version_id=? WHERE consent_template_id=?", templateVersion, template);
        jdbc.update("INSERT INTO seal.consent_item_definition(consent_item_definition_id,consent_template_version_id,processing_activity_version_id,item_order,item_code,item_type,item_title,required,purpose_text,retention_text,refusal_notice) VALUES(?,?,?,1,'COLLECT','COLLECTION_USE','개인정보 수집 이용 동의',TRUE,'교육 프로그램 운영 목적','동의 제출 후 3년','동의를 거부하면 신청이 제한될 수 있습니다.')",
                consentItem, templateVersion, activityVersion);
        jdbc.update("INSERT INTO seal.consent_item_data_selection(consent_item_definition_id,processing_activity_version_id,processing_data_item_id) VALUES(?,?,?)",
                consentItem, activityVersion, dataItem);
        return new Fixture(templateVersion, activity, retentionVersion);
    }

    private UUID fixtureCreator() {
        UUID creator = UUID.randomUUID();
        jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash,must_change_password) SELECT ?,?,?,organization_unit_id,password_hash,FALSE FROM seal.admin_user WHERE login_id='seal-admin'",
                creator, "fixture-" + creator.toString().substring(0, 8), "통합테스트 작성자");
        return creator;
    }

    private UUID id(String sql) { return jdbc.queryForObject(sql, UUID.class); }
    private record Fixture(UUID templateVersionId, UUID activityId, UUID retentionVersionId) { }
}
