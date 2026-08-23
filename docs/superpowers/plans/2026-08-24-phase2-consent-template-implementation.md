# Phase 2 Consent Template Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 승인된 개인정보 처리업무 버전으로 구조화 동의서 템플릿을 작성하고 검토·승인·발행하여 이메일과 모바일 웹이 재사용할 불변 원본을 만든다.

**Architecture:** `template` 모듈에 domain/application/persistence/web 경계를 만들고 `policy` 모듈의 공개 조회 인터페이스로 승인 처리업무만 참조한다. 템플릿 본문은 구조화 테이블로 저장하고 제한 HTML은 저장 전에 정제하며, 발행 트랜잭션에서 정책을 재검증하고 canonical JSON SHA-256을 확정한다.

**Tech Stack:** Java 17, eGovFrame 5.0.0, Spring Boot 3.5.6, Spring MVC/Security, JSP/JSTL, MyBatis, PostgreSQL, OWASP Java HTML Sanitizer, Jackson, JUnit 5, AssertJ

**Spec:** `docs/superpowers/specs/2026-08-24-phase2-consent-template-design.md`

## Global Constraints

- 전자서명, SMS·알림톡, OTP, 본인확인, 민감정보·고유식별정보, 만 14세 미만과 메일 열람 추적은 구현하지 않는다.
- Java 17, JSP/JSTL, 네이티브 JavaScript와 PostgreSQL을 사용하고 jQuery, Redis, Kafka, Flyway와 Liquibase를 추가하지 않는다.
- 적용된 SQL은 수정하지 않고 `database/03_patch`에 순번 패치를 추가하며 대응 검증 SQL을 함께 만든다.
- 관리자 변경 POST는 CSRF와 POST-Redirect-GET을 사용한다.
- 권한이 없으면 403, 권한은 있으나 처리업무 범위 밖이면 404를 반환한다.
- 발행 버전은 수정·삭제하지 않고 새 버전으로만 변경한다.
- 토큰, 비밀번호와 동의 원문을 로그나 감사 메타데이터에 기록하지 않는다.
- 새 도메인·보안 동작은 실패 테스트를 먼저 실행한 후 최소 구현을 작성한다.

---

### Task 1: 템플릿 DB 스키마와 권한

**Files:**
- Create: `database/03_patch/20260824_001_create_consent_template_tables.sql`
- Create: `database/04_verify/005_consent_template_verify.sql`
- Create: `database/03_patch/20260824_002_add_template_draft_replace_function.sql`
- Create: `database/04_verify/006_template_draft_replace_verify.sql`
- Modify: `database/03_patch/README.md`
- Test: `src/test/java/kr/or/arte/seal/template/persistence/ConsentTemplateSchemaPostgresIntegrationTest.java`

**Interfaces:**
- Consumes: 기존 `processing_activity`, `processing_activity_version`, `processing_data_item`, `admin_user`, `permission`, `admin_role`, `role_permission`, `db_change_history`
- Produces: `consent_template`, `consent_template_version`, `consent_item_definition`, `consent_item_data_selection`, `template_review`와 `CONSENT_TEMPLATE_*` 권한 5개

- [ ] **Step 1: 실제 PostgreSQL 제약을 요구하는 실패 통합 테스트 작성**

```java
@Test
void rejectsTemplateVersionFromAnotherProcessingActivity() {
    assertThatThrownBy(() -> jdbc.update("""
        INSERT INTO seal.consent_template_version
          (consent_template_version_id, consent_template_id, processing_activity_id,
           processing_activity_version_id, version_number, workflow_status, title,
           introduction_html, created_by, updated_by)
        VALUES (?, ?, ?, ?, 1, 'DRAFT', '개인정보 수집 이용 동의',
                '<p>개인정보 처리 내용을 확인해 주세요.</p>', ?, ?)
        """, versionId, templateId, otherActivityId, approvedVersionId, actorId, actorId))
        .isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void sealAppCannotDeleteTemplateOrUpdateReviewHistory() {
    assertThat(hasTablePrivilege("seal_app", "consent_template", "DELETE")).isFalse();
    assertThat(hasTablePrivilege("seal_app", "template_review", "UPDATE")).isFalse();
}
```

- [ ] **Step 2: 통합 테스트 RED 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateSchemaPostgresIntegrationTest test`  
Expected: FAIL because the five template tables do not exist.

- [ ] **Step 3: 수동 DB 패치 작성**

패치에는 다음 제약을 정확히 포함한다.

초안 항목 교체는 `seal_app`에 DELETE 권한을 부여하지 않고 별도 보완 패치의 `clear_draft_consent_items(version_id, actor_id)` 함수로 수행한다. 함수는 `SECURITY DEFINER`와 고정 `search_path`를 사용하고 대상 버전의 `DRAFT` 상태 및 작업자의 활성 조직 범위를 검사한다.

```sql
CREATE TABLE consent_template (
    consent_template_id UUID PRIMARY KEY,
    template_code VARCHAR(30) NOT NULL UNIQUE,
    template_name VARCHAR(100) NOT NULL,
    processing_activity_id UUID NOT NULL REFERENCES processing_activity(processing_activity_id),
    template_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_published_version_id UUID,
    created_by UUID NOT NULL REFERENCES admin_user(admin_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID NOT NULL REFERENCES admin_user(admin_user_id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_template_activity_pair UNIQUE (consent_template_id, processing_activity_id),
    CONSTRAINT ck_template_code CHECK (template_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_template_status CHECK (template_status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT ck_template_version_no CHECK (version_no >= 0)
);
```

교차 업무 FK를 위해 다음 스키마를 그대로 구현한다.

```sql
ALTER TABLE processing_activity_version
  ADD CONSTRAINT uq_processing_version_activity_pair
  UNIQUE (processing_activity_version_id, processing_activity_id);
ALTER TABLE processing_data_item
  ADD CONSTRAINT uq_processing_data_item_version_pair
  UNIQUE (processing_data_item_id, processing_activity_version_id);

CREATE TABLE consent_template_version (
    consent_template_version_id UUID PRIMARY KEY,
    consent_template_id UUID NOT NULL,
    processing_activity_id UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    workflow_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(200) NOT NULL,
    introduction_html VARCHAR(8000) NOT NULL,
    review_requested_by UUID REFERENCES admin_user(admin_user_id),
    review_requested_at TIMESTAMPTZ,
    approved_by UUID REFERENCES admin_user(admin_user_id),
    approved_at TIMESTAMPTZ,
    published_by UUID REFERENCES admin_user(admin_user_id),
    published_at TIMESTAMPTZ,
    rejection_comment VARCHAR(1000),
    content_hash CHAR(64),
    created_by UUID NOT NULL REFERENCES admin_user(admin_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID NOT NULL REFERENCES admin_user(admin_user_id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_template_version_number UNIQUE (consent_template_id, version_number),
    CONSTRAINT uq_template_version_activity_pair UNIQUE
      (consent_template_version_id, processing_activity_version_id),
    CONSTRAINT fk_template_version_template_activity FOREIGN KEY
      (consent_template_id, processing_activity_id)
      REFERENCES consent_template(consent_template_id, processing_activity_id),
    CONSTRAINT fk_template_version_processing_activity FOREIGN KEY
      (processing_activity_version_id, processing_activity_id)
      REFERENCES processing_activity_version(processing_activity_version_id, processing_activity_id),
    CONSTRAINT ck_template_workflow CHECK
      (workflow_status IN ('DRAFT','REVIEW_REQUESTED','APPROVED','PUBLISHED','RETIRED')),
    CONSTRAINT ck_template_title CHECK (length(btrim(title)) BETWEEN 5 AND 200),
    CONSTRAINT ck_template_intro CHECK (length(introduction_html) BETWEEN 10 AND 8000),
    CONSTRAINT ck_template_review_pair CHECK
      ((review_requested_by IS NULL) = (review_requested_at IS NULL)),
    CONSTRAINT ck_template_approval_pair CHECK
      ((approved_by IS NULL) = (approved_at IS NULL)),
    CONSTRAINT ck_template_publish_pair CHECK
      ((published_by IS NULL) = (published_at IS NULL)),
    CONSTRAINT ck_template_author_approver CHECK
      (approved_by IS NULL OR approved_by <> created_by),
    CONSTRAINT ck_template_hash CHECK
      (content_hash IS NULL OR content_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_template_published_hash CHECK
      (workflow_status NOT IN ('PUBLISHED','RETIRED') OR content_hash IS NOT NULL),
    CONSTRAINT ck_template_version_lock CHECK (version_no >= 0)
);

CREATE TABLE consent_item_definition (
    consent_item_definition_id UUID PRIMARY KEY,
    consent_template_version_id UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    item_order INTEGER NOT NULL,
    item_code VARCHAR(30) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    item_title VARCHAR(200) NOT NULL,
    required BOOLEAN NOT NULL,
    purpose_text VARCHAR(1000) NOT NULL,
    retention_text VARCHAR(1000) NOT NULL,
    refusal_notice VARCHAR(1000) NOT NULL,
    third_party_recipient VARCHAR(500),
    CONSTRAINT uq_template_item_order UNIQUE (consent_template_version_id, item_order),
    CONSTRAINT uq_template_item_code UNIQUE (consent_template_version_id, item_code),
    CONSTRAINT uq_template_item_version_pair UNIQUE
      (consent_item_definition_id, processing_activity_version_id),
    CONSTRAINT fk_template_item_version FOREIGN KEY
      (consent_template_version_id, processing_activity_version_id)
      REFERENCES consent_template_version
        (consent_template_version_id, processing_activity_version_id),
    CONSTRAINT ck_template_item_order CHECK (item_order >= 1),
    CONSTRAINT ck_template_item_code CHECK (item_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_template_item_type CHECK
      (item_type IN ('COLLECTION_USE','THIRD_PARTY_PROVISION','OPTIONAL')),
    CONSTRAINT ck_template_optional_required CHECK (item_type <> 'OPTIONAL' OR required = FALSE),
    CONSTRAINT ck_template_third_party CHECK
      ((item_type = 'THIRD_PARTY_PROVISION') =
       (third_party_recipient IS NOT NULL AND length(btrim(third_party_recipient)) > 0))
);

CREATE TABLE consent_item_data_selection (
    consent_item_definition_id UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    processing_data_item_id UUID NOT NULL,
    PRIMARY KEY (consent_item_definition_id, processing_data_item_id),
    CONSTRAINT fk_selection_item_version FOREIGN KEY
      (consent_item_definition_id, processing_activity_version_id)
      REFERENCES consent_item_definition
        (consent_item_definition_id, processing_activity_version_id),
    CONSTRAINT fk_selection_data_version FOREIGN KEY
      (processing_data_item_id, processing_activity_version_id)
      REFERENCES processing_data_item
        (processing_data_item_id, processing_activity_version_id)
);

CREATE TABLE template_review (
    template_review_id UUID PRIMARY KEY,
    consent_template_version_id UUID NOT NULL
      REFERENCES consent_template_version(consent_template_version_id),
    review_action VARCHAR(20) NOT NULL,
    actor_id UUID NOT NULL REFERENCES admin_user(admin_user_id),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_template_review_action CHECK
      (review_action IN ('REQUESTED','APPROVED','REJECTED','PUBLISHED','RETIRED'))
);

ALTER TABLE consent_template
  ADD CONSTRAINT fk_template_current_published_version
  FOREIGN KEY (current_published_version_id)
  REFERENCES consent_template_version(consent_template_version_id);
```

새 권한 UUID는 기존 `00000000-0000-0000-0002-*` 대역에서 18~22를 사용해 `CONSENT_TEMPLATE_VIEW`, `CONSENT_TEMPLATE_EDIT`, `CONSENT_TEMPLATE_REVIEW`, `CONSENT_TEMPLATE_APPROVE`, `CONSENT_TEMPLATE_PUBLISH`를 INSERT하고 `SYSTEM_ADMIN`에만 기본 배정한다. 본문 네 테이블에는 `seal_app` SELECT·INSERT·UPDATE, `template_review`에는 SELECT·INSERT만 부여하며 모든 테이블의 DELETE·TRUNCATE와 스키마 DDL은 거부한다.

- [ ] **Step 4: 검증 SQL 작성**

`005_consent_template_verify.sql`은 테이블·CHECK·FK·UNIQUE 존재, 권한 5개, `seal_app`의 DML 최소 권한, `template_review` UPDATE/DELETE 거부와 `db_change_history` 체크섬을 `DO $$ ... RAISE EXCEPTION` 방식으로 검증한다.

- [ ] **Step 5: `seal_test`에 패치 적용 후 GREEN 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateSchemaPostgresIntegrationTest test`  
Expected: PASS, including cross-activity FK and least-privilege assertions.

- [ ] **Step 6: Task 1 커밋**

```powershell
git add database/03_patch/20260824_001_create_consent_template_tables.sql database/04_verify/005_consent_template_verify.sql database/03_patch/README.md src/test/java/kr/or/arte/seal/template/persistence/ConsentTemplateSchemaPostgresIntegrationTest.java
git commit -m "feat: 동의서 템플릿 데이터 모델 추가"
```

### Task 2: 도메인 상태·항목 검증과 HTML 정제

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/kr/or/arte/seal/template/domain/TemplateWorkflowStatus.java`
- Create: `src/main/java/kr/or/arte/seal/template/domain/ConsentItemType.java`
- Create: `src/main/java/kr/or/arte/seal/template/domain/ConsentItemDefinition.java`
- Create: `src/main/java/kr/or/arte/seal/template/domain/ConsentTemplateVersion.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplateHtmlSanitizer.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplateContentHasher.java`
- Create: `src/main/java/kr/or/arte/seal/template/config/TemplateContentConfiguration.java`
- Test: `src/test/java/kr/or/arte/seal/template/domain/ConsentTemplateVersionTest.java`
- Test: `src/test/java/kr/or/arte/seal/template/application/TemplateHtmlSanitizerTest.java`
- Test: `src/test/java/kr/or/arte/seal/template/application/TemplateContentHasherTest.java`

**Interfaces:**
- Produces: `ConsentTemplateVersion.requestReview`, `approve`, `reject`, `publish`, `retire`; `TemplateHtmlSanitizer.sanitize`; `TemplateContentHasher.sha256`
- Consumes: 처리업무 항목 UUID·명칭과 서버 파생 보유기간 문구

- [ ] **Step 1: 상태와 항목 규칙 실패 테스트 작성**

```java
@Test
void authorCannotApproveOwnTemplateVersion() {
    var requested = validDraft(AUTHOR).requestReview(AUTHOR);
    assertThatThrownBy(() -> requested.approve(AUTHOR))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("작성자와 승인자는 달라야 합니다.");
}

@Test
void optionalItemCannotBeRequired() {
    assertThatThrownBy(() -> item(ConsentItemType.OPTIONAL, true, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("선택 동의 항목은 필수로 설정할 수 없습니다.");
}

@Test
void publishedVersionCannotBeEdited() {
    var published = validDraft(AUTHOR).requestReview(AUTHOR).approve(APPROVER)
        .publish(PUBLISHER, HASH);
    assertThatThrownBy(() -> published.changeContent("새 제목", HTML, ITEMS))
        .isInstanceOf(IllegalStateException.class);
}
```

- [ ] **Step 2: 도메인 RED 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateVersionTest test`  
Expected: compilation FAIL because domain types do not exist.

- [ ] **Step 3: 최소 도메인 구현**

```java
public record ConsentItemDefinition(
        UUID itemId, int order, String code, ConsentItemType type,
        String title, boolean required, String purpose, String retentionText,
        String refusalNotice, String thirdPartyRecipient,
        List<UUID> processingDataItemIds) {
    public ConsentItemDefinition {
        if (type == ConsentItemType.OPTIONAL && required) {
            throw new IllegalArgumentException("선택 동의 항목은 필수로 설정할 수 없습니다.");
        }
        if ((type == ConsentItemType.THIRD_PARTY_PROVISION)
                != (thirdPartyRecipient != null && !thirdPartyRecipient.isBlank())) {
            throw new IllegalArgumentException("제3자 제공 항목만 제공받는 자를 입력해야 합니다.");
        }
        if (processingDataItemIds == null || processingDataItemIds.isEmpty()) {
            throw new IllegalArgumentException("개인정보 항목을 하나 이상 선택해야 합니다.");
        }
    }
}
```

버전 record는 상태별 필수 필드와 길이를 생성자에서 검증하고 각 전이 메서드는 새 불변 record를 반환한다.

- [ ] **Step 4: 도메인 GREEN 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateVersionTest test`  
Expected: PASS.

- [ ] **Step 5: 정제·해시 실패 테스트 작성**

```java
@Test
void removesScriptAndAttributesButKeepsApprovedMarkup() {
    String result = sanitizer.sanitize("<p onclick='x()'><strong>안내</strong><script>x()</script></p>");
    assertThat(result).isEqualTo("<p><strong>안내</strong></p>");
}

@Test
void hashIsStableAcrossMapInsertionOrderAndCrLf() {
    assertThat(hasher.sha256(contentWith("\r\n", reversedMap())))
        .isEqualTo(hasher.sha256(contentWith("\n", sortedMap())));
}
```

- [ ] **Step 6: 정제·해시 RED 확인**

Run: `./mvnw.cmd -Dtest=TemplateHtmlSanitizerTest,TemplateContentHasherTest test`  
Expected: compilation FAIL because the services do not exist.

- [ ] **Step 7: OWASP 정제와 canonical hash 구현**

`pom.xml`에 Maven Central에서 확인한 `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260313.1`을 property로 고정한다. `PolicyFactory`는 `p`, `br`, `ul`, `ol`, `li`, `strong`, `em`, `h3`, `h4`만 허용한다. 해시는 Jackson 정렬 옵션, LF 줄바꿈과 UTF-8을 사용한다.

- [ ] **Step 8: 정제·해시 GREEN 및 Task 2 커밋**

Run: `./mvnw.cmd -Dtest=ConsentTemplateVersionTest,TemplateHtmlSanitizerTest,TemplateContentHasherTest test`  
Expected: PASS.

```powershell
git add pom.xml src/main/java/kr/or/arte/seal/template src/test/java/kr/or/arte/seal/template/domain src/test/java/kr/or/arte/seal/template/application
git commit -m "feat: 동의서 템플릿 도메인 규칙 구현"
```

### Task 3: 승인 처리업무 참조와 MyBatis 저장소

**Files:**
- Create: `src/main/java/kr/or/arte/seal/policy/application/activity/ApprovedProcessingActivityReference.java`
- Create: `src/main/java/kr/or/arte/seal/policy/application/activity/ApprovedDataItemReference.java`
- Create: `src/main/java/kr/or/arte/seal/policy/application/activity/ApprovedProcessingActivityQuery.java`
- Create: `src/main/java/kr/or/arte/seal/policy/persistence/activity/MyBatisApprovedProcessingActivityQuery.java`
- Modify: `src/main/java/kr/or/arte/seal/policy/persistence/activity/ProcessingActivityMapper.java`
- Modify: `src/main/resources/mybatis/mappers/policy/ProcessingActivityMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateRepository.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateDetail.java`
- Create: `src/main/java/kr/or/arte/seal/template/persistence/ConsentTemplateMapper.java`
- Create: `src/main/java/kr/or/arte/seal/template/persistence/MyBatisConsentTemplateRepository.java`
- Create: `src/main/resources/mybatis/mappers/template/ConsentTemplateMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/template/persistence/ConsentTemplatePostgresIntegrationTest.java`

**Interfaces:**
- Produces: `ApprovedProcessingActivityQuery.findApprovedForTemplate(UUID actorId, UUID versionId)` and `ConsentTemplateRepository` lifecycle persistence methods
- Consumes: Task 1 tables and Task 2 domain records

- [ ] **Step 1: 저장소 실패 통합 테스트 작성**

```java
@Test
void savesAndReloadsDraftWithOrderedItems() {
    repository.insertTemplate(draftDetailWithItems(ITEM_2, ITEM_1), ACTOR, NOW);

    var loaded = repository.requireVersionForUpdate(ACTOR, TEMPLATE_ID, VERSION_ID);

    assertThat(loaded.items()).extracting(ConsentItemDefinition::order)
        .containsExactly(1, 2);
}
```

- [ ] **Step 2: persistence RED 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplatePostgresIntegrationTest test`  
Expected: compilation FAIL because repository types do not exist.

- [ ] **Step 3: 공개 정책 조회 인터페이스 구현**

```java
public interface ApprovedProcessingActivityQuery {
    ApprovedProcessingActivityReference findApprovedForTemplate(UUID actorId, UUID versionId);
}

public record ApprovedProcessingActivityReference(
    UUID activityId, UUID versionId, UUID ownerOrganizationId,
    String activityName, String processingPurpose, String legalBasisType,
    UUID retentionPolicyVersionId, String retentionDisplayText,
    List<ApprovedDataItemReference> dataItems) {}
```

조회 구현은 처리업무 `ACTIVE`, 버전 `APPROVED`, 보유정책 버전 `APPROVED`, 법적 근거 `CONSENT`를 모두 SQL에서 제한하고 없으면 빈 결과가 아니라 명시적 업무 예외를 반환한다.

- [ ] **Step 4: 템플릿 Mapper와 저장소 구현**

Mapper는 논리 템플릿·버전·항목·선택 항목을 각각 명시적 INSERT/UPDATE로 처리한다. 발행 메서드는 `SELECT ... FOR UPDATE`, 이전 발행 버전 retire, 새 버전 publish, current pointer 갱신 행 수를 모두 검사한다. 동적 SQL 식별자 조립은 사용하지 않는다.

```java
public interface ConsentTemplateRepository {
    void insertTemplate(ConsentTemplateDetail draft, UUID actorId, Instant now);
    ConsentTemplateDetail requireVisible(UUID actorId, UUID templateId);
    ConsentTemplateDetail requireVersionForUpdate(UUID actorId, UUID templateId, UUID versionId);
    void replaceDraft(ConsentTemplateVersion draft, UUID actorId, Instant now);
    void updateTransition(ConsentTemplateVersion version, UUID actorId, Instant now);
    void publish(UUID templateId, UUID versionId, UUID actorId, String contentHash, Instant now);
    UUID copyNextDraft(UUID actorId, UUID templateId, UUID sourceVersionId, Instant now);
    void updateStatus(UUID actorId, UUID templateId, String status, long versionNo, Instant now);
    List<ConsentTemplateDetail> searchVisible(UUID actorId, String keyword, String status);
}
```

- [ ] **Step 5: persistence GREEN 확인 및 Task 3 커밋**

Run: `./mvnw.cmd -Dtest=ConsentTemplatePostgresIntegrationTest test`  
Expected: PASS with real PostgreSQL.

```powershell
git add src/main/java/kr/or/arte/seal/policy src/main/resources/mybatis/mappers/policy src/main/java/kr/or/arte/seal/template src/main/resources/mybatis/mappers/template src/test/java/kr/or/arte/seal/template/persistence
git commit -m "feat: 동의서 템플릿 저장소 구현"
```

### Task 4: 템플릿 생성·검토·승인·발행 서비스

**Files:**
- Create: `src/main/java/kr/or/arte/seal/template/application/CreateConsentTemplateCommand.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentItemDraftCommand.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/UpdateConsentTemplateDraftCommand.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateManagement.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateManagementService.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateQueryService.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplatePolicyValidator.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplateBusinessConflictException.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplateNotFoundException.java`
- Test: `src/test/java/kr/or/arte/seal/template/application/ConsentTemplateManagementServiceTest.java`

**Interfaces:**
- Produces: create/update/requestReview/approve/reject/publish/createNextDraft/status operations
- Consumes: `ApprovedProcessingActivityQuery`, `ConsentTemplateRepository`, sanitizer, hasher, `AuditEventWriter`, `Clock`

```java
public record ConsentItemDraftCommand(
    String code, ConsentItemType type, String title, boolean required,
    String purpose, String retentionText, String refusalNotice,
    String thirdPartyRecipient, List<UUID> processingDataItemIds) {}

public record CreateConsentTemplateCommand(
    String templateCode, String templateName, UUID processingActivityVersionId,
    String title, String introductionHtml, List<ConsentItemDraftCommand> items,
    UUID actorId) {}

public record UpdateConsentTemplateDraftCommand(
    UUID templateId, UUID versionId, String templateName, String title,
    String introductionHtml, List<ConsentItemDraftCommand> items,
    long templateVersionNo, long contentVersionNo, UUID actorId) {}

public interface ConsentTemplateManagement {
    public UUID create(CreateConsentTemplateCommand command);
    public void updateDraft(UpdateConsentTemplateDraftCommand command);
    public void requestReview(UUID templateId, UUID versionId, UUID actorId);
    public void approve(UUID templateId, UUID versionId, UUID actorId);
    public void reject(UUID templateId, UUID versionId, UUID actorId, String comment);
    public void publish(UUID templateId, UUID versionId, UUID actorId);
    public UUID createNextDraft(UUID templateId, UUID sourceVersionId, UUID actorId);
    public void updateStatus(UUID templateId, String status, long versionNo, UUID actorId);
}
```

- [ ] **Step 1: 애플리케이션 실패 테스트 작성**

```java
@Test
void publishRevalidatesActivityAndRetiresPreviousPublishedVersion() {
    given(repository.requireVersionForUpdate(PUBLISHER, TEMPLATE_ID, NEW_VERSION))
        .willReturn(approvedDetail());
    given(policyQuery.findApprovedForTemplate(AUTHOR, ACTIVITY_VERSION)).willReturn(reference());

    service.publish(TEMPLATE_ID, NEW_VERSION, PUBLISHER);

    then(repository).should().publish(TEMPLATE_ID, NEW_VERSION, PUBLISHER, EXPECTED_HASH, NOW);
    then(auditWriter).should().append(argThat(e ->
        e.actionCode().equals("CONSENT_TEMPLATE_PUBLISH")));
}

@Test
void reviewRequestRejectsDataItemOutsideApprovedActivity() {
    assertThatThrownBy(() -> service.requestReview(TEMPLATE_ID, VERSION_ID, ACTOR))
        .isInstanceOf(TemplateBusinessConflictException.class);
}
```

- [ ] **Step 2: service RED 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateManagementServiceTest test`  
Expected: compilation FAIL because service and commands do not exist.

- [ ] **Step 3: 최소 서비스 구현**

```java
@Transactional
public void publish(UUID templateId, UUID versionId, UUID actorId) {
    ConsentTemplateDetail detail = repository.requireDetailForUpdate(templateId, versionId);
    ApprovedProcessingActivityReference policy =
        policyQuery.findApprovedForTemplate(actorId, detail.version().processingActivityVersionId());
    validator.validateAgainst(detail.version(), policy);
    String hash = hasher.sha256(detail, policy);
    repository.publish(templateId, versionId, actorId, hash, clock.instant());
    audit("CONSENT_TEMPLATE_PUBLISH", templateId, versionId, actorId, hash);
}
```

생성·수정 때 HTML을 정제하고 `COLLECTION_USE`/`OPTIONAL` 보유기간은 policy 참조에서 덮어쓴다. 모든 상태 전이는 도메인 메서드를 먼저 거치고 저장 행 수가 1이 아니면 409 예외로 변환한다.

- [ ] **Step 4: service GREEN 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateManagementServiceTest test`  
Expected: PASS for normal, invalid state, self-approval, stale policy and duplicate publication paths.

- [ ] **Step 5: Task 4 커밋**

```powershell
git add src/main/java/kr/or/arte/seal/template/application src/test/java/kr/or/arte/seal/template/application
git commit -m "feat: 동의서 템플릿 승인 워크플로우 구현"
```

### Task 5: 관리자 MVC와 구조화 입력 화면

**Files:**
- Create: `src/main/java/kr/or/arte/seal/template/web/ConsentTemplateController.java`
- Create: `src/main/java/kr/or/arte/seal/template/web/ConsentTemplateForm.java`
- Create: `src/main/java/kr/or/arte/seal/template/web/ConsentItemForm.java`
- Create: `src/main/java/kr/or/arte/seal/template/web/TemplateWebExceptionHandler.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/templates/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/templates/form.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/templates/detail.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/templates/preview.jsp`
- Create: `src/main/webapp/resources/css/template.css`
- Create: `src/main/webapp/resources/js/template-form.js`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/home.jsp`
- Test: `src/test/java/kr/or/arte/seal/template/web/ConsentTemplateControllerTest.java`

**Interfaces:**
- Produces: 설계서의 `/admin/templates/**` GET/POST URL
- Consumes: Task 4 management/query services and Spring Security authorities

- [ ] **Step 1: 권한·CSRF·폼 실패 MVC 테스트 작성**

```java
@Test
void listRequiresViewPermission() throws Exception {
    mockMvc.perform(get("/admin/templates").with(authentication(admin())))
        .andExpect(status().isForbidden());
}

@Test
void createRejectsPostWithoutCsrf() throws Exception {
    mockMvc.perform(post("/admin/templates")
            .with(authentication(admin("CONSENT_TEMPLATE_EDIT"))))
        .andExpect(status().isForbidden());
}

@Test
void outOfScopeDetailIsNotFound() throws Exception {
    given(query.requireScoped(TEMPLATE_ID, USER_ID)).willThrow(new TemplateNotFoundException());
    mockMvc.perform(get("/admin/templates/{id}", TEMPLATE_ID)
            .with(authentication(admin("CONSENT_TEMPLATE_VIEW"))))
        .andExpect(status().isNotFound());
}
```

- [ ] **Step 2: MVC RED 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateControllerTest test`  
Expected: compilation FAIL because controller/form types do not exist.

- [ ] **Step 3: 컨트롤러와 폼 구현**

권한 매핑은 다음과 같이 고정한다.

```text
목록·상세·미리보기 GET                         CONSENT_TEMPLATE_VIEW
생성·편집 GET/POST, 새 버전 POST               CONSENT_TEMPLATE_EDIT
검토요청 POST                                   CONSENT_TEMPLATE_REVIEW
승인·반려 POST                                  CONSENT_TEMPLATE_APPROVE
발행·상태변경 POST                              CONSENT_TEMPLATE_PUBLISH
```

`ConsentTemplateQueryService`는 `search(UUID actorId, String keyword, String status)`, `requireScoped(UUID templateId, UUID actorId)`, `requireVersionScoped(UUID templateId, UUID versionId, UUID actorId)`를 제공한다. Form은 항목 목록에 `@Valid`를 적용하고 빈 목록, 중복 코드와 순서는 service에서 다시 검증한다. 409 예외는 오류 JSP가 아니라 상세 화면으로 되돌려 최신 상태 메시지를 표시한다.

- [ ] **Step 4: JSP·네이티브 JS 구현**

JSP는 `<c:out>`과 Spring form 태그를 기본으로 사용한다. 정제된 `introduction_html`은 미리보기에서만 신뢰된 서버 결과로 출력한다. `template-form.js`는 항목 행 복제와 이름 인덱스 재부여만 담당하고 승인·검증 로직은 포함하지 않는다. 사용자 수정 중인 공통 `app.css`는 수정하지 않고 새 `template.css`만 추가한다.

- [ ] **Step 5: MVC GREEN 확인**

Run: `./mvnw.cmd -Dtest=ConsentTemplateControllerTest test`  
Expected: PASS for 200/302/403/404/CSRF/form-error cases.

- [ ] **Step 6: Task 5 커밋**

```powershell
git add src/main/java/kr/or/arte/seal/template/web src/main/webapp/WEB-INF/jsp/admin/templates src/main/webapp/resources/css/template.css src/main/webapp/resources/js/template-form.js src/main/webapp/WEB-INF/jsp/admin/home.jsp src/test/java/kr/or/arte/seal/template/web
git commit -m "feat: 동의서 템플릿 관리자 화면 구현"
```

### Task 6: 새 버전 복제와 단일 원본 미리보기

**Files:**
- Create: `src/main/java/kr/or/arte/seal/template/application/PublishedTemplateView.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/TemplatePreviewRenderer.java`
- Create: `src/main/webapp/WEB-INF/jsp/fragments/consent/template-content.jsp`
- Modify: `src/main/java/kr/or/arte/seal/template/application/ConsentTemplateManagementService.java`
- Modify: `src/main/java/kr/or/arte/seal/template/web/ConsentTemplateController.java`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/templates/preview.jsp`
- Test: `src/test/java/kr/or/arte/seal/template/application/TemplatePreviewRendererTest.java`
- Test: `src/test/java/kr/or/arte/seal/template/application/ConsentTemplateVersionCopyTest.java`

**Interfaces:**
- Produces: 이메일·공개 웹이 다음 단계에서 재사용할 `PublishedTemplateView`와 JSP fragment
- Consumes: 발행/폐기 버전의 구조화 데이터

- [ ] **Step 1: 복제·렌더링 실패 테스트 작성**

```java
@Test
void createsNextDraftWithoutCopyingWorkflowActorsOrHash() {
    var copied = service.createNextDraft(TEMPLATE_ID, PUBLISHED_VERSION, EDITOR);
    assertThat(copied.versionNumber()).isEqualTo(3);
    assertThat(copied.workflowStatus()).isEqualTo(TemplateWorkflowStatus.DRAFT);
    assertThat(copied.approvedBy()).isNull();
    assertThat(copied.contentHash()).isNull();
}

@Test
void previewUsesStructuredItemOrder() {
    assertThat(renderer.toView(detailWithItems(2, 1)).items())
        .extracting(PublishedTemplateView.Item::order).containsExactly(1, 2);
}
```

- [ ] **Step 2: RED 확인**

Run: `./mvnw.cmd -Dtest=TemplatePreviewRendererTest,ConsentTemplateVersionCopyTest test`  
Expected: compilation FAIL because renderer/view/copy operation do not exist.

- [ ] **Step 3: 최소 복제·공유 view 구현**

복제는 source의 표시 내용과 선택 항목만 새 UUID로 복사한다. 승인·발행·반려 필드와 해시는 NULL로 초기화한다. `PublishedTemplateView`는 개인정보나 관리자 식별자를 포함하지 않고 제목, 안내문, 처리 목적, 보유기간과 정렬된 결정 항목만 노출한다.

- [ ] **Step 4: GREEN 확인 및 Task 6 커밋**

Run: `./mvnw.cmd -Dtest=TemplatePreviewRendererTest,ConsentTemplateVersionCopyTest test`  
Expected: PASS.

```powershell
git add src/main/java/kr/or/arte/seal/template src/main/webapp/WEB-INF/jsp/fragments/consent src/main/webapp/WEB-INF/jsp/admin/templates/preview.jsp src/test/java/kr/or/arte/seal/template/application
git commit -m "feat: 발행 템플릿 단일 원본 미리보기 구현"
```

### Task 7: 단계 통합 검증과 운영 문서

**Files:**
- Modify: `README.md`
- Modify: `database/README.md`
- Modify: `docs/superpowers/plans/2026-08-24-phase2-consent-template-implementation.md`

**Interfaces:**
- Consumes: Task 1~6 전체 산출물
- Produces: 적용·검증 명령과 Phase 2 완료 증거

- [ ] **Step 1: SQL 체크섬과 검증 실행**

패치 파일 SHA-256을 계산해 `psql` 변수로 넘기고 `seal_test`에 적용한다. 검증 SQL은 모든 assertion을 통과해야 하며 재적용은 명시적 중복 오류로 중단돼야 한다.

- [ ] **Step 2: 전체 테스트 실행**

Run: `./mvnw.cmd test`  
Expected: all tests PASS, 0 failures, 0 errors, 0 skipped.

- [ ] **Step 3: 실행형 WAR 패키징**

Run: `./mvnw.cmd clean package`  
Expected: exit 0 and `target/seal.war` exists.

- [ ] **Step 4: 9090 대표 화면 스모크**

애플리케이션을 local profile로 실행하고 다음을 확인한다.

```text
GET  /login                                                     -> 200
GET  /admin/templates                                           -> 200 after login and permission
POST /admin/templates                                           -> 302 with CSRF
GET  /admin/templates/{id}/versions/{versionId}/preview          -> 200
POST /admin/templates/{id}/versions/{versionId}/publish          -> 302, then detail shows PUBLISHED
GET  same out-of-scope template                                  -> 404
```

대표 데스크톱과 360px 모바일 폭에서 목록, 폼, 상세, 미리보기의 겹침·키보드 포커스·레이블을 확인한다.

- [ ] **Step 5: 문서와 계획 체크박스 갱신**

README에 템플릿 URL, 권한과 SQL 적용 순서를 추가한다. 본 계획의 실제 완료 단계만 `[x]`로 바꾸고 외부 SMTP·HTTPS는 Phase 2 검증 대상이 아님을 기록한다.

- [ ] **Step 6: 최종 커밋**

```powershell
git add README.md database/README.md docs/superpowers/plans/2026-08-24-phase2-consent-template-implementation.md
git commit -m "docs: 2단계 템플릿 검증 절차 기록"
```

- [ ] **Step 7: 원격 푸시 전 최종 대조**

`git status --short`, `git diff --check`, `git log --oneline origin/main..HEAD`를 확인한다. 사용자 `app.css`, PPT·DOCX와 `tools/`는 포함하지 않는다. 검증된 `feature/phase2-template`만 Private GitHub 저장소에 푸시한다.
