# 처리제한·링크·공개 세션 보강 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 처리제한 대상자의 요청·링크 발급 우회를 차단하고, 서로 다른 법적 보존 사유의 공존과 공개 동의 세션의 단일 활성·만료 HMAC 파기를 보장한다.

**Architecture:** 처리제한 검사를 요청 관리와 링크 발급의 도메인 경계에 중복 적용해 모든 진입점을 보호한다. `legal_hold`의 부분 유일 인덱스는 요청·사유 단위로 변경하고, 공개 동의 세션은 링크 잠금 트랜잭션에서 이전 활성 세션을 만료한 뒤 생성하며 접근 시 정리와 제한된 배치 정리를 함께 제공한다.

**Tech Stack:** Java 17, Spring Boot MVC/Security/Scheduling, JSP, MyBatis, PostgreSQL 16+, JUnit 5, AssertJ, Mockito, Spring MockMvc

**Spec:** `docs/superpowers/specs/2026-08-25-restriction-link-session-hardening-design.md`

## Global Constraints

- 전자서명, SMS, OTP, 휴대전화/PASS 본인확인, 이메일 열람 추적을 추가하지 않는다.
- Flyway와 Liquibase를 사용하지 않고 `database/03_patch` 수동 SQL과 SHA-256 변경 이력을 사용한다.
- 적용된 기존 SQL은 수정하지 않고 신규 패치와 대응 검증 SQL만 추가한다.
- `seal_owner`가 객체를 소유하고 `seal_app`의 기존 최소권한을 확대하지 않는다.
- 운영 Rocky Linux 반영, 백업, 인증서와 운영 SMTP 검증은 이번 범위에서 제외한다.
- 기존 작업트리의 미커밋 수정사항을 보존하고 각 태스크의 파일만 선택적으로 커밋한다.

---

### Task 1: 법적 보존과 처리제한 보류의 공존

**Files:**
- Create: `database/03_patch/20260825_010_allow_legal_hold_reasons.sql`
- Create: `database/04_verify/014_legal_hold_reasons_verify.sql`
- Modify: `src/main/resources/mybatis/mappers/retention/RetentionMapper.xml:11`
- Modify: `src/test/java/kr/or/arte/seal/retention/persistence/RetentionPostgresIntegrationTest.java`
- Modify: `database/03_patch/README.md`
- Modify: `database/README.md`
- Modify: `.github/workflows/verify.yml:82-94`

**Interfaces:**
- Consumes: `RetentionRepository.placeLegalHold(...)`, `RetentionRepository.placeRestrictionHold(...)`
- Produces: 활성 부분 유일키 `(consent_request_id, hold_reason_code)` 및 동일 사유 멱등 UPSERT

- [ ] **Step 1: 기존 감사 보존과 처리제한이 함께 유지되어야 하는 실패 통합 테스트 작성**

`RetentionPostgresIntegrationTest`에 실제 `RetentionJobService`와 PostgreSQL을 사용하는 테스트를 추가한다. 기대값은 코드가 아니라 리터럴 `2`, `1`, `1`로 검증한다.

```java
@Test
void keepsAuditAndRightsRestrictionHoldsActiveAtTheSameTime() {
    UUID actor = id("SELECT admin_user_id FROM seal.admin_user WHERE login_id='seal-admin'");
    UUID org = id("SELECT organization_unit_id FROM seal.organization_unit WHERE root_unit=TRUE");
    Fixture fixture = publishedFixture(org, fixtureCreator(), actor);
    UUID recipient = recipients.create(new CreateRecipientCommand(actor, org,
            "보존 공존 대상자", "hold-" + UUID.randomUUID() + "@example.com", "공존 검증"));
    var request = requests.create(actor, new CreateConsentRequestCommand(org, recipient,
            fixture.templateVersionId(), Instant.now().plusSeconds(7200), UUID.randomUUID()));

    retention.placeLegalHold(actor, request.requestId(), "AUDIT", "감사 대응",
            LocalDate.now().plusDays(30));
    retention.placeRestrictionHold(request.requestId(), actor, Instant.now());
    retention.placeRestrictionHold(request.requestId(), actor, Instant.now());

    assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM seal.legal_hold
             WHERE consent_request_id=? AND active
            """, Integer.class, request.requestId())).isEqualTo(2);
    assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM seal.legal_hold
             WHERE consent_request_id=? AND active AND hold_reason_code='AUDIT'
            """, Integer.class, request.requestId())).isOne();
    assertThat(jdbc.queryForObject("""
            SELECT count(*) FROM seal.legal_hold
             WHERE consent_request_id=? AND active AND hold_reason_code='RIGHTS_RESTRICTION'
            """, Integer.class, request.requestId())).isOne();
}
```

- [ ] **Step 2: 테스트를 실행해 기존 단일 요청 인덱스 때문에 실패하는지 확인**

Run:

```powershell
.\mvnw.cmd -Dtest=RetentionPostgresIntegrationTest#keepsAuditAndRightsRestrictionHoldsActiveAtTheSameTime test
```

Expected: FAIL. 두 번째 사유가 별도 활성 행으로 생성되지 않아 활성 행 수가 `1`이다.

- [ ] **Step 3: 신규 불변 DB 패치 작성**

`20260825_010_allow_legal_hold_reasons.sql`은 기존 형식대로 `script_checksum`, `app_version`, `target_database`를 받고 다음 DDL과 변경 이력을 하나의 트랜잭션으로 적용한다.

```sql
DROP INDEX seal.uq_active_legal_hold;
CREATE UNIQUE INDEX uq_active_legal_hold_reason
  ON legal_hold(consent_request_id, hold_reason_code)
  WHERE active;

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260825_010_allow_legal_hold_reasons.sql', :'script_checksum', :'app_version',
       '동의 요청별 서로 다른 활성 보존 보류 사유의 공존 허용');
```

패치 앞에는 기존과 동일한 `\set ON_ERROR_STOP on`, 변수 검증, `\connect`, `BEGIN`, `SET LOCAL ROLE seal_owner`, `SET LOCAL search_path = seal, public`을 포함하고 마지막에 `COMMIT`한다.

- [ ] **Step 4: 처리제한 UPSERT의 충돌 대상을 요청·사유로 변경**

`RetentionMapper.xml`의 `restrictionHold`을 다음 의미로 변경한다.

```sql
ON CONFLICT (consent_request_id, hold_reason_code) WHERE active
DO UPDATE SET active=EXCLUDED.active
```

다른 사유 행의 `hold_reason_code`, `reason_text`, `expires_on`, 행위자와 시각은 변경하지 않는다.

- [ ] **Step 5: 신규 DB 계약 검증 SQL 작성**

`014_legal_hold_reasons_verify.sql`은 다음을 실행으로 검증한다.

```sql
IF NOT EXISTS (
  SELECT 1 FROM pg_indexes
   WHERE schemaname='seal' AND tablename='legal_hold'
     AND indexname='uq_active_legal_hold_reason'
     AND indexdef LIKE '%(consent_request_id, hold_reason_code)%'
     AND indexdef LIKE '%WHERE active%'
) THEN
  RAISE EXCEPTION 'active legal hold reason unique index is missing';
END IF;

IF EXISTS (
  SELECT 1 FROM pg_indexes
   WHERE schemaname='seal' AND indexname='uq_active_legal_hold'
) THEN
  RAISE EXCEPTION 'legacy single active legal hold index still exists';
END IF;
```

마지막에 `db_change_history`의 `20260825_010` 체크섬이 `expected_checksum`과 같은지 확인하고 실패 시 종료 코드 `4`로 끝낸다.

- [ ] **Step 6: CI와 DB 문서에 010/014 매핑 추가**

`.github/workflows/verify.yml`의 검증 매핑에 다음을 추가한다.

```yaml
014_*) source=database/03_patch/20260825_010_allow_legal_hold_reasons.sql ;;
```

두 README의 적용 순서에 13번째 패치와 `014` 검증 명령을 추가한다.

- [ ] **Step 7: 패치를 로컬 테스트 DB에 적용하고 RED 테스트와 검증 SQL을 GREEN으로 확인**

SHA-256을 계산해 비밀번호를 출력하지 않고 패치와 검증을 실행한다.

```powershell
$env:PGPASSWORD=(Get-Content -Raw 'C:\tmp\postgres-local-password.txt').Trim()
$checksum=(Get-FileHash database/03_patch/20260825_010_allow_legal_hold_reasons.sql -Algorithm SHA256).Hash.ToLower()
psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -X -v ON_ERROR_STOP=1 -v target_database=seal_test -v script_checksum=$checksum -v app_version=local -f database/03_patch/20260825_010_allow_legal_hold_reasons.sql
psql -h 127.0.0.1 -p 5432 -U postgres -d postgres -X -v ON_ERROR_STOP=1 -v target_database=seal_test -v expected_checksum=$checksum -f database/04_verify/014_legal_hold_reasons_verify.sql
.\mvnw.cmd -Dtest=RetentionPostgresIntegrationTest#keepsAuditAndRightsRestrictionHoldsActiveAtTheSameTime test
Remove-Item Env:PGPASSWORD
```

Expected: 검증 SQL에 `Legal hold reason verification passed`, 테스트 1개 PASS.

- [ ] **Step 8: Task 1 파일만 커밋**

```powershell
git add database/03_patch/20260825_010_allow_legal_hold_reasons.sql database/04_verify/014_legal_hold_reasons_verify.sql database/03_patch/README.md database/README.md .github/workflows/verify.yml src/main/resources/mybatis/mappers/retention/RetentionMapper.xml src/test/java/kr/or/arte/seal/retention/persistence/RetentionPostgresIntegrationTest.java
git commit -m "fix: allow concurrent legal hold reasons"
```

---

### Task 2: 모든 링크 발급 경로의 처리제한 강제

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/link/application/SecureLinkManagementService.java`
- Modify: `src/main/java/kr/or/arte/seal/request/web/ConsentRequestController.java`
- Modify: `src/test/java/kr/or/arte/seal/request/web/ConsentRequestControllerTest.java`
- Create: `src/test/java/kr/or/arte/seal/link/application/SecureLinkRestrictionTest.java`
- Modify: 링크 서비스를 직접 생성하는 기존 단위 테스트

**Interfaces:**
- Consumes: `ConsentRestrictionQuery.hasActiveRestriction(UUID, LocalDate)`
- Produces: 처리제한을 강제하는 `activate(UUID, UUID, long)`와 `reissue(UUID, UUID, long)`

- [ ] **Step 1: 링크 핵심 서비스의 발급·재발급 거부 실패 테스트 작성**

`SecureLinkRestrictionTest`에서 실제 `SecureLinkManagementService`를 생성한다. 요청 저장소는 `READY`, `ACTIVE` 레코드를 순서대로 반환하고 제한 조회는 `true`를 반환한다.

```java
assertThatThrownBy(() -> service.activate(ACTOR, REQUEST, 0))
        .isInstanceOf(RequestBusinessConflictException.class)
        .hasMessageContaining("처리제한");
assertThatThrownBy(() -> service.reissue(ACTOR, REQUEST, 0))
        .isInstanceOf(RequestBusinessConflictException.class)
        .hasMessageContaining("처리제한");
verifyNoInteractions(links);
```

이 테스트가 방지하는 변경은 링크 서비스에서 처리제한 검사를 제거하거나 발급 mutation 뒤로 이동하는 것이다.

- [ ] **Step 2: 테스트를 실행해 링크 저장소가 호출되는 기존 동작으로 실패하는지 확인**

```powershell
.\mvnw.cmd -Dtest=SecureLinkRestrictionTest test
```

Expected: FAIL. 현재 생성자에 `ConsentRestrictionQuery`가 없거나 `links.insert/activateRequest/revokeCurrent`가 호출된다.

- [ ] **Step 3: 링크 서비스 생성자와 선행 정책 검사를 최소 구현**

`SecureLinkManagementService`에 `ConsentRestrictionQuery restrictions`를 주입하고 다음 헬퍼를 추가한다.

```java
private void requireNotRestricted(UUID recipientId) {
    LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
    if (restrictions.hasActiveRestriction(recipientId, today)) {
        throw new RequestBusinessConflictException("처리제한 중인 대상자의 링크를 발급할 수 없습니다.");
    }
}
```

`activate()`와 `reissue()`에서 범위·버전·상태·만료 검사를 마친 후 링크 변경 전에 `requireNotRestricted(request.recipientId())`를 호출한다. 기존 생성자 호출 테스트에는 제한 조회 fake/mock을 전달한다.

- [ ] **Step 4: 직접 API가 노출되지 않아야 하는 MVC 실패 테스트 작성**

기존 `activateShowsIssuedLinkWithoutTokenInRedirectUrl`을 다음 계약 테스트 두 개로 교체한다.

```java
@Test
void directActivateEndpointIsNotExposed() throws Exception {
    mvc.perform(post("/admin/requests/{id}/activate", REQUEST)
            .param("versionNo", "1").with(csrf())
            .with(authentication(admin("CONSENT_REQUEST_ACTIVATE"))))
       .andExpect(status().isNotFound());
}

@Test
void directReissueEndpointIsNotExposed() throws Exception {
    mvc.perform(post("/admin/requests/{id}/reissue", REQUEST)
            .param("versionNo", "1").with(csrf())
            .with(authentication(admin("CONSENT_REQUEST_LINK_REISSUE"))))
       .andExpect(status().isNotFound());
}
```

- [ ] **Step 5: MVC 테스트가 기존 매핑 때문에 실패하는지 확인**

```powershell
.\mvnw.cmd -Dtest=ConsentRequestControllerTest test
```

Expected: FAIL. 기존 컨트롤러가 3xx 응답을 반환한다.

- [ ] **Step 6: 직접 컨트롤러 메서드와 불필요한 의존성 제거**

`activate()`와 `reissue()` 매핑을 삭제한다. 컨트롤러에서 다른 용도로 사용하지 않는다면 `SecureLinkManagementService links` 필드와 생성자 인자를 제거한다. `/deliver`, `/redeliver`, `/issued-link`는 유지한다.

- [ ] **Step 7: 링크와 MVC 집중 테스트를 GREEN으로 확인**

```powershell
.\mvnw.cmd -Dtest=SecureLinkRestrictionTest,ConsentRequestControllerTest,ConsentRestrictionEnforcementTest test
```

Expected: 모든 테스트 PASS, 직접 링크 mutation 상호작용 없음.

- [ ] **Step 8: Task 2 파일만 커밋**

```powershell
git add src/main/java/kr/or/arte/seal/link/application/SecureLinkManagementService.java src/main/java/kr/or/arte/seal/request/web/ConsentRequestController.java src/test/java/kr/or/arte/seal/link/application/SecureLinkRestrictionTest.java src/test/java/kr/or/arte/seal/request/web/ConsentRequestControllerTest.java src/test/java
git commit -m "fix: enforce restrictions at link issuance boundary"
```

`git add src/test/java` 대신 실제로 생성자가 변경된 테스트 파일 목록을 `git status --short`로 확인해 선택적으로 추가한다.

---

### Task 3: 초안 수정과 최종 확정의 처리제한 재검사

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagementService.java`
- Modify: `src/test/java/kr/or/arte/seal/request/application/ConsentRestrictionEnforcementTest.java`
- Modify: `src/test/java/kr/or/arte/seal/request/application/ConsentRequestManagementServiceTest.java`

**Interfaces:**
- Consumes: 기존 `ConsentRestrictionQuery`
- Produces: 생성·수정·확정에 동일하게 적용되는 내부 `requireNotRestricted(UUID)` 정책

- [ ] **Step 1: 제한 대상자로의 초안 변경을 거부하는 실패 테스트 작성**

```java
@Test
void activeRestrictionBlocksDraftRecipientChangeBeforeTemplateLookup() {
    given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(Optional.of(request(DRAFT)));
    given(recipients.findScoped(ACTOR, RECIPIENT)).willReturn(Optional.of(recipient()));
    given(restrictions.hasActiveRestriction(eq(RECIPIENT), any(LocalDate.class))).willReturn(true);

    assertThatThrownBy(() -> service.updateDraft(ACTOR, REQUEST,
            new UpdateConsentRequestCommand(RECIPIENT, TEMPLATE_VERSION,
                    NOW.plusSeconds(3600), 0)))
            .isInstanceOf(RequestBusinessConflictException.class)
            .hasMessageContaining("처리제한");
    verifyNoInteractions(templates);
}
```

- [ ] **Step 2: 기존 초안이 제한된 뒤 확정되는 것을 거부하는 실패 테스트 작성**

```java
@Test
void activeRestrictionBlocksFinalizationBeforeSnapshotCreation() {
    given(requests.findScopedForUpdate(ACTOR, REQUEST)).willReturn(Optional.of(request(DRAFT)));
    given(restrictions.hasActiveRestriction(eq(RECIPIENT), any(LocalDate.class))).willReturn(true);

    assertThatThrownBy(() -> service.finalizeRequest(ACTOR, REQUEST, 0))
            .isInstanceOf(RequestBusinessConflictException.class)
            .hasMessageContaining("처리제한");
    verifyNoInteractions(templates, snapshots);
}
```

두 테스트의 기대 동작은 정책 경계이며, mock 호출 자체가 아니라 예외와 snapshot/template 부작용 부재를 검증한다.

- [ ] **Step 3: 두 테스트가 현재 코드에서 실패하는지 확인**

```powershell
.\mvnw.cmd -Dtest=ConsentRestrictionEnforcementTest test
```

Expected: FAIL. `updateDraft`와 `finalizeRequest`가 제한 예외 없이 다음 단계로 진행한다.

- [ ] **Step 4: 중복 제한 검사를 헬퍼로 통합하고 두 경로에 적용**

`ConsentRequestManagementService`에 다음 내부 메서드를 두고 기존 `create()`의 인라인 검사도 이 메서드로 교체한다.

```java
private void requireNotRestricted(UUID recipientId) {
    LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
    if (restrictions.hasActiveRestriction(recipientId, today)) {
        throw new RequestBusinessConflictException(
                "처리제한 중인 대상자에게 동의 요청을 생성하거나 확정할 수 없습니다.");
    }
}
```

- `updateDraft`: 대상자 범위·활성 상태 확인 직후, 템플릿 조회 전에 호출
- `finalizeRequest`: 잠근 초안 레코드의 상태가 `DRAFT`인지 확인한 직후, 템플릿과 snapshot 조회 전에 호출

- [ ] **Step 5: 요청 관리 전체 단위 테스트를 GREEN으로 확인**

```powershell
.\mvnw.cmd -Dtest=ConsentRestrictionEnforcementTest,ConsentRequestManagementServiceTest test
```

Expected: 모든 테스트 PASS. 기존 생성, 수정, 확정, 취소 회귀 없음.

- [ ] **Step 6: Task 3 파일만 커밋**

```powershell
git add src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagementService.java src/test/java/kr/or/arte/seal/request/application/ConsentRestrictionEnforcementTest.java src/test/java/kr/or/arte/seal/request/application/ConsentRequestManagementServiceTest.java
git commit -m "fix: recheck restrictions during request editing"
```

---

### Task 4: 공개 동의 세션 단일 활성·접근 시 만료·배치 정리

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/consent/application/PublicConsentRepository.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/application/PublicConsentEntryService.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/application/PublicConsentQueryService.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/application/ConsentSubmissionService.java`
- Create: `src/main/java/kr/or/arte/seal/consent/application/PublicConsentSessionExpiryService.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/persistence/PublicConsentMapper.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/persistence/MyBatisPublicConsentRepository.java`
- Modify: `src/main/resources/mybatis/mappers/consent/PublicConsentMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/consent/scheduling/PublicConsentSessionExpiryJob.java`
- Modify: `src/test/java/kr/or/arte/seal/consent/application/PublicConsentEntryServiceTest.java`
- Modify: `src/test/java/kr/or/arte/seal/consent/application/PublicConsentQueryServiceTest.java`
- Modify: `src/test/java/kr/or/arte/seal/consent/application/ConsentSubmissionServiceTest.java`
- Create: `src/test/java/kr/or/arte/seal/consent/application/PublicConsentSessionExpiryServiceTest.java`
- Create: `src/test/java/kr/or/arte/seal/consent/scheduling/PublicConsentSessionExpiryJobTest.java`
- Modify: `src/test/java/kr/or/arte/seal/request/persistence/ConsentRequestPostgresIntegrationTest.java`

**Interfaces:**
- Produces: `int expireSession(UUID sessionId, Instant now)`, `int expireDueSessions(Instant now, int limit)`
- Consumes: 기존 요청 단위 `expireSessions(UUID, Instant)`와 링크 잠금 조회

- [ ] **Step 1: 새 세션 시작이 기존 활성 세션을 먼저 만료해야 하는 실패 테스트 작성**

`PublicConsentEntryServiceTest`의 저장소 fake가 `expireSessions` 호출과 삽입 순서를 기록하게 하고 다음을 검증한다.

```java
PublicConsentFlow flow = service.start(validToken);

assertThat(repository.operations).containsExactly(
        "expire:" + REQUEST,
        "insert:" + REQUEST);
assertThat(flow.requestId()).isEqualTo(REQUEST);
```

실패를 유발해야 하는 변경은 `start()`에서 기존 활성 세션 만료를 생략하거나 새 세션 뒤에 수행하는 것이다.

- [ ] **Step 2: 만료 조회와 제출이 상태·HMAC 파기를 요청하는 실패 테스트 작성**

`PublicConsentQueryServiceTest`와 `ConsentSubmissionServiceTest`에 `expiresAt = NOW.minusSeconds(1)`, `sessionStatus = ACTIVE`인 실제 view를 반환하고 다음을 검증한다.

```java
assertThatThrownBy(() -> query.load(flow))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("만료");
assertThat(repository.expiredSessionIds).containsExactly(SESSION);
```

```java
assertThatThrownBy(() -> submission.submit(flow, decisions, UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("만료");
assertThat(repository.expiredSessionIds).containsExactly(SESSION);
```

- [ ] **Step 3: 배치 서비스 한도와 스케줄 위임 실패 테스트 작성**

```java
@Test
void expiresDueSessionsWithValidatedLimit() {
    given(repository.expireDueSessions(NOW, 100)).willReturn(3);
    assertThat(new PublicConsentSessionExpiryService(repository, CLOCK).expireDue(100)).isEqualTo(3);
    assertThatThrownBy(() -> service.expireDue(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.expireDue(1001)).isInstanceOf(IllegalArgumentException.class);
}
```

```java
@Test
void scheduledJobExpiresOneBatch() {
    var service = mock(PublicConsentSessionExpiryService.class);
    new PublicConsentSessionExpiryJob(service).expireDueSessions();
    verify(service).expireDue(100);
}
```

- [ ] **Step 4: 집중 테스트를 실행해 새 수명주기 계약이 아직 없어 실패하는지 확인**

```powershell
.\mvnw.cmd -Dtest=PublicConsentEntryServiceTest,PublicConsentQueryServiceTest,ConsentSubmissionServiceTest,PublicConsentSessionExpiryServiceTest,PublicConsentSessionExpiryJobTest test
```

Expected: FAIL. 기존 세션 만료 순서와 `expireSession`, `expireDueSessions`가 구현되어 있지 않다.

- [ ] **Step 5: 저장소와 MyBatis 인터페이스를 최소 확장**

`PublicConsentRepository`와 구현체에 다음 메서드를 추가한다.

```java
int expireSession(UUID sessionId, Instant now);
int expireDueSessions(Instant now, int limit);
```

`PublicConsentMapper.xml`은 개별 만료와 제한 배치를 다음 조건으로 구현한다.

```sql
UPDATE seal.public_consent_session
SET session_status='EXPIRED', flow_hmac=NULL, flow_destroyed_at=#{now}
WHERE public_consent_session_id=#{sessionId}
  AND session_status='ACTIVE' AND expires_at&lt;=#{now}
```

```sql
WITH picked AS (
  SELECT public_consent_session_id
    FROM seal.public_consent_session
   WHERE session_status='ACTIVE' AND expires_at&lt;=#{now}
   ORDER BY expires_at
   LIMIT #{limit}
   FOR UPDATE SKIP LOCKED
)
UPDATE seal.public_consent_session s
   SET session_status='EXPIRED', flow_hmac=NULL, flow_destroyed_at=#{now}
  FROM picked
 WHERE s.public_consent_session_id=picked.public_consent_session_id
```

- [ ] **Step 6: 세션 생성과 접근 시 만료 반영 구현**

- `PublicConsentEntryService.start`: 보안 링크를 잠근 뒤 `repository.expireSessions(row.requestId(), now)`를 호출하고 새 세션을 삽입한다.
- `PublicConsentQueryService.load`: `@Transactional`로 변경하고 세션을 잠금 조회한다. 만료면 `expireSession(sessionId, now)` 호출 후 만료 예외를 발생시킨다.
- `ConsentSubmissionService.submit`: 활성/만료 검사에서 `expiresAt <= now`이면 `expireSession(sessionId, now)`을 호출한 뒤 만료 예외를 발생시킨다.
- 완료 세션의 동일 멱등키 재시도 반환은 기존 순서를 유지한다.

- [ ] **Step 7: 배치 서비스와 스케줄러 구현**

```java
@Service
public class PublicConsentSessionExpiryService {
    private final PublicConsentRepository repository;
    private final Clock clock;
    @Transactional
    public int expireDue(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("세션 만료 배치 크기는 1~1000이어야 합니다.");
        }
        return repository.expireDueSessions(clock.instant(), limit);
    }
}
```

```java
@Component
@ConditionalOnProperty(prefix="seal.public-session-expiry", name="enabled",
        havingValue="true", matchIfMissing=true)
public class PublicConsentSessionExpiryJob {
    @Scheduled(fixedDelayString="${seal.public-session-expiry.fixed-delay:PT10M}")
    public void expireDueSessions() { service.expireDue(100); }
}
```

조건부 빈 테스트는 `ApplicationContextRunner`로 `enabled=false`일 때 빈이 없고 기본 설정에서 빈이 있음을 검증한다.

- [ ] **Step 8: 단위 테스트를 GREEN으로 확인**

```powershell
.\mvnw.cmd -Dtest=PublicConsentEntryServiceTest,PublicConsentQueryServiceTest,ConsentSubmissionServiceTest,PublicConsentSessionExpiryServiceTest,PublicConsentSessionExpiryJobTest test
```

Expected: 모든 테스트 PASS.

- [ ] **Step 9: 실제 PostgreSQL에서 반복 시작과 배치 만료를 검증하는 통합 테스트 작성**

`ConsentRequestPostgresIntegrationTest`에 다음 결과를 검증한다.

```java
var first = publicEntry.start(issued.issuedLink().token());
var second = publicEntry.start(issued.issuedLink().token());

assertThat(jdbc.queryForObject("""
        SELECT count(*) FROM seal.public_consent_session
         WHERE consent_request_id=? AND session_status='ACTIVE'
        """, Integer.class, request.requestId())).isOne();
assertThat(jdbc.queryForObject("""
        SELECT count(*) FROM seal.public_consent_session
         WHERE consent_request_id=? AND session_status='EXPIRED'
           AND flow_hmac IS NULL AND flow_destroyed_at IS NOT NULL
        """, Integer.class, request.requestId())).isOne();
```

별도 만료 fixture의 `expires_at`을 과거로 갱신한 뒤 `sessionExpiry.expireDue(100)`의 반환값과 `EXPIRED`, `flow_hmac IS NULL`, `flow_destroyed_at IS NOT NULL`을 검증한다.

- [ ] **Step 10: PostgreSQL 통합 테스트를 GREEN으로 확인**

```powershell
.\mvnw.cmd -Dtest=ConsentRequestPostgresIntegrationTest test
```

Expected: 반복 시작과 배치 정리 시나리오 포함 전체 PASS.

- [ ] **Step 11: Task 4 파일만 커밋**

```powershell
git add src/main/java/kr/or/arte/seal/consent src/main/resources/mybatis/mappers/consent/PublicConsentMapper.xml src/test/java/kr/or/arte/seal/consent src/test/java/kr/or/arte/seal/request/persistence/ConsentRequestPostgresIntegrationTest.java
git commit -m "fix: expire abandoned public consent sessions"
```

---

### Task 5: 전체 회귀 검증과 배포 전 상태 확인

**Files:**
- Modify only if verification exposes a regression directly caused by Tasks 1-4
- Review: `docs/superpowers/specs/2026-08-25-restriction-link-session-hardening-design.md`
- Review: `docs/superpowers/plans/2026-08-25-restriction-link-session-hardening-implementation.md`

**Interfaces:**
- Consumes: Tasks 1-4의 exact commit tree
- Produces: 전체 테스트·DB 계약·WAR 빌드 검증 증거

- [ ] **Step 1: 신규 DB 검증 SQL을 로컬 `seal_dev`에도 같은 체크섬으로 적용**

`seal_test`에서 통과한 정확한 파일 SHA-256을 다시 계산해 `seal_dev`에 적용한다. 이미 동일 체크섬 이력이 있으면 재적용하지 않고 이력을 확인한다.

- [ ] **Step 2: 모든 DB 계약 검증 SQL 실행**

기존 `001`~`014`를 순서대로 실행하며 체크섬이 필요한 검증에는 `.github/workflows/verify.yml`과 동일한 원본 패치 체크섬을 전달한다.

Expected: 모든 스크립트 종료 코드 `0`, 마지막 신규 검증은 `Legal hold reason verification passed`.

- [ ] **Step 3: 전체 Maven 검증을 한 번 실행**

```powershell
.\mvnw.cmd verify
```

Expected: exit code `0`, failures `0`, errors `0`, `target/seal.war` 생성.

- [ ] **Step 4: 변경 품질과 범위 확인**

```powershell
git diff --check HEAD~4..HEAD
git status --short --branch
git log --oneline -5
Get-Item target/seal.war | Select-Object FullName,Length,LastWriteTime
```

Expected: 공백 오류 없음. 사용자 기존 변경을 제외한 신규 태스크 파일이 모두 커밋되어 있고 예상하지 않은 파일이 포함되지 않는다.

- [ ] **Step 5: 설계 완료 기준 대조**

- 생성·수정·확정·최초 발급·재발급이 처리제한을 차단하는 테스트가 각각 존재한다.
- `AUDIT`와 `RIGHTS_RESTRICTION` 활성 보류가 실제 PostgreSQL에서 공존한다.
- 공개 요청당 활성 세션이 하나이며 만료 세션 HMAC이 접근 시와 배치에서 파기된다.
- 이메일 발송, 공개 동의 제출, 증적, 권리요청, 파기 기존 테스트가 전체 검증에서 통과한다.

- [ ] **Step 6: 검증 중 직접 발생한 수정이 있으면 해당 테스트와 함께 보완 커밋**

검증에서 실패가 없으면 빈 커밋을 만들지 않는다. 실패가 있으면 원인 재현 테스트가 실패하는지 먼저 확인하고 최소 수정 후 해당 집중 테스트와 전체 `verify`를 다시 실행한다.

---

## 계획 자체 검수 결과

- 설계의 네 결함은 Task 1~4에 각각 대응한다.
- DB 변경은 신규 `010` 패치와 `014` 검증으로만 수행하며 적용된 SQL을 수정하지 않는다.
- 링크와 요청 정책은 서비스 경계에서 검사하고 직접 관리자 API를 제거한다.
- 세션은 생성, 접근, 제출, 무접근 배치의 네 수명주기 경로를 모두 다룬다.
- 각 생산 코드 변경 전에 관찰 가능한 실패 테스트와 RED 실행 단계가 있다.
- 운영 서버 반영과 외부 SMTP 검증은 포함하지 않는다.
