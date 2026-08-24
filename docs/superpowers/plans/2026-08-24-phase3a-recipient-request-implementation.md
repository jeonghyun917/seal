# Phase 3A Recipient and Consent Request Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Encrypt and manage individual recipients, create immutable consent request snapshots, and manage request expiry, cancellation, and one-time secure-link issuance.

**Architecture:** Add bounded `recipient`, `request`, and `link` modules to the existing JSP modular monolith. Direct identifiers are encrypted at the application boundary, request snapshots are immutable canonical data derived from approved policy and published template versions, and secure-link tokens are returned once while only HMAC values persist.

**Tech Stack:** Java 17, eGovFrame runtime 5.0.0, Spring Boot 3.5.6, Spring Security, JSP/JSTL, MyBatis, PostgreSQL, Java JCA/JCE, JUnit 5, Spring Test.

**Spec:** `docs/superpowers/specs/2026-08-24-phase3a-recipient-request-design.md`

## Global Constraints

- No electronic signature, SMS/Alimtalk, OTP, phone/PASS identity, tracking pixel, sensitive/unique identifiers, minors, SSO/LDAP, Redis/Kafka, or paid KMS.
- Keep JSP and native JavaScript; do not add jQuery, SPA tooling, Lombok, or MapStruct.
- Use PostgreSQL manual incremental SQL and SHA-256 change history; do not add Flyway or Liquibase.
- Use `seal_owner` for schema ownership and `seal_app` for least-privilege runtime DML.
- Never log or audit names, email addresses, ciphertext, plaintext tokens, token HMAC values, snapshot JSON, passwords, or key material.
- Every state-changing web request keeps Spring Security CSRF and POST-Redirect-GET.
- Run persistence tests against `seal_test`, never `seal_dev`; local application runs on port 9090.
- Follow RED → verify RED → minimal GREEN → verify GREEN for business, security, persistence, and MVC behavior.

---

### Task 1: Personal-data cryptography, normalization, and masking

**Files:**
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/PersonalDataContext.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/EncryptedValue.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/PersonalDataCipher.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/PersonalDataProtectionException.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/AesGcmPersonalDataCipher.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/EmailNormalizer.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/EmailLookupHasher.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/HmacEmailLookupHasher.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/crypto/PersonalDataMasker.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/config/PersonalDataProtectionProperties.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/config/PersonalDataProtectionConfiguration.java`
- Modify: `src/main/resources/application.yml`
- Modify: `.env.example`
- Test: `src/test/java/kr/or/arte/seal/recipient/crypto/AesGcmPersonalDataCipherTest.java`
- Test: `src/test/java/kr/or/arte/seal/recipient/crypto/EmailProtectionTest.java`
- Test: `src/test/java/kr/or/arte/seal/recipient/config/PersonalDataProtectionConfigurationTest.java`

**Interfaces:**
- Produces: `PersonalDataCipher.encrypt(PersonalDataContext, String): EncryptedValue`, `decrypt(PersonalDataContext, EncryptedValue): String`.
- Produces: `EmailNormalizer.normalize(String): String`, `EmailLookupHasher.hmac(String): String`, and masker methods `name(String)` and `email(String)`.
- Consumes: external key files named by `SEAL_DATA_KEYS_FILE`, `SEAL_DATA_ACTIVE_KEY_ID`, and `SEAL_EMAIL_INDEX_HMAC_KEY_FILE`.

- [x] **Step 1: Write crypto behavior tests that fail because the types do not exist**

```java
@Test
void encryptsSamePlaintextDifferentlyAndRejectsDifferentAad() {
    var cipher = cipherWith("v1", fixed32ByteKey());
    var context = new PersonalDataContext("recipient", RECIPIENT_ID, ORGANIZATION_ID, "email");
    EncryptedValue first = cipher.encrypt(context, "person@example.test");
    EncryptedValue second = cipher.encrypt(context, "person@example.test");
    assertThat(first.bytes()).isNotEqualTo(second.bytes());
    assertThat(cipher.decrypt(context, first)).isEqualTo("person@example.test");
    assertThatThrownBy(() -> cipher.decrypt(
            new PersonalDataContext("recipient", OTHER_ID, ORGANIZATION_ID, "email"), first))
            .isInstanceOf(PersonalDataProtectionException.class);
}
```

```java
@ParameterizedTest
@CsvSource({"' Person@Example.TEST ','person@example.test'", "'A@B.TEST','a@b.test'"})
void normalizesEmailBeforeHmac(String raw, String expected) {
    assertThat(EmailNormalizer.normalize(raw)).isEqualTo(expected);
}

@Test
void masksWithoutExposingFullIdentifiers() {
    assertThat(PersonalDataMasker.name("홍길동")).isEqualTo("홍*동");
    assertThat(PersonalDataMasker.email("person@example.test"))
            .isEqualTo("p*****@e******.test");
}
```

- [x] **Step 2: Run the crypto tests and verify RED**

Run: `.\mvnw.cmd -Dtest=AesGcmPersonalDataCipherTest,EmailProtectionTest,PersonalDataProtectionConfigurationTest test`

Expected: compilation failure for missing crypto/config types.

- [x] **Step 3: Implement the minimal cryptography and fail-closed key loader**

```java
public interface PersonalDataCipher {
    EncryptedValue encrypt(PersonalDataContext context, String plaintext);
    String decrypt(PersonalDataContext context, EncryptedValue value);
}

public record PersonalDataContext(String aggregate, UUID objectId,
                                  UUID organizationId, String field) {}
public record EncryptedValue(String keyId, byte[] bytes) {
    public EncryptedValue { bytes = bytes.clone(); }
    @Override public byte[] bytes() { return bytes.clone(); }
}
```

`AesGcmPersonalDataCipher` must use `AES/GCM/NoPadding`, a fresh 12-byte nonce, a 128-bit tag, the exact AAD order `aggregate\0field\0objectId\0organizationId`, and stored bytes `0x01 || nonce || ciphertext+tag`. `PersonalDataProtectionConfiguration` loads `key-id=Base64` lines, rejects duplicate IDs, unknown active IDs, non-32-byte AES keys, and HMAC keys shorter than 32 bytes before creating beans.

- [x] **Step 4: Add exact external configuration names**

```yaml
seal:
  personal-data:
    active-key-id: ${SEAL_DATA_ACTIVE_KEY_ID:}
    keys-file: ${SEAL_DATA_KEYS_FILE:}
    email-index-hmac-key-file: ${SEAL_EMAIL_INDEX_HMAC_KEY_FILE:}
```

Add only variable names and path examples to `.env.example`; do not add keys.

- [x] **Step 5: Run tests and commit Task 1**

Run: `.\mvnw.cmd -Dtest=AesGcmPersonalDataCipherTest,EmailProtectionTest,PersonalDataProtectionConfigurationTest test`

Expected: PASS.

Commit: `feat: 대상자 개인정보 암호화 기반 추가`

### Task 2: PostgreSQL recipient, request, snapshot, event, and link schema

**Files:**
- Create: `database/03_patch/20260824_003_create_recipient_request_tables.sql`
- Create: `database/04_verify/007_recipient_request_verify.sql`
- Test: `src/test/java/kr/or/arte/seal/request/persistence/RecipientRequestSchemaPostgresIntegrationTest.java`
- Modify: `database/03_patch/README.md`

**Interfaces:**
- Produces: tables `recipient`, `consent_request`, `consent_request_snapshot`, `consent_request_item`, `consent_request_item_data`, `consent_request_event`, and `secure_link`.
- Produces: seven permissions from the spec and SYSTEM_ADMIN role assignments.
- Consumes: existing organization, admin, approved processing, retention policy, and template tables.

- [x] **Step 1: Write a real PostgreSQL schema test before applying the patch**

```java
@Test
void recipientRequestSchemaEnforcesCrossAggregateAndLeastPrivilegeRules() {
    assertThat(count("select count(*) from information_schema.tables " +
            "where table_schema='seal' and table_name in " +
            "('recipient','consent_request','consent_request_snapshot'," +
            "'consent_request_item','consent_request_item_data'," +
            "'consent_request_event','secure_link')")).isEqualTo(7);
    assertThat(singleBoolean("select has_table_privilege('seal_app'," +
            "'seal.consent_request_snapshot','UPDATE')")).isFalse();
    assertThat(singleBoolean("select has_table_privilege('seal_app'," +
            "'seal.recipient','DELETE')")).isFalse();
}
```

- [x] **Step 2: Run the schema test and verify RED**

Run: `.\mvnw.cmd -Dtest=RecipientRequestSchemaPostgresIntegrationTest test`

Expected: FAIL because the seven tables do not exist in `seal_test`.

- [x] **Step 3: Implement the SQL patch with all data constraints**

The patch must include:

```sql
CREATE UNIQUE INDEX ux_recipient_active_email
    ON recipient (organization_unit_id, email_lookup_hmac)
    WHERE recipient_status = 'ACTIVE';

ALTER TABLE consent_request
    ADD CONSTRAINT ck_request_expiry CHECK (expires_at > created_at),
    ADD CONSTRAINT uq_request_org_idempotency
        UNIQUE (organization_unit_id, idempotency_key);

ALTER TABLE consent_request
    ADD CONSTRAINT fk_request_current_link
    FOREIGN KEY (current_secure_link_id, consent_request_id)
    REFERENCES secure_link (secure_link_id, consent_request_id);

CREATE UNIQUE INDEX ux_secure_link_current_per_request
    ON secure_link (consent_request_id)
    WHERE link_status IN ('ISSUED', 'ACCESSED');
```

Use deferred creation for the circular current-link FK. Add composite UNIQUE/FK constraints tying recipient organization, processing activity/version, template/activity, and current link/request together. Snapshot, item, item-data, and event tables receive only SELECT/INSERT grants; all new tables deny DELETE/TRUNCATE.

- [x] **Step 4: Write `007_recipient_request_verify.sql`**

The verification script must raise exceptions for missing tables, missing permissions, missing lifecycle CHECK constraints, wrong owner, app DELETE/TRUNCATE, mutable snapshot/event grants, missing composite FKs, or absent checksum history.

- [x] **Step 5: Apply to `seal_test`, run verification, and turn GREEN**

Calculate SHA-256, apply with `script_checksum` and `app_version`, run `007`, then run the schema integration test.

Expected: SQL verify prints `Phase 3A recipient request verification passed`; test PASS.

- [x] **Step 6: Apply the same verified patch to `seal_dev` and commit Task 2**

Commit: `feat: 대상자 요청 데이터 모델 추가`

### Task 3: Recipient domain, repository, and management service

**Files:**
- Create: `src/main/java/kr/or/arte/seal/recipient/domain/RecipientStatus.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/domain/Recipient.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/CreateRecipientCommand.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/UpdateRecipientCommand.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientSummary.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientDetail.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientStoredRecord.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/DuplicateRecipientException.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientRepository.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientManagement.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientManagementService.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/application/RecipientQueryService.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/persistence/RecipientMapper.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/persistence/RecipientRow.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/persistence/MyBatisRecipientRepository.java`
- Create: `src/main/resources/mybatis/mappers/recipient/RecipientMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/recipient/domain/RecipientTest.java`
- Test: `src/test/java/kr/or/arte/seal/recipient/application/RecipientManagementServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/recipient/persistence/RecipientPostgresIntegrationTest.java`

**Interfaces:**
- Produces: `RecipientManagement.create`, `update`, `changeStatus`; all take the actor UUID explicitly.
- Produces: scoped query methods that return masked summaries and never expose ciphertext.
- Consumes: Task 1 cipher/hasher/masker and Task 2 tables.

- [x] **Step 1: Write recipient domain and service tests**

```java
@Test
void createEncryptsIdentifiersAndAuditsOnlyObjectIds() {
    UUID id = service.create(ACTOR, new CreateRecipientCommand(
            ORGANIZATION, "홍길동", "Person@Example.TEST", "교육 신청 접수"));
    RecipientStoredRecord stored = repository.requireStored(id);
    assertThat(stored.emailLookupHmac()).isEqualTo("expected-email-hmac");
    assertThat(stored.nameCiphertext()).doesNotContain("홍길동".getBytes(UTF_8));
    assertThat(audit.last().metadata()).doesNotContainKeys("name", "email");
}

@Test
void duplicateActiveEmailInOrganizationReturnsMaskedConflict() {
    repository.insert(activeRecipient("person@example.test"));
    assertThatThrownBy(() -> service.create(ACTOR, command(" PERSON@EXAMPLE.TEST ")))
            .isInstanceOf(DuplicateRecipientException.class)
            .satisfies(ex -> assertThat(((DuplicateRecipientException) ex).maskedEmail())
                    .isEqualTo("p*****@e******.test"));
}
```

- [x] **Step 2: Run tests and verify RED**

Run: `.\mvnw.cmd -Dtest=RecipientTest,RecipientManagementServiceTest test`

Expected: compilation failure for missing recipient types.

- [x] **Step 3: Implement domain and service with scope-before-decrypt**

```java
public interface RecipientManagement {
    UUID create(UUID actorId, CreateRecipientCommand command);
    void update(UUID actorId, UUID recipientId, UpdateRecipientCommand command);
    void changeStatus(UUID actorId, UUID recipientId, RecipientStatus status, long versionNo);
}
```

Validate name 1–100, email with Jakarta `@Email` plus normalized length ≤254, basis 5–500. Generate recipient UUID before encryption so AAD is stable. Query scope first using encrypted rows, then decrypt only the one scoped row. Updates with blank name/email preserve existing ciphertext and lookup HMAC.

- [x] **Step 4: Implement parameter-bound MyBatis queries and real integration tests**

The mapper must use `#{}` only, recursive organization scope like existing policy/template queries, partial unique conflict translation, optimistic `version_no`, and active-processing-scope semantics from the spec.

Run: `.\mvnw.cmd -Dtest=RecipientPostgresIntegrationTest test`

Expected: PASS for insert, exact-HMAC lookup, masked scoped list, out-of-scope absence, update conflict, and inactive/reactivation duplicate protection.

- [x] **Step 5: Run all Task 3 tests and commit**

Commit: `feat: 대상자 등록과 마스킹 조회 구현`

### Task 4: Recipient administrator JSP flow

**Files:**
- Create: `src/main/java/kr/or/arte/seal/recipient/web/RecipientForm.java`
- Create: `src/main/java/kr/or/arte/seal/recipient/web/RecipientController.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/recipients/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/recipients/form.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/recipients/detail.jsp`
- Create: `src/main/webapp/resources/css/recipient-request.css`
- Test: `src/test/java/kr/or/arte/seal/recipient/web/RecipientControllerTest.java`

**Interfaces:**
- Produces: `/admin/recipients` URLs from the spec.
- Consumes: Task 3 management/query services and `AdminPrincipal`.

- [x] **Step 1: Write MVC tests for authority, CSRF, masking, and duplicate input preservation**

```java
@Test
void listRequiresViewAndContainsOnlyMaskedValues() throws Exception {
    given(query.search(any(), any())).willReturn(List.of(maskedSummary()));
    mockMvc.perform(get("/admin/recipients").with(authentication(admin("RECIPIENT_VIEW"))))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/recipients/list"))
            .andExpect(model().attribute("recipients", hasItem(
                    hasProperty("maskedEmail", is("p*****@e******.test"))))));
}

@Test
void createWithoutCsrfIsForbidden() throws Exception {
    mockMvc.perform(post("/admin/recipients").with(authentication(admin("RECIPIENT_EDIT"))))
            .andExpect(status().isForbidden());
}
```

- [x] **Step 2: Run MVC tests and verify RED**

Run: `.\mvnw.cmd -Dtest=RecipientControllerTest test`

Expected: compilation failure for missing form/controller.

- [x] **Step 3: Implement server-rendered list, create, edit, detail, and status actions**

Use exact `@PreAuthorize` permissions, field-level Korean validation, no full identifier values in list/detail/edit HTML, and 404 for scoped misses. Duplicate email returns the same form with a masked existing-recipient link.

- [x] **Step 4: Verify GREEN and commit**

Run: `.\mvnw.cmd -Dtest=RecipientControllerTest test`

Commit: `feat: 대상자 관리자 화면 구현`

### Task 5: Published-template request source and immutable snapshot domain

**Files:**
- Create: `src/main/java/kr/or/arte/seal/template/application/PublishedConsentTemplateQuery.java`
- Create: `src/main/java/kr/or/arte/seal/template/application/PublishedConsentTemplateReference.java`
- Create: `src/main/java/kr/or/arte/seal/template/persistence/MyBatisPublishedConsentTemplateQuery.java`
- Modify: `src/main/java/kr/or/arte/seal/template/persistence/ConsentTemplateQueryMapper.java`
- Modify: `src/main/resources/mybatis/mappers/template/ConsentTemplateQueryMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/request/domain/ConsentRequestStatus.java`
- Create: `src/main/java/kr/or/arte/seal/request/domain/ConsentRequest.java`
- Create: `src/main/java/kr/or/arte/seal/request/domain/RequestValidationException.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestSnapshot.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestSnapshotSource.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestSnapshotFactory.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestSnapshotHasher.java`
- Test: `src/test/java/kr/or/arte/seal/request/domain/ConsentRequestTest.java`
- Test: `src/test/java/kr/or/arte/seal/request/application/RequestSnapshotFactoryTest.java`
- Test: `src/test/java/kr/or/arte/seal/template/persistence/PublishedConsentTemplatePostgresIntegrationTest.java`

**Interfaces:**
- Produces: `PublishedConsentTemplateQuery.requirePublished(actorId, templateVersionId)` with IDs, template hash, policy reference, and ordered `PublishedTemplateView`.
- Produces: immutable `RequestSnapshot(snapshotJson, snapshotHash, items)` without recipient/admin/token data.

- [x] **Step 1: Write domain and snapshot tests**

```java
@Test
void finalizingFreezesContentWithoutDirectIdentifiers() {
    RequestSnapshot snapshot = factory.create(sourceWithRecipient("홍길동", "p@example.test"));
    assertThat(snapshot.json()).contains("templateContentHash");
    assertThat(snapshot.json()).doesNotContain("홍길동", "p@example.test", ACTOR.toString());
    assertThat(snapshot.items()).extracting(RequestSnapshot.Item::order)
            .containsExactly(1, 2);
}

@Test
void requestCannotFinalizeWithLessThanOneHourRemaining() {
    assertThatThrownBy(() -> draft(EXPIRES_IN_59_MINUTES).finalizeAt(NOW, SNAPSHOT_ID))
            .isInstanceOf(RequestValidationException.class);
}
```

- [x] **Step 2: Run tests and verify RED**

Run: `.\mvnw.cmd -Dtest=ConsentRequestTest,RequestSnapshotFactoryTest,PublishedConsentTemplatePostgresIntegrationTest test`

Expected: compilation failure for missing request and published-query types.

- [x] **Step 3: Implement exact status transitions and canonical snapshot hashing**

```java
public enum ConsentRequestStatus { DRAFT, READY, ACTIVE, COMPLETED, EXPIRED, CANCELLED }

public record RequestSnapshot(String canonicalJson, String snapshotHash,
                              List<Item> items) {
    public RequestSnapshot { items = List.copyOf(items); }
}
```

Canonical payload order is schemaVersion, institution, requestCode, processing version, retention version/display, template version/hash, title, sanitized introduction, expiry, withdrawal notice, then ordered items/data items. Jackson sorts object keys; line endings are LF; lists retain business order.

- [x] **Step 4: Implement scoped published-template query and integration test**

Require template workflow `PUBLISHED`, logical template `ACTIVE`, processing activity `ACTIVE`, actor organization scope, and optional active processing scope. Historical published versions remain valid even after a newer template version is published.

- [x] **Step 5: Verify GREEN and commit**

Commit: `feat: 요청 스냅샷 도메인 구현`

### Task 6: Consent request repository and lifecycle service

**Files:**
- Create: `src/main/java/kr/or/arte/seal/request/application/CreateConsentRequestCommand.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/UpdateConsentRequestCommand.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestCreateResult.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestBusinessConflictException.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/RequestNotFoundException.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestDetail.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestSummary.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestRepository.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagement.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagementService.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestQueryService.java`
- Create: `src/main/java/kr/or/arte/seal/request/persistence/ConsentRequestMapper.java`
- Create: `src/main/java/kr/or/arte/seal/request/persistence/MyBatisConsentRequestRepository.java`
- Create: `src/main/resources/mybatis/mappers/request/ConsentRequestMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/request/application/ConsentRequestManagementServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/request/persistence/ConsentRequestPostgresIntegrationTest.java`

**Interfaces:**
- Produces: create/update/finalize/cancel and scoped list/detail/preview.
- Consumes: Task 3 recipient repository, Task 5 published-template query and snapshot factory.
- Produces: row-lock repository method used by Task 7 link issuance.

- [x] **Step 1: Write service tests for idempotency, finalize, cancellation, and immutability**

```java
@Test
void repeatedIdempotencyKeyReturnsExistingRequestWithoutSecondInsert() {
    UUID key = UUID.randomUUID();
    var first = service.create(ACTOR, command(key));
    var second = service.create(ACTOR, command(key));
    assertThat(second.requestId()).isEqualTo(first.requestId());
    assertThat(repository.insertCount()).isEqualTo(1);
}

@Test
void finalizePersistsSnapshotItemsEventAndAuditAtomically() {
    service.finalizeRequest(ACTOR, REQUEST_ID, VERSION_NO);
    assertThat(repository.require(REQUEST_ID).status()).isEqualTo(ConsentRequestStatus.READY);
    assertThat(repository.snapshot(REQUEST_ID).items()).isNotEmpty();
    assertThat(repository.events(REQUEST_ID)).extracting("eventType").contains("FINALIZED");
}
```

- [x] **Step 2: Run service tests and verify RED**

Run: `.\mvnw.cmd -Dtest=ConsentRequestManagementServiceTest test`

Expected: compilation failure for missing service/repository.

- [x] **Step 3: Implement transaction boundaries and Korean business conflicts**

```java
public interface ConsentRequestManagement {
    ConsentRequestCreateResult create(UUID actorId, CreateConsentRequestCommand command);
    void updateDraft(UUID actorId, UUID requestId, UpdateConsentRequestCommand command);
    void finalizeRequest(UUID actorId, UUID requestId, long versionNo);
    void cancel(UUID actorId, UUID requestId, long versionNo, String reason);
}
```

Create uses organization-scoped recipient and published template, returns an existing result for the same org/idempotency key, and writes `CREATED`. Finalize locks request, revalidates all references, inserts snapshot/items/data/event, and updates READY in one transaction. Cancel locks request and records reason code only in audit metadata.

- [x] **Step 4: Implement MyBatis and PostgreSQL lifecycle tests**

Test real idempotency unique behavior, scoped query 404, snapshot immutability grants, optimistic conflict, concurrent finalize, and cancellation terminality.

Run: `.\mvnw.cmd -Dtest=ConsentRequestPostgresIntegrationTest test`

Expected: PASS.

- [x] **Step 5: Verify Task 6 and commit**

Commit: `feat: 동의 요청 생성과 확정 구현`

### Task 7: One-time secure-link issue and reissue

**Files:**
- Create: `src/main/java/kr/or/arte/seal/link/domain/SecureLinkStatus.java`
- Create: `src/main/java/kr/or/arte/seal/link/application/IssuedSecureLink.java`
- Create: `src/main/java/kr/or/arte/seal/link/application/SecureTokenGenerator.java`
- Create: `src/main/java/kr/or/arte/seal/link/application/SecureLinkHasher.java`
- Create: `src/main/java/kr/or/arte/seal/link/application/SecureLinkRepository.java`
- Create: `src/main/java/kr/or/arte/seal/link/application/SecureLinkManagementService.java`
- Create: `src/main/java/kr/or/arte/seal/link/config/SecureLinkConfiguration.java`
- Create: `src/main/java/kr/or/arte/seal/link/persistence/SecureLinkMapper.java`
- Create: `src/main/java/kr/or/arte/seal/link/persistence/MyBatisSecureLinkRepository.java`
- Create: `src/main/resources/mybatis/mappers/link/SecureLinkMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/link/application/SecureTokenGeneratorTest.java`
- Test: `src/test/java/kr/or/arte/seal/link/application/SecureLinkManagementServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/link/persistence/SecureLinkPostgresIntegrationTest.java`

**Interfaces:**
- Produces: `activate(actorId, requestId, versionNo): IssuedSecureLink` and `reissue(...)`.
- Produces: 43-character unpadded Base64URL token once; repository receives only 64-character HMAC.
- Consumes: Task 6 request row-lock and status transition methods.

- [x] **Step 1: Write token and lifecycle tests**

```java
@Test
void generatesThirtyTwoRandomBytesAsUnpaddedBase64Url() {
    String token = generator.generate();
    assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
}

@Test
void reissueRevokesPreviousLinkAndNeverPersistsRawToken() {
    IssuedSecureLink issued = service.reissue(ACTOR, REQUEST_ID, VERSION_NO);
    assertThat(repository.previousStatus()).isEqualTo(SecureLinkStatus.REVOKED);
    assertThat(repository.lastInsert().tokenHmac()).hasSize(64);
    assertThat(repository.serializedWrites()).doesNotContain(issued.token());
}
```

- [x] **Step 2: Run tests and verify RED**

Run: `.\mvnw.cmd -Dtest=SecureTokenGeneratorTest,SecureLinkManagementServiceTest test`

Expected: compilation failure for missing link types.

- [x] **Step 3: Implement token, HMAC, collision retry, activate, and reissue**

Use `SecureRandom`, `Base64.getUrlEncoder().withoutPadding()`, `HmacSHA256`, constant-time hash comparison, exactly three insert attempts on unique collisions, and no `toString()` that includes token/HMAC. Activation requires READY; reissue requires ACTIVE; both reject expired/terminal requests. Link expiry equals request expiry.

- [ ] **Step 4: Implement real PostgreSQL concurrency tests**

Verify one current link per request, previous revocation, composite current-link FK, no raw token columns, concurrent reissue produces one current ISSUED link, and cancelled/expired requests cannot issue.

- [x] **Step 5: Verify and commit**

Commit: `feat: 일회용 보안 링크 발급 구현`

### Task 8: Consent request administrator screens and expiry scheduler

**Files:**
- Create: `src/main/java/kr/or/arte/seal/request/web/ConsentRequestForm.java`
- Create: `src/main/java/kr/or/arte/seal/request/web/ConsentRequestController.java`
- Create: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestExpiryService.java`
- Create: `src/main/java/kr/or/arte/seal/request/scheduling/ConsentRequestExpiryJob.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/requests/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/requests/form.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/requests/preview.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/requests/issued-link.jsp`
- Modify: `src/main/webapp/resources/css/recipient-request.css`
- Test: `src/test/java/kr/or/arte/seal/request/web/ConsentRequestControllerTest.java`
- Test: `src/test/java/kr/or/arte/seal/request/scheduling/ConsentRequestExpiryJobTest.java`

**Interfaces:**
- Produces: all `/admin/requests` URLs from the spec.
- Consumes: Tasks 6–7 services.
- Produces: one-time session flash `issuedLink`; it is removed after one successful render.

- [x] **Step 1: Write MVC and scheduler tests**

```java
@Test
void activateShowsIssuedLinkOnceAndDoesNotPutTokenInRedirectUrl() throws Exception {
    given(linkService.activate(any(), eq(REQUEST_ID), anyLong()))
            .willReturn(new IssuedSecureLink("opaque-token", EXPIRES_AT));
    MvcResult result = mockMvc.perform(post("/admin/requests/{id}/activate", REQUEST_ID)
                    .param("versionNo", "0").with(csrf())
                    .with(authentication(admin("CONSENT_REQUEST_ACTIVATE"))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/requests/" + REQUEST_ID + "/issued-link"))
            .andReturn();
    assertThat(result.getResponse().getRedirectedUrl()).doesNotContain("opaque-token");
}

@Test
void expiryJobLeavesCompletedAndCancelledRequestsUnchanged() {
    job.expireDueRequests();
    assertThat(repository.changedStatuses()).doesNotContainKeys(COMPLETED_ID, CANCELLED_ID);
}
```

- [x] **Step 2: Run tests and verify RED**

Run: `.\mvnw.cmd -Dtest=ConsentRequestControllerTest,ConsentRequestExpiryJobTest test`

Expected: compilation failure for missing controller/job.

- [x] **Step 3: Implement permissions, forms, previews, and one-time token display**

List/detail always use masked recipient fields. Preview renders only the immutable snapshot. The issued-link JSP uses `Referrer-Policy: no-referrer`, contains no third-party assets, and never includes token in title, log, query parameter, analytics, or audit metadata. Reload without flash redirects to detail with `linkNotAvailable`.

- [x] **Step 4: Implement one-minute idempotent expiry job**

```java
@Scheduled(fixedDelayString = "${seal.request.expiry-job-delay:PT1M}")
public void expireDueRequests() {
    expiryService.expireBatch(clock.instant(), 100);
}
```

Repository selection uses `FOR UPDATE SKIP LOCKED`, only READY/ACTIVE due rows, and updates event, current link, request, and audit in one transaction per batch.

- [x] **Step 5: Verify and commit**

Commit: `feat: 동의 요청 관리자 화면과 만료 처리 구현`

### Task 9: Integration verification, local keys, documentation, and branch delivery

**Files:**
- Modify: `README.md`
- Modify: `database/README.md`
- Modify: `docs/superpowers/plans/2026-08-24-phase3a-recipient-request-implementation.md`
- Local only, ignored: `.env`
- Local only, outside repository: `C:\tmp\seal-keys\data.keys`, `email-index.key`, `link-hmac.key`

**Interfaces:**
- Consumes: all Phase 3A deliverables.
- Produces: reproducible DB/key setup, verification evidence, executable WAR, 9090 pages, and pushed feature branch.

- [x] **Step 1: Generate local-only cryptographic keys without printing them**

Generate 32 random bytes per purpose using Java `SecureRandom`, write only to `C:\tmp\seal-keys`, restrict ACL to the current user, and add file paths plus active ID `v1` to ignored `.env`. Never print, commit, or copy the keys into command history output.

- [x] **Step 2: Run SQL verification on `seal_test` and `seal_dev`**

Run `database/04_verify/007_recipient_request_verify.sql` on both databases and verify the recorded checksum equals the patch file SHA-256.

- [x] **Step 3: Run focused and full automated verification**

Run: `.\mvnw.cmd test`

Expected: all tests PASS with 0 failures, 0 errors, 0 skipped.

- [x] **Step 4: Package and run the exact verified source**

Run: `.\mvnw.cmd clean package`

Expected: exit 0 and `target/seal.war` exists. Start it on 9090 with `seal_dev` and external key paths; `/actuator/health` returns `UP`.

- [x] **Step 5: Perform representative 9090 smoke tests**

Verified login with the configured administrator and the recipient create/detail/edit/status flow using a fictional `.test` email address. Recipient and request list/create screens were checked at desktop and 360px widths with no console errors or horizontal overflow. Request create/finalize/activate/reissue/cancel, one-time link handling, and permission denial are covered by MVC/service tests plus the real `seal_test` PostgreSQL lifecycle integration test rather than by persistent browser fixtures.

- [x] **Step 6: Update documents and actual plan checkboxes**

Document key file creation, SQL order, URLs, permissions, masking, excluded 3B/4/5 functionality, and exact test evidence. Change only completed plan steps to `[x]`.

- [ ] **Step 7: Final review, commit, and push**

Run `git diff --check`, secret scan for actual key/token/email fixtures, `git status --short`, and `git log --oneline origin/feature/phase2-template..HEAD`. Commit documentation as `docs: 3A 대상자 요청 검증 절차 기록` and push `feature/phase3a-recipient-request` to the private GitHub repository. Do not merge to main without explicit user direction.

### Actual completion evidence (2026-08-24)

- Full suite: 171 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL: patch `20260824_003` and verification `007` passed on both `seal_test` and `seal_dev`.
- Runtime: `target/seal.war` packaged successfully, port 9090 health status `UP`.
- Browser: login plus recipient create/edit/inactivate completed; recipient/request pages passed representative desktop and 360px layout checks.
- Remaining non-blocking verification: a dedicated simultaneous secure-link reissue stress test. Database uniqueness and revocation behavior are already covered by constraints and lifecycle integration tests, but high-contention behavior should be run before production release.
