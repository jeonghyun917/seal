package kr.or.arte.seal.request.persistence;

import kr.or.arte.seal.recipient.application.CreateRecipientCommand;
import kr.or.arte.seal.recipient.application.RecipientManagementService;
import kr.or.arte.seal.request.application.ConsentRequestManagementService;
import kr.or.arte.seal.request.application.CreateConsentRequestCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class ConsentRequestPostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RecipientManagementService recipients;
    @Autowired ConsentRequestManagementService requests;

    @Test
    void createsAndFinalizesRequestWithImmutableSnapshotRows() {
        UUID actor = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='vheraclesv'");
        UUID creator = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
        UUID org = id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
        Fixture fixture = publishedFixture(org, creator, actor);
        UUID recipient = recipients.create(new CreateRecipientCommand(actor, org, "요청 대상자",
                "request-" + UUID.randomUUID() + "@example.com", "교육 프로그램 신청서 직접 수집"));

        var created = requests.create(actor, new CreateConsentRequestCommand(org, recipient,
                fixture.templateVersionId(), Instant.now().plusSeconds(7200), UUID.randomUUID()));
        requests.finalizeRequest(actor, created.requestId(), 0);

        assertThat(jdbc.queryForObject("SELECT request_status FROM seal.consent_request WHERE consent_request_id=?",
                String.class, created.requestId())).isEqualTo("READY");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.consent_request_snapshot WHERE consent_request_id=?",
                Integer.class, created.requestId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM seal.consent_request_item_data data JOIN seal.consent_request_item item ON item.consent_request_item_id=data.consent_request_item_id JOIN seal.consent_request_snapshot snapshot ON snapshot.consent_request_snapshot_id=item.consent_request_snapshot_id WHERE snapshot.consent_request_id=?",
                Integer.class, created.requestId())).isEqualTo(1);
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
        return new Fixture(templateVersion);
    }

    private UUID id(String sql) { return jdbc.queryForObject(sql, UUID.class); }
    private record Fixture(UUID templateVersionId) { }
}
