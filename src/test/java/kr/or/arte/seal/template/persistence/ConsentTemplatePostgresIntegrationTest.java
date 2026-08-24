package kr.or.arte.seal.template.persistence;

import kr.or.arte.seal.policy.application.activity.ApprovedProcessingActivityQuery;
import kr.or.arte.seal.template.application.ConsentTemplateDetail;
import kr.or.arte.seal.template.application.ConsentTemplateManagement;
import kr.or.arte.seal.template.application.ConsentTemplateRepository;
import kr.or.arte.seal.template.domain.ConsentItemDefinition;
import kr.or.arte.seal.template.domain.ConsentItemType;
import kr.or.arte.seal.template.domain.ConsentTemplateVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SEAL_DB_PASSWORD", matches = ".+")
class ConsentTemplatePostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Autowired ConsentTemplateRepository repository;
    @Autowired ConsentTemplateManagement management;
    @Autowired ApprovedProcessingActivityQuery approvedActivityQuery;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void loadsOnlyCurrentApprovedConsentActivityWithDerivedRetentionAndItems() {
        Fixture fixture = fixture();

        var reference = approvedActivityQuery.findApprovedForTemplate(
                fixture.authorId(), fixture.activityVersionId());

        assertThat(reference.activityId()).isEqualTo(fixture.activityId());
        assertThat(reference.retentionDisplayText()).isEqualTo("동의 제출 후 3년");
        assertThat(reference.dataItems()).extracting(item -> item.itemName())
                .containsExactly("성명", "이메일");
    }

    @Test
    void savesReloadsAndReplacesDraftWithOrderedItems() {
        Fixture fixture = fixture();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ConsentItemDefinition second = item(2, "EMAIL", "이메일 수집 이용 동의",
                fixture.dataItemIds().get(1));
        ConsentItemDefinition first = item(1, "NAME", "성명 수집 이용 동의",
                fixture.dataItemIds().get(0));
        ConsentTemplateVersion draft = ConsentTemplateVersion.draft(
                versionId, templateId, fixture.activityId(), fixture.activityVersionId(),
                1, "교육 프로그램 개인정보 동의서",
                "<p>교육 프로그램 운영을 위한 개인정보 처리 안내입니다.</p>",
                List.of(second, first), fixture.authorId());
        ConsentTemplateDetail detail = new ConsentTemplateDetail(
                templateId, "TPL-" + compact(templateId), "교육 프로그램 동의서",
                fixture.activityId(), "교육 프로그램 운영", "ACTIVE", null, 0, draft);

        repository.insertTemplate(detail, fixture.authorId(), NOW);

        ConsentTemplateDetail loaded = repository.requireVersionForUpdate(
                fixture.authorId(), templateId, versionId);
        assertThat(loaded.version().items()).extracting(ConsentItemDefinition::order)
                .containsExactly(1, 2);

        ConsentTemplateVersion changed = loaded.version().changeContent(
                "수정된 교육 프로그램 개인정보 동의서",
                "<p>수정된 교육 프로그램 개인정보 처리 안내입니다.</p>",
                List.of(item(1, "EMAIL", "이메일 수집 이용 동의", fixture.dataItemIds().get(1))));
        repository.replaceDraft(changed, fixture.authorId(), NOW.plusSeconds(60));

        ConsentTemplateDetail reloaded = repository.requireVersionForUpdate(
                fixture.authorId(), templateId, versionId);
        assertThat(reloaded.version().title()).startsWith("수정된");
        assertThat(reloaded.version().items()).singleElement()
                .satisfies(item -> assertThat(item.code()).isEqualTo("EMAIL"));
    }

    @Test
    void draftItemReplacementFunctionRejectsOutOfScopeActor() {
        Fixture fixture = fixture();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ConsentTemplateVersion draft = ConsentTemplateVersion.draft(
                versionId, templateId, fixture.activityId(), fixture.activityVersionId(),
                1, "교육 프로그램 개인정보 동의서",
                "<p>교육 프로그램 운영을 위한 개인정보 처리 안내입니다.</p>",
                List.of(item(1, "NAME", "성명 수집 이용 동의", fixture.dataItemIds().get(0))),
                fixture.authorId());
        repository.insertTemplate(new ConsentTemplateDetail(
                templateId, "TPL-" + compact(templateId), "교육 프로그램 동의서",
                fixture.activityId(), "교육 프로그램 운영", "ACTIVE", null, 0, draft),
                fixture.authorId(), NOW);

        UUID outsiderId = UUID.randomUUID();
        insertActor(outsiderId, "outsider", UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThatThrownBy(() -> jdbcTemplate.queryForObject(
                "SELECT seal.clear_draft_consent_items(?, ?)", Integer.class,
                versionId, outsiderId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void reviewsApprovesAndPublishesAgainstPostgresql() {
        Fixture fixture = fixture();
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ConsentTemplateVersion draft = ConsentTemplateVersion.draft(
                versionId, templateId, fixture.activityId(), fixture.activityVersionId(),
                1, "교육 프로그램 개인정보 동의서",
                "<p>교육 프로그램 운영을 위한 개인정보 처리 안내입니다.</p>",
                List.of(item(1, "NAME", "성명 수집 이용 동의", fixture.dataItemIds().get(0))),
                fixture.authorId());
        repository.insertTemplate(new ConsentTemplateDetail(
                templateId, "TPL-" + compact(templateId), "교육 프로그램 동의서",
                fixture.activityId(), "교육 프로그램 운영", "ACTIVE", null, 0, draft),
                fixture.authorId(), NOW);

        management.requestReview(templateId, versionId, fixture.authorId());
        management.approve(templateId, versionId, fixture.reviewerId());
        management.publish(templateId, versionId, fixture.reviewerId());

        assertThat(jdbcTemplate.queryForObject("""
                SELECT workflow_status = 'PUBLISHED' AND content_hash ~ '^[0-9a-f]{64}$'
                  FROM seal.consent_template_version
                 WHERE consent_template_version_id = ?
                """, Boolean.class, versionId)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT current_published_version_id = ?
                  FROM seal.consent_template WHERE consent_template_id = ?
                """, Boolean.class, versionId, templateId)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM seal.template_review
                 WHERE consent_template_version_id = ?
                """, Integer.class, versionId)).isEqualTo(3);
    }

    private Fixture fixture() {
        UUID authorId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        insertActor(authorId, "author", organizationId);
        insertActor(reviewerId, "reviewer", organizationId);
        jdbcTemplate.update("""
                INSERT INTO seal.user_scope (user_scope_id, admin_user_id, scope_type,
                    scope_target_id, include_descendants, assigned_by, active)
                VALUES (?, ?, 'ORGANIZATION', ?, TRUE, ?, TRUE)
                """, UUID.randomUUID(), authorId, organizationId, authorId);
        jdbcTemplate.update("""
                INSERT INTO seal.user_scope (user_scope_id, admin_user_id, scope_type,
                    scope_target_id, include_descendants, assigned_by, active)
                VALUES (?, ?, 'ORGANIZATION', ?, TRUE, ?, TRUE)
                """, UUID.randomUUID(), reviewerId, organizationId, authorId);

        UUID retentionPolicyId = UUID.randomUUID();
        UUID retentionVersionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy (retention_policy_id, policy_code, policy_name,
                    owner_organization_unit_id, policy_status, created_by, updated_by)
                VALUES (?, ?, '통합테스트 보유정책', ?, 'ACTIVE', ?, ?)
                """, retentionPolicyId, "RP-" + compact(retentionPolicyId), organizationId,
                authorId, authorId);
        jdbcTemplate.update("""
                INSERT INTO seal.retention_policy_version (retention_policy_version_id,
                    retention_policy_id, version_number, workflow_status, start_event,
                    period_value, period_unit, grace_period_days, destruction_method,
                    created_by, review_requested_by, review_requested_at,
                    approved_by, approved_at, updated_by)
                VALUES (?, ?, 1, 'APPROVED', 'CONSENT_SUBMITTED_AT', 3, 'YEAR', 0, 'DELETE',
                    ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                """, retentionVersionId, retentionPolicyId, authorId, authorId,
                reviewerId, reviewerId);
        jdbcTemplate.update("""
                UPDATE seal.retention_policy SET current_approved_version_id = ?
                 WHERE retention_policy_id = ?
                """, retentionVersionId, retentionPolicyId);

        UUID activityId = UUID.randomUUID();
        UUID activityVersionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.processing_activity (processing_activity_id, activity_code,
                    activity_name, owner_organization_unit_id, activity_status,
                    created_by, updated_by)
                VALUES (?, ?, '교육 프로그램 운영', ?, 'ACTIVE', ?, ?)
                """, activityId, "PA-" + compact(activityId), organizationId, authorId, authorId);
        jdbcTemplate.update("""
                INSERT INTO seal.processing_activity_version (processing_activity_version_id,
                    processing_activity_id, version_number, workflow_status, processing_purpose,
                    legal_basis_type, legal_basis_detail, retention_policy_version_id,
                    created_by, review_requested_by, review_requested_at,
                    approved_by, approved_at, updated_by)
                VALUES (?, ?, 1, 'APPROVED', '교육 프로그램 참여자 등록 및 운영 목적',
                    'CONSENT', '정보주체의 개인정보 수집 이용 동의', ?,
                    ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                """, activityVersionId, activityId, retentionVersionId, authorId, authorId,
                reviewerId, reviewerId);
        jdbcTemplate.update("""
                UPDATE seal.processing_activity SET current_approved_version_id = ?
                 WHERE processing_activity_id = ?
                """, activityVersionId, activityId);

        UUID nameItemId = UUID.randomUUID();
        UUID emailItemId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO seal.processing_data_item (processing_data_item_id,
                    processing_activity_version_id, item_order, item_name, necessity_reason,
                    required, data_category)
                VALUES (?, ?, 1, '성명', '참여자 식별', TRUE, 'GENERAL'),
                       (?, ?, 2, '이메일', '교육 일정 안내', TRUE, 'GENERAL')
                """, nameItemId, activityVersionId, emailItemId, activityVersionId);
        return new Fixture(authorId, reviewerId, activityId, activityVersionId,
                List.of(nameItemId, emailItemId));
    }

    private void insertActor(UUID id, String prefix, UUID organizationId) {
        jdbcTemplate.update("""
                INSERT INTO seal.admin_user (admin_user_id, login_id, display_name,
                    organization_unit_id, password_hash, must_change_password)
                VALUES (?, ?, '통합테스트 관리자', ?, repeat('x', 60), FALSE)
                """, id, prefix + "-" + id.toString().substring(0, 8), organizationId);
    }

    private ConsentItemDefinition item(int order, String code, String title, UUID dataItemId) {
        return new ConsentItemDefinition(UUID.randomUUID(), order, code,
                ConsentItemType.COLLECTION_USE, title, true,
                "교육 프로그램 참여자 등록 및 운영 목적", "동의 제출 후 3년",
                "동의를 거부할 수 있으나 교육 프로그램 신청이 제한될 수 있습니다.",
                null, List.of(dataItemId));
    }

    private static String compact(UUID id) {
        return id.toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private record Fixture(UUID authorId, UUID reviewerId, UUID activityId, UUID activityVersionId,
                           List<UUID> dataItemIds) {
    }
}
