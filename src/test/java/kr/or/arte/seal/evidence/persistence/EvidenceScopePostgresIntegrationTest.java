package kr.or.arte.seal.evidence.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SEAL_REVIEW_DB_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:15439/seal_dev.*")
class EvidenceScopePostgresIntegrationTest {
    @Test void scopesRealQueriesBeforePaginationAndRejectsDestroyedClaims() throws Exception {
        var ds = new DriverManagerDataSource(System.getenv("SEAL_REVIEW_DB_URL"), "review_admin", "");
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setTypeHandlersPackage("kr.or.arte.seal.common.persistence");
        factory.setMapperLocations(new ClassPathResource("mybatis/mappers/evidence/EvidenceMapper.xml"),
                new ClassPathResource("mybatis/mappers/retention/RetentionMapper.xml"),
                new ClassPathResource("mybatis/mappers/consent/PublicConsentMapper.xml"),
                new ClassPathResource("mybatis/mappers/recipient/RecipientMapper.xml"),
                new ClassPathResource("mybatis/mappers/template/ConsentTemplateQueryMapper.xml"),
                new ClassPathResource("mybatis/mappers/rights/RightsRequestMapper.xml"));
        var sqlSession = new SqlSessionTemplate(factory.getObject());
        var mapper = sqlSession.getMapper(EvidenceMapper.class);
        var retention = sqlSession.getMapper(kr.or.arte.seal.retention.persistence.RetentionMapper.class);
        var consent = sqlSession.getMapper(kr.or.arte.seal.consent.persistence.PublicConsentMapper.class);
        var jdbc = new JdbcTemplate(ds);
        new TransactionTemplate(new DataSourceTransactionManager(ds)).executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            UUID org = jdbc.queryForObject("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit", UUID.class);
            UUID actor = UUID.randomUUID();
            jdbc.update("INSERT INTO seal.admin_user(admin_user_id,login_id,display_name,organization_unit_id,password_hash) VALUES(?,?,'Review fixture',?,?)",
                    actor, "review-"+actor.toString().substring(0,8), org, "$2a$10$"+"a".repeat(53));
            jdbc.update("INSERT INTO seal.user_scope(user_scope_id,admin_user_id,scope_type,scope_target_id,include_descendants,assigned_by) VALUES(?,?,'ORGANIZATION',?,FALSE,?)",
                    UUID.randomUUID(), actor, org, actor);
            var allowed = fixture(jdbc, org, actor);
            var denied = fixture(jdbc, org, actor);
            jdbc.update("INSERT INTO seal.user_processing_activity_scope(admin_user_id,processing_activity_id,assigned_by) VALUES(?,?,?)", actor, allowed.activity, actor);
            assertThat(mapper.search(actor,null,1,0)).extracting(row -> row.packageId()).containsExactly(allowed.evidence);
            var recipients = sqlSession.getMapper(kr.or.arte.seal.recipient.persistence.RecipientMapper.class);
            assertThat(recipients.findManyScoped(actor,java.util.List.of(allowed.recipient,denied.recipient))).hasSize(2);
            assertThat(recipients.findManyScoped(UUID.randomUUID(),java.util.List.of(allowed.recipient))).isEmpty();
            var templates = sqlSession.getMapper(kr.or.arte.seal.template.persistence.ConsentTemplateQueryMapper.class);
            assertThat(templates.findVersionOptions(actor,java.util.List.of(allowed.version,denied.version)))
                    .extracting(row -> row.templateVersionId()).containsExactly(allowed.version);
            var filter = kr.or.arte.seal.evidence.application.EvidenceSearchFilter.of("review",null,null);
            assertThat(mapper.search(actor,null,filter,20,0)).hasSize(1);
            assertThat(mapper.search(actor,null,kr.or.arte.seal.evidence.application.EvidenceSearchFilter.of("%",null,null),20,0)).isEmpty();
            var day = java.time.LocalDate.of(2026,9,29);
            var dates = kr.or.arte.seal.evidence.application.EvidenceSearchFilter.of(null,day,day);
            jdbc.update("UPDATE seal.evidence_package SET queued_at=? WHERE evidence_package_id=?",java.sql.Timestamp.from(dates.untilExclusive().minusSeconds(1)),allowed.evidence);
            assertThat(mapper.search(actor,null,dates,20,0)).hasSize(1);
            jdbc.update("UPDATE seal.evidence_package SET queued_at=? WHERE evidence_package_id=?",java.sql.Timestamp.from(dates.untilExclusive()),allowed.evidence);
            sqlSession.clearCache();
            assertThat(mapper.search(actor,null,dates,20,0)).isEmpty();
            var rights = sqlSession.getMapper(kr.or.arte.seal.rights.persistence.RightsRequestMapper.class);
            for (var target : java.util.List.of(allowed,denied)) {
                jdbc.update("INSERT INTO seal.rights_request(rights_request_id,request_code,organization_unit_id,recipient_id,consent_request_id,rights_type,received_date,due_date,detail_ciphertext,encryption_key_id,created_by,created_at) VALUES(?,?,?,?,?,'ACCESS',CURRENT_DATE,CURRENT_DATE+30,?,'review',?,CURRENT_TIMESTAMP)",UUID.randomUUID(),"RR-"+target.request.toString().substring(0,8),org,target.recipient,target.request,new byte[32],actor);
            }
            var rightsRows=rights.search(actor,null,20,0);
            assertThat(rightsRows).hasSize(2);
            assertThat(rightsRows.stream().filter(row -> row.recipientId().equals(allowed.recipient)).findFirst().orElseThrow().templateTitle()).isEqualTo("Review consent");
            assertThat(rightsRows.stream().filter(row -> row.recipientId().equals(denied.recipient)).findFirst().orElseThrow().templateTitle()).isNull();
            assertThat(mapper.findScoped(actor,allowed.evidence)).isPresent();
            assertThat(mapper.findScoped(actor,denied.evidence)).isEmpty();
            assertThat(mapper.findByRequestScoped(actor,denied.request)).isEmpty();
            assertThat(mapper.requeue(actor,denied.evidence,Instant.now())).isZero();
            assertThat(mapper.requeue(actor,allowed.evidence,Instant.now())).isEqualTo(1);
            jdbc.update("UPDATE seal.evidence_package SET generation_status='PROCESSING' WHERE evidence_package_id=?", allowed.evidence);
            assertThat(mapper.lockPublishableClaim(allowed.evidence,allowed.request,1)).isPresent();
            assertThat(retention.invalidatePendingEvidence(allowed.request,Instant.now())).isEqualTo(1);
            assertThat(mapper.lockPublishableClaim(allowed.evidence,allowed.request,1)).isEmpty();
            assertThat(retention.placeLegalHold(UUID.randomUUID(),denied.request,"REVIEW","Review scope test",null,actor,Instant.now())).isZero();
            assertThat(retention.restrictionHold(UUID.randomUUID(),denied.request,actor,Instant.now())).isZero();
            UUID hold=UUID.randomUUID();
            jdbc.update("INSERT INTO seal.legal_hold(legal_hold_id,consent_request_id,hold_reason_code,reason_text,held_by,held_at) VALUES(?,?,'REVIEW','Review scope test',?,CURRENT_TIMESTAMP)",hold,denied.request,actor);
            assertThat(retention.releaseLegalHold(hold,actor,Instant.now())).isZero();
            Instant deadline=Instant.now().plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            jdbc.update("UPDATE seal.consent_request SET expires_at=? WHERE consent_request_id=?",java.sql.Timestamp.from(deadline),allowed.request);
            String tokenHash=allowed.request.toString().replace("-", "").repeat(2);
            assertThat(consent.findSession(tokenHash).orElseThrow().expiresAt()).isEqualTo(deadline);
            assertThat(consent.findSessionForUpdate(tokenHash).orElseThrow().expiresAt()).isEqualTo(deadline);
            assertThat(consent.findLink(tokenHash).orElseThrow().expiresAt()).isEqualTo(deadline);
        });
    }

    private record Fixture(UUID activity, UUID request, UUID evidence, UUID recipient, UUID version) {}
    private Fixture fixture(JdbcTemplate jdbc, UUID org, UUID actor) {
        UUID policy=UUID.randomUUID(), policyVersion=UUID.randomUUID(), activity=UUID.randomUUID(), activityVersion=UUID.randomUUID();
        UUID template=UUID.randomUUID(), templateVersion=UUID.randomUUID(), recipient=UUID.randomUUID(), request=UUID.randomUUID();
        UUID snapshot=UUID.randomUUID(), link=UUID.randomUUID(), session=UUID.randomUUID(), response=UUID.randomUUID(), evidence=UUID.randomUUID();
        String code=request.toString().substring(0,8).toUpperCase();
        String hash=request.toString().replace("-", "").repeat(2);
        jdbc.update("INSERT INTO seal.retention_policy(retention_policy_id,policy_code,policy_name,owner_organization_unit_id,created_by,updated_by) VALUES(?,?,'Review policy',?,?,?)", policy,"RP-"+code,org,actor,actor);
        jdbc.update("INSERT INTO seal.retention_policy_version(retention_policy_version_id,retention_policy_id,version_number,start_event,period_value,period_unit,destruction_method,created_by,updated_by) VALUES(?,?,1,'CONSENT_SUBMITTED_AT',1,'YEAR','DELETE',?,?)",policyVersion,policy,actor,actor);
        jdbc.update("INSERT INTO seal.processing_activity(processing_activity_id,activity_code,activity_name,owner_organization_unit_id,created_by,updated_by) VALUES(?,?,'Review activity',?,?,?)",activity,"PA-"+code,org,actor,actor);
        jdbc.update("INSERT INTO seal.processing_activity_version(processing_activity_version_id,processing_activity_id,version_number,processing_purpose,legal_basis_type,legal_basis_detail,retention_policy_version_id,created_by,updated_by) VALUES(?,?,1,'Review purpose','CONSENT','Review consent',?,?,?)",activityVersion,activity,policyVersion,actor,actor);
        jdbc.update("INSERT INTO seal.consent_template(consent_template_id,template_code,template_name,processing_activity_id,created_by,updated_by) VALUES(?,?,'Review template',?,?,?)",template,"TPL-"+code,activity,actor,actor);
        jdbc.update("INSERT INTO seal.consent_template_version(consent_template_version_id,consent_template_id,processing_activity_id,processing_activity_version_id,version_number,title,introduction_html,created_by,updated_by) VALUES(?,?,?,?,1,'Review consent','<p>Review</p>',?,?)",templateVersion,template,activity,activityVersion,actor,actor);
        jdbc.update("INSERT INTO seal.recipient(recipient_id,organization_unit_id,recipient_name_ciphertext,recipient_email_ciphertext,encryption_key_id,email_lookup_hmac,collection_basis,created_by,updated_by) VALUES(?,?,?,?,'review',?,'Synthetic review fixture',?,?)",recipient,org,new byte[32],new byte[32],hash,actor,actor);
        jdbc.update("INSERT INTO seal.consent_request(consent_request_id,request_code,organization_unit_id,recipient_id,processing_activity_id,processing_activity_version_id,consent_template_id,consent_template_version_id,retention_policy_version_id,request_status,expires_at,idempotency_key,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,'EXPIRED',CURRENT_TIMESTAMP+INTERVAL '1 day',?,?,?)",request,"CR-20260929-"+code,org,recipient,activity,activityVersion,template,templateVersion,policyVersion,UUID.randomUUID(),actor,actor);
        jdbc.update("INSERT INTO seal.consent_request_snapshot(consent_request_snapshot_id,consent_request_id,snapshot_json,snapshot_hash,template_content_hash,created_by) VALUES(?,?,CAST(? AS jsonb),?,?,?)",snapshot,request,"{\"title\":\"Review consent\"}",hash,hash,actor);
        jdbc.update("UPDATE seal.consent_request SET snapshot_id=? WHERE consent_request_id=?",snapshot,request);
        jdbc.update("INSERT INTO seal.secure_link(secure_link_id,consent_request_id,token_hmac,expires_at,issued_by) VALUES(?,?,?,CURRENT_TIMESTAMP+INTERVAL '1 day',?)",link,request,hash,actor);
        jdbc.update("INSERT INTO seal.public_consent_session(public_consent_session_id,flow_hmac,consent_request_id,secure_link_id,expires_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP+INTERVAL '1 day')",session,hash,request,link);
        jdbc.update("INSERT INTO seal.consent_response(consent_response_id,consent_request_id,public_consent_session_id,response_status,idempotency_key,snapshot_hash,submitted_at) VALUES(?,?,?,'SUBMITTED',?,?,CURRENT_TIMESTAMP)",response,request,session,UUID.randomUUID(),hash);
        jdbc.update("INSERT INTO seal.evidence_package(evidence_package_id,consent_response_id,consent_request_id,organization_unit_id,generation_status,queued_at) VALUES(?,?,?,?,'FAILED',CURRENT_TIMESTAMP)",evidence,response,request,org);
        return new Fixture(activity,request,evidence,recipient,templateVersion);
    }
}
