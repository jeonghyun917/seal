# Phase 6 Evidence, Rights, and Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 완료 동의의 무결성 증적, 관리자 권리요청 처리, 이중 통제 파기와 범위 제한 통계를 단일 WAR에 구현한다.

**Architecture:** PostgreSQL 현재 상태 행과 불변 이벤트 행을 분리하고 증적·파기 작업은 `FOR UPDATE SKIP LOCKED` 기반 내부 워커가 처리한다. 증적 본문은 설정된 로컬 루트에 원자 저장하며, 권리요청 상세는 기존 AES-256-GCM 경계를 재사용한다. 운영 서버·백업·실 SMTP·HTTPS 인수는 포함하지 않는다.

**Tech Stack:** Java 17, eGovFrame Runtime 5.0.0, Spring Boot 3.5.6, Spring MVC/JSP, Spring Security, MyBatis, PostgreSQL 16.14, OpenHTMLtoPDF 1.1.76, PDFBox 3.0.8, Noto Sans KR, JUnit 5, AssertJ, MockMvc

**Spec:** `docs/superpowers/specs/2026-08-24-phase6-evidence-rights-lifecycle-design.md`

**실행 상태:** 2026-08-24 구현 완료. `seal_test`·`seal_dev`에 `005`·`006`·`007`을 적용하고 검증 SQL `009`·`010`·`011`을 통과했다. 전체 자동화 테스트 223건과 실제 PostgreSQL 증적·권리요청·기산일/보존 보류·잔여정보 파기·통계 통합 테스트가 통과했다. Rocky Linux, 백업·복구, 운영 HTTPS·실 SMTP는 계획대로 운영 인수 범위에 남겼다.

## Global Constraints

- 기존 `consent_response`, `consent_response_item`과 불변 이벤트는 UPDATE·DELETE하지 않는다.
- 운영 서버, 백업·복구, 백업본 파기, HTTPS와 실 SMTP 검증은 제외한다.
- 증적 HMAC 키는 기존 데이터·이메일·링크 키와 분리하고 저장소 밖 파일에서 읽는다.
- 목록·통계·감사 메타데이터에 이름, 이메일, 암호문, 권리요청 메모와 증적 본문을 넣지 않는다.
- 모든 관리자 조회는 권한과 조직·처리업무 범위를 함께 검사한다.
- 도메인·상태·보안 규칙은 RED를 확인한 뒤 최소 구현한다.
- SQL 변경은 `20260824_005`, 검증은 `009`로 추가하고 기존 적용 SQL은 수정하지 않는다.

---

### Task 1: Phase 6 DB 경계와 권한

**Files:**
- Create: `database/03_patch/20260824_005_create_evidence_rights_retention_tables.sql`
- Create: `database/04_verify/009_evidence_rights_retention_verify.sql`
- Create: `src/test/java/kr/or/arte/seal/evidence/persistence/Phase6SchemaPostgresIntegrationTest.java`
- Modify: `database/README.md`

**Interfaces:**
- Consumes: 기존 `consent_response`, `consent_request`, `recipient`, `retention_policy_version`, `admin_user`, `audit_event`
- Produces: `evidence_package`, `evidence_file`, `evidence_verification`, `rights_request`, `rights_request_event`, `consent_withdrawal`, `retention_anchor_event`, `legal_hold`, `retention_job`, `retention_job_event`과 Phase 6 권한

- [ ] **Step 1: 스키마 실패 통합 테스트를 작성한다**

`Phase6SchemaPostgresIntegrationTest`에서 `to_regclass`로 열 개 테이블을 확인하고 다음 실제 DB 동작을 검증한다.

```java
assertThat(tableExists("evidence_package")).isTrue();
assertThat(tableExists("retention_job_event")).isTrue();
assertThat(canAppUpdate("evidence_file")).isFalse();
assertThat(canAppDelete("rights_request_event")).isFalse();
```

- [ ] **Step 2: RED를 확인한다**

Run:

```powershell
$env:SEAL_DB_URL='jdbc:postgresql://127.0.0.1:5432/seal_test?currentSchema=seal'
.\mvnw.cmd -Dtest=Phase6SchemaPostgresIntegrationTest test
```

Expected: `evidence_package` 부재로 FAIL.

- [ ] **Step 3: SQL 패치와 검증 SQL을 작성한다**

현재 상태 테이블에는 `row_version`, 작업에는 `idempotency_key`, 불변 이벤트에는 FK와 생성시각을 둔다. 다음 제약을 SQL CHECK/UNIQUE로 강제한다.

```sql
CREATE UNIQUE INDEX uq_active_evidence_response
    ON evidence_package (response_id)
 WHERE generation_status <> 'DESTROYED';
CHECK (generation_status IN ('QUEUED','PROCESSING','FINALIZED','FAILED','DESTROYED'));
CHECK (rights_type IN ('ACCESS','CORRECTION','ERASURE','RESTRICTION','WITHDRAWAL'));
CHECK (job_status IN ('DUE','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','CONFIRMED','FAILED','HELD','CANCELLED'));
```

`recipient`의 이름·이메일 암호문, 키 ID와 이메일 HMAC을 nullable로 바꾸고 `DESTROYED` 상태에서만 모두 NULL이 되도록 CHECK를 교체한다. Phase 6 권한 코드는 설계서의 13개를 삽입하되 역할에 자동 부여하지 않는다.

- [ ] **Step 4: seal_test·seal_dev에 적용하고 검증한다**

파일 SHA-256을 `script_checksum`으로 넘겨 패치를 적용하고 `009`를 두 DB에서 실행한다. 검증은 소유자, FK, CHECK, 부분 유일 인덱스, `seal_app` DELETE/TRUNCATE 거부와 불변 테이블 UPDATE 거부를 확인한다.

- [ ] **Step 5: 통합 테스트를 GREEN으로 만든다**

Run:

```powershell
.\mvnw.cmd -Dtest=Phase6SchemaPostgresIntegrationTest test
```

Expected: PASS.

---

### Task 2: 6A 증적 생성·저장·검증 코어

**Files:**
- Modify: `pom.xml`
- Modify: `.env.example`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/fonts/NotoSansKR-Regular.ttf`
- Create: `src/main/resources/fonts/OFL.txt`
- Create: `src/main/java/kr/or/arte/seal/evidence/domain/EvidenceGenerationStatus.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/domain/EvidenceVerificationResult.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceQueue.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceManifestFactory.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidencePackageGenerator.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidencePackageVerifier.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/infrastructure/EvidenceHmacSigner.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/infrastructure/LocalEvidenceStorage.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/infrastructure/OpenHtmlEvidencePdfRenderer.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/application/EvidenceManifestFactoryTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/infrastructure/LocalEvidenceStorageTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/infrastructure/OpenHtmlEvidencePdfRendererTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/application/EvidencePackageVerifierTest.java`

**Interfaces:**
- Consumes: canonical Jackson `ObjectMapper`, `Clock`, 설정 파일 HMAC 키, 완료 응답 투영
- Produces: `EvidenceQueue.enqueue(UUID responseId, UUID requestId, Instant queuedAt)`, `EvidencePackageGenerator.generate(EvidenceWorkItem item)`, `EvidencePackageVerifier.verify(EvidencePackageView item)`

- [ ] **Step 1: canonical JSON·HMAC·변조 검출 실패 테스트를 작성한다**

손으로 고정한 파일 해시를 사용해 매니페스트 키 순서가 달라도 같은 canonical bytes와 HMAC이 나오는지, 한 바이트 변경이 `INVALID_HASH`인지 검증한다.

```java
assertThat(factory.canonicalBytes(files)).isEqualTo(expectedUtf8);
assertThat(verifier.verify(tamperedPackage)).isEqualTo(EvidenceVerificationResult.INVALID_HASH);
```

- [ ] **Step 2: 파일 경로와 PDF 실패 테스트를 작성한다**

JUnit `@TempDir`에서 `../escape` 경로를 거부하고, 정상 생성은 임시 디렉터리가 남지 않으며 PDFBox로 추출한 텍스트에 `개인정보 동의 증적`과 요청 UUID가 포함되는지 검증한다.

- [ ] **Step 3: RED를 확인한다**

Run:

```powershell
.\mvnw.cmd -Dtest=EvidenceManifestFactoryTest,LocalEvidenceStorageTest,OpenHtmlEvidencePdfRendererTest,EvidencePackageVerifierTest test
```

Expected: 신규 타입 부재로 컴파일 FAIL.

- [ ] **Step 4: 의존성과 최소 코어를 구현한다**

`pom.xml`에 다음을 추가한다.

```xml
<dependency>
  <groupId>io.github.openhtmltopdf</groupId>
  <artifactId>openhtmltopdf-pdfbox</artifactId>
  <version>1.1.76</version>
</dependency>
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.8</version>
</dependency>
```

HMAC은 `Mac.getInstance("HmacSHA256")`, 해시는 `MessageDigest.getInstance("SHA-256")`, ZIP은 JDK API만 사용한다. PDF HTML은 정적 XHTML과 내부 CSS만 사용하고 렌더러에 Noto Sans KR 파일을 직접 등록한다.

- [ ] **Step 5: 코어 테스트를 GREEN으로 만든다**

같은 focused 명령을 실행해 4개 테스트 클래스를 모두 통과시킨다.

---

### Task 3: 증적 DB 작업·동의 완료 연동·관리자 화면

**Files:**
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceRepository.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceWorker.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceQueryService.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/persistence/EvidenceMapper.java`
- Create: `src/main/java/kr/or/arte/seal/evidence/persistence/MyBatisEvidenceRepository.java`
- Create: `src/main/resources/mybatis/mappers/evidence/EvidenceMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/evidence/web/EvidenceController.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/evidence/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/evidence/detail.jsp`
- Modify: `src/main/java/kr/or/arte/seal/consent/application/ConsentSubmissionService.java`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/evidence/application/EvidenceWorkerTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/persistence/EvidencePostgresIntegrationTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/web/EvidenceControllerTest.java`
- Modify: `src/test/java/kr/or/arte/seal/consent/application/ConsentSubmissionServiceTest.java`

**Interfaces:**
- Consumes: Task 2 생성기·검증기, 기존 응답·스냅샷·이벤트 DB
- Produces: 증적 목록·상세·검증·재시도·ZIP 다운로드와 동의 완료 원자 큐 등록

- [ ] **Step 1: 큐 원자성·선점·멱등 실패 테스트를 작성한다**

같은 응답을 두 번 enqueue해 활성 행이 하나인지, 두 worker가 `claimDue`할 때 하나만 가져가는지, 10분 지난 PROCESSING만 복구되는지 실제 PostgreSQL에서 검증한다.

- [ ] **Step 2: 다운로드 권한·범위·CSRF 실패 테스트를 작성한다**

`EVIDENCE_DOWNLOAD`이 없거나 다른 조직이면 403/404, 검증 실패 파일이면 409, 변경 POST에 CSRF가 없으면 403을 기대한다.

- [ ] **Step 3: RED를 확인한다**

Run:

```powershell
.\mvnw.cmd -Dtest=EvidenceWorkerTest,EvidencePostgresIntegrationTest,EvidenceControllerTest,ConsentSubmissionServiceTest test
```

- [ ] **Step 4: persistence와 worker를 구현한다**

`claimDue`는 다음 형태의 CTE를 사용한다.

```sql
WITH picked AS (
  SELECT evidence_package_id FROM evidence_package
   WHERE generation_status='QUEUED'
      OR (generation_status='PROCESSING' AND locked_at < #{leaseExpiredAt})
   ORDER BY queued_at
   FOR UPDATE SKIP LOCKED LIMIT #{limit}
)
UPDATE evidence_package p SET generation_status='PROCESSING', locked_at=#{now}
FROM picked WHERE p.evidence_package_id=picked.evidence_package_id
RETURNING p.*
```

실제 파일 저장이 성공한 뒤 한 트랜잭션에서 파일 메타데이터와 `FINALIZED`를 저장한다. 런타임 실패는 개인정보 없는 코드로 `FAILED` 처리한다.

- [ ] **Step 5: 동의 제출과 큐 등록을 원자화한다**

`ConsentSubmissionService` 생성자에 `EvidenceQueue`를 추가하고 `insertResponse` 직후 같은 트랜잭션에서 `enqueue(responseId, requestId, now)`를 호출한다. 같은 제출 멱등키 재요청은 새 큐를 만들지 않는다.

- [ ] **Step 6: 관리자 JSP와 다운로드를 구현한다**

목록은 상태·기간 필터와 집계만 표시한다. 상세에는 파일명·크기·SHA-256 앞 12자·최근 검증 결과를 표시한다. 다운로드는 검증 후 메모리 ZIP을 스트리밍하고 `Cache-Control: no-store`를 반환한다.

- [ ] **Step 7: focused 테스트를 GREEN으로 만든다**

Step 3 명령을 다시 실행한다.

---

### Task 4: 6B 철회·권리요청

**Files:**
- Create: `src/main/java/kr/or/arte/seal/rights/domain/RightsRequestType.java`
- Create: `src/main/java/kr/or/arte/seal/rights/domain/RightsRequestStatus.java`
- Create: `src/main/java/kr/or/arte/seal/rights/domain/RightsRequestTransition.java`
- Create: `src/main/java/kr/or/arte/seal/retention/application/RetentionJobRequestPort.java`
- Create: `src/main/java/kr/or/arte/seal/rights/application/RightsRequestService.java`
- Create: `src/main/java/kr/or/arte/seal/rights/application/RightsRequestRepository.java`
- Create: `src/main/java/kr/or/arte/seal/rights/persistence/RightsRequestMapper.java`
- Create: `src/main/java/kr/or/arte/seal/rights/persistence/MyBatisRightsRequestRepository.java`
- Create: `src/main/resources/mybatis/mappers/rights/RightsRequestMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/rights/web/RightsRequestController.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/rights/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/rights/form.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/rights/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/rights/domain/RightsRequestTransitionTest.java`
- Test: `src/test/java/kr/or/arte/seal/rights/application/RightsRequestServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/rights/persistence/RightsRequestPostgresIntegrationTest.java`
- Test: `src/test/java/kr/or/arte/seal/rights/web/RightsRequestControllerTest.java`

**Interfaces:**
- Consumes: 대상자·요청 범위 조회, `PersonalDataCipher`, 증적 조회
- Produces: 접수·외부 본인확인·검토·승인/반려·완료, 불변 철회 이력과 삭제 작업 연결, Task 5가 구현할 `RetentionJobRequestPort`

- [ ] **Step 1: 상태·이중 통제·기한 실패 테스트를 작성한다**

접수일보다 이른 기한, 본인확인 없는 검토, 등록자와 같은 결정자, 승인되지 않은 완료를 각각 거부하고 정상 상태 경로를 검증한다.

- [ ] **Step 2: 암호화·불변 철회·범위 실패 테스트를 작성한다**

DB에 메모 원문이 없고 복호화 상세만 원문을 반환하는지, 철회가 `consent_response`를 바꾸지 않고 `consent_withdrawal` 하나를 추가하는지, 다른 조직 관리자가 404인지 검증한다.

- [ ] **Step 3: RED를 확인한다**

Run:

```powershell
.\mvnw.cmd -Dtest=RightsRequestTransitionTest,RightsRequestServiceTest,RightsRequestPostgresIntegrationTest,RightsRequestControllerTest test
```

- [ ] **Step 4: 도메인·암호화 persistence·서비스를 구현한다**

상태 전이는 enum 분기 한 곳에 모으고 모든 명령은 `findScopedForUpdate` 후 row version을 증가시킨다. 메모 암호화 컨텍스트는 `RIGHTS_REQUEST/<id>/<org>/<field>`로 고정한다.

- [ ] **Step 5: 유형별 완료 연결을 구현한다**

`WITHDRAWAL`은 불변 철회 행과 요청·감사를 같은 트랜잭션에 저장한다. `ERASURE`는 `RetentionJobRequestPort.requestUrgent(...)`가 반환한 job ID를 연결하며 CONFIRMED 확인 전 완료를 거부한다. `ACCESS`, `CORRECTION`, `RESTRICTION`은 실행 결과 객체 ID와 시각을 기록한다.

- [ ] **Step 6: JSP·권한·CSRF를 구현하고 GREEN을 확인한다**

목록에는 개인정보가 없는 필드만 표시하고 상세에서만 권한 확인 후 메모를 복호화한다. Step 3 focused 테스트를 모두 통과시킨다.

---

### Task 5: 6C 보유기간·보존 보류·파기

**Files:**
- Create: `src/main/java/kr/or/arte/seal/retention/domain/RetentionDueDateCalculator.java`
- Create: `src/main/java/kr/or/arte/seal/retention/domain/RetentionJobStatus.java`
- Create: `src/main/java/kr/or/arte/seal/retention/domain/RetentionJobTransition.java`
- Create: `src/main/java/kr/or/arte/seal/retention/application/RetentionScheduleService.java`
- Create: `src/main/java/kr/or/arte/seal/retention/application/RetentionJobService.java`
- Create: `src/main/java/kr/or/arte/seal/retention/application/RetentionRepository.java`
- Create: `src/main/java/kr/or/arte/seal/retention/persistence/RetentionMapper.java`
- Create: `src/main/java/kr/or/arte/seal/retention/persistence/MyBatisRetentionRepository.java`
- Create: `src/main/resources/mybatis/mappers/retention/RetentionMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/retention/web/RetentionJobController.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/retention/list.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/retention/domain/RetentionDueDateCalculatorTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/domain/RetentionJobTransitionTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/application/RetentionJobServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/persistence/RetentionPostgresIntegrationTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/web/RetentionJobControllerTest.java`

**Interfaces:**
- Consumes: 승인 보유정책, 응답·완료시각, 수동 기산 이벤트, recipient·mail·session·evidence 저장소
- Produces: `RetentionJobRequestPort`, 예정일, 보존 보류, 승인·실행·확인과 최소 데이터 보존

- [ ] **Step 1: 날짜 계산 RED 테스트를 작성한다**

리터럴 기대값으로 다음을 검증한다.

```java
assertThat(calc(DATE_2024_02_29, 1, YEAR, 0)).isEqualTo(LocalDate.of(2025, 2, 28));
assertThat(calc(DATE_2026_01_31, 1, MONTH, 3)).isEqualTo(LocalDate.of(2026, 3, 3));
```

기산 이벤트가 필요한 정책은 임의 추정 대신 `ANCHOR_REQUIRED`를 반환해야 한다.

- [ ] **Step 2: 이중 통제·보류·멱등 RED 테스트를 작성한다**

요청자=승인자, 실행자=확인자, 활성 보류 중 승인, 다른 멱등키 중복 실행을 거부한다. 같은 멱등키 재실행은 기존 결과를 반환한다.

- [ ] **Step 3: 과잉 파기 방지 PostgreSQL RED 테스트를 작성한다**

같은 recipient에 보존 중 요청과 만료 요청을 만들고 만료 요청을 실행한다. 만료 요청의 mail 암호문은 NULL이지만 recipient 이름·이메일은 유지되어야 한다. 마지막 요청까지 만료된 경우에만 recipient PII가 NULL이고 상태가 DESTROYED여야 한다.

- [ ] **Step 4: RED를 확인한다**

Run:

```powershell
.\mvnw.cmd -Dtest=RetentionDueDateCalculatorTest,RetentionJobTransitionTest,RetentionJobServiceTest,RetentionPostgresIntegrationTest,RetentionJobControllerTest test
```

- [ ] **Step 5: 계산·상태·persistence를 구현한다**

날짜 계산은 `LocalDate.plusDays/plusMonths/plusYears`를 직접 사용한다. 모든 실행은 job 행을 잠그고 보류·상태·행위자 분리와 멱등키를 검사한다. SQL은 다른 보존 중 요청 존재 여부를 `NOT EXISTS`로 확인한 뒤에만 recipient PII를 NULL 처리한다.

- [ ] **Step 6: 주 저장소 파일 파기를 구현한다**

`FILE_SECURE_DELETE`는 루트 경로 검증 후 해당 패키지 파일을 삭제하고 DB 메타데이터를 DESTROYED로 바꾼다. 실패 시 job을 FAILED로 기록하고 성공으로 확인하지 않는다. 백업·물리 매체 삭제는 수행하지 않는다.

- [ ] **Step 7: JSP·권한·CSRF와 GREEN을 확인한다**

상세 화면은 요청자·승인자·실행자·확인자와 주 저장소 범위를 명시한다. Step 4 focused 테스트를 통과시킨다.

---

### Task 6: 통계·통합 검증·문서·GitHub

**Files:**
- Create: `src/main/java/kr/or/arte/seal/statistics/application/StatisticsQueryService.java`
- Create: `src/main/java/kr/or/arte/seal/statistics/persistence/StatisticsMapper.java`
- Create: `src/main/resources/mybatis/mappers/statistics/StatisticsMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/statistics/web/StatisticsController.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/statistics/index.jsp`
- Test: `src/test/java/kr/or/arte/seal/statistics/persistence/StatisticsPostgresIntegrationTest.java`
- Test: `src/test/java/kr/or/arte/seal/statistics/web/StatisticsControllerTest.java`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/home.jsp`
- Modify: `README.md`
- Modify: `database/README.md`
- Modify: `docs/superpowers/plans/2026-08-24-phase6-evidence-rights-lifecycle-implementation.md`

**Interfaces:**
- Consumes: Phase 1~6 현재 상태 테이블과 관리자 데이터 범위
- Produces: 최근 30일·최대 1년 개인정보 없는 운영 집계와 최종 검증 기록

- [ ] **Step 1: 통계 범위·기간·개인정보 부재 RED 테스트를 작성한다**

두 조직의 fixture를 만들고 허용 조직 수만 집계되는지, 1년 초과 기간이 거부되는지, 반환 DTO에 recipient ID·이름·이메일 필드가 없는지 검증한다.

- [ ] **Step 2: 통계 쿼리와 JSP를 구현한다**

각 집계는 관리자 scope CTE에 조인한다. 기본 30일이며 `from`, `to`를 `LocalDate`로 받고 기간을 검증한다. JSP는 상태별 카드와 표만 사용하며 외부 JS·차트 라이브러리를 추가하지 않는다.

- [ ] **Step 3: focused 통계 테스트를 GREEN으로 만든다**

Run:

```powershell
.\mvnw.cmd -Dtest=StatisticsPostgresIntegrationTest,StatisticsControllerTest test
```

- [ ] **Step 4: SQL 009와 전체 자동화 테스트를 실행한다**

Run:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

Expected: 전체 실패·오류 0, `target/seal.war` 생성.

- [ ] **Step 5: 9090 대표 화면을 검증한다**

로그인 후 증적·권리요청·파기·통계 화면 200, 권한 없는 사용자 403/404, 증적 다운로드 no-store, 데스크톱과 360px 수평 넘침 없음, 공개 동의 기존 흐름 회귀 없음을 확인한다.

- [ ] **Step 6: 비밀정보·변경 상태를 검증한다**

실제 비밀번호, HMAC 키, 증적 파일, `.env`와 `target`이 staged되지 않았는지 확인하고 `git diff --check`를 통과시킨다.

- [ ] **Step 7: 완료 문서와 Git 브랜치를 정리한다**

README에 Phase 6 기능·로컬 URL·키 설정·운영 미검증 범위를 기록하고 모든 실제 완료 체크박스를 `[x]`로 바꾼다. 기능 커밋을 생성해 `feature/phase6-evidence-rights-lifecycle` 원격 브랜치로 푸시하며 main 병합과 PR 생성은 별도 승인 전까지 수행하지 않는다.
