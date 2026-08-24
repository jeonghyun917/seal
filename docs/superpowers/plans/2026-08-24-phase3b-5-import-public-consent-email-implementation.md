# Phase 3B~5 Import, Public Consent, and Email Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Excel 대상자 일괄반입부터 공개 동의 제출, HTML 이메일 Outbox 발송까지 하나의 안전한 요청 흐름으로 완성한다.

**Architecture:** `recipient.importing`, `consent`, `delivery` 모듈을 기존 `recipient`, `request`, `link` application 경계에 연결한다. 업로드 임시 개인정보와 메일 본문은 AES-GCM으로 암호화하고, 공개 토큰·흐름 식별자는 HMAC만 저장하며, 상태 전이는 PostgreSQL 행 잠금과 멱등키로 보호한다.

**Tech Stack:** Java 17, eGovFrame 5.0.0, Spring Boot 3.5.6, JSP/JSTL, Spring Security, MyBatis, PostgreSQL, Apache POI, Spring Mail

**Spec:** `docs/superpowers/specs/2026-08-24-phase3b-5-import-public-consent-email-design.md`

## Global Constraints

- 전자서명, SMS·알림톡, OTP, 실명 본인확인, 추적 픽셀, 민감정보·고유식별정보와 만 14세 미만은 제외한다.
- Flyway/Liquibase 없이 누적 SQL 패치와 검증 SQL을 사용한다.
- 공개 GET은 상태를 변경하지 않고 모든 공개 POST는 CSRF를 유지한다.
- 이메일·웹은 동일한 불변 요청 스냅샷을 사용한다.
- 개인정보·토큰·HTML 원문은 로그와 감사 메타데이터에 기록하지 않는다.

---

### Task 1: Phase 3B DB와 Apache POI 기반 파일 경계

**Files:** `pom.xml`, `database/03_patch/20260824_004_create_import_consent_delivery_tables.sql`, `database/04_verify/008_import_consent_delivery_verify.sql`, `recipient/importing/**`

**Interfaces:** `RecipientWorkbookParser.parse(InputStream) -> ParsedRecipientWorkbook`; `UploadMalwareScanner.scan(InputStream) -> MalwareScanResult`.

- [ ] 실패 테스트로 `.xlsx` 헤더·크기·행수·수식·이메일 오류와 ClamAV 실패 폐쇄를 정의한다.
- [ ] 테스트가 타입 부재와 검증 부재로 실패하는 것을 확인한다.
- [ ] POI 파서, 업로드 제한 설정, JDK 소켓 ClamAV 경계와 로컬 비활성 구현을 추가한다.
- [ ] 단위 테스트를 통과시키고 의존성 트리를 확인한다.
- [ ] SQL 패치·검증 SQL을 작성해 `seal_test`, `seal_dev`에 적용한다.

### Task 2: 암호화 임시 배치와 확인 반입

**Files:** `recipient/importing/application/**`, `recipient/importing/persistence/**`, `recipient/importing/web/**`, 관리자 JSP/CSS

**Interfaces:** `RecipientImportService.validate(...) -> RecipientImportPreview`; `confirm(actorId,batchId) -> RecipientImportResult`; `expireDue(now,limit)`.

- [ ] 정상·중복·오류·재확인·24시간 만료·조직 범위 실패 테스트를 작성한다.
- [ ] RED를 확인한 뒤 암호화 staging, 마스킹 결과, 행 잠금과 멱등 확인 반입을 구현한다.
- [ ] 양식 다운로드, 업로드, 미리보기, 확인 화면과 `RECIPIENT_IMPORT` 권한을 구현한다.
- [ ] 실제 PostgreSQL 통합 테스트와 MVC CSRF·권한 테스트를 통과시킨다.
- [ ] Phase 3B 변경을 커밋한다.

### Task 3: 공개 링크 세션과 스캐너 안전 진입

**Files:** `consent/domain/**`, `consent/application/**`, `consent/persistence/**`, `consent/web/**`, `common/config/SecurityConfiguration.java`

**Interfaces:** `PublicConsentEntryService.inspect(token)`; `start(token,httpSession) -> PublicConsentFlow`; `PublicConsentQuery.load(flowId,sessionId)`.

- [ ] GET 무상태, 잘못된·만료·폐기·완료 링크, POST만 ACCESSED 전환 테스트를 작성한다.
- [ ] RED 확인 후 토큰 HMAC 조회, DB 공개 세션 HMAC, 30분 만료와 세션 결합을 구현한다.
- [ ] 공개 Security 허용 경로, 보안 헤더, 기관 확인 JSP를 구현한다.
- [ ] 실제 PostgreSQL과 MVC 테스트를 통과시킨다.

### Task 4: 항목 결정·확인·멱등 최종 제출

**Files:** `consent/application/**`, `consent/domain/**`, `consent/persistence/**`, 공개 JSP/CSS

**Interfaces:** `storeDecisions(flow,Map<UUID,Decision>)`; `submit(flow,idempotencyKey) -> ConsentSubmissionResult`.

- [ ] 전체 항목 선택 강제, 기본값 없음, 변조 항목 거부, 같은 멱등키 재응답, 다른 키 중복 거부 테스트를 작성한다.
- [ ] RED 확인 후 서버 세션 결정 보관과 단일 트랜잭션 최종 제출을 구현한다.
- [ ] 모바일 결정·확인·완료 JSP와 접근 가능한 오류·포커스 처리를 구현한다.
- [ ] PostgreSQL 동시/멱등 통합 테스트와 MVC CSRF 테스트를 통과시킨다.
- [ ] Phase 4 변경을 커밋한다.

### Task 5: 스냅샷 HTML 메일과 암호화 Outbox

**Files:** `delivery/domain/**`, `delivery/application/**`, `delivery/persistence/**`, `PersonalDataContext.java`

**Interfaces:** `ConsentMailRenderer.render(snapshot,recipient,publicUrl)`; `ConsentDeliveryService.activateAndQueue(...) -> DeliveryIssueResult`; `reissueAndQueue(...)`.

- [ ] 외부 리소스·폼·추적 픽셀 없는 HTML, 동일 해시, 암호화 저장, 이전 대기 메일 취소 테스트를 작성한다.
- [ ] RED 확인 후 스냅샷 렌더러, 연락처 내부 조회, 링크+Outbox 원자 트랜잭션을 구현한다.
- [ ] 대상 이메일·HTML AES-GCM 암호화와 SHA-256 고정을 구현한다.
- [ ] PostgreSQL 통합 테스트를 통과시킨다.

### Task 6: SMTP worker, 재시도와 관리자 화면

**Files:** `delivery/config/**`, `delivery/scheduling/**`, `delivery/web/**`, 관리자 JSP/CSS, `application.yml`, `.env.example`

**Interfaces:** `MailTransport.send(MailMessage)`; `MailOutboxWorker.processBatch(now,limit)`.

- [ ] SMTP 성공·일시 오류·영구 오류·1/5/30분 재시도·10분 임대 복구·로컬 비발송 테스트를 작성한다.
- [ ] RED 확인 후 JavaMail 전송기와 `FOR UPDATE SKIP LOCKED` worker를 구현한다.
- [ ] 요청 화면의 발송/재발송과 메일 상태 목록·상세·로컬 HTML 미리보기를 구현한다.
- [ ] `SMTP_ACCEPTED`가 수신·열람이 아님을 모든 상태 화면에 명시한다.
- [ ] Phase 5 변경을 커밋한다.

### Task 7: 통합 검증과 전달

**Files:** `README.md`, `database/README.md`, 본 계획 문서

- [ ] SQL `008`을 `seal_test`, `seal_dev`에서 검증한다.
- [ ] 전체 `mvnw test`와 `clean package`를 실행한다.
- [ ] 9090에서 Excel 미리보기/확인, 공개 진입/결정/제출, 로컬 메일 미리보기를 확인한다.
- [ ] 데스크톱과 360px, 콘솔 오류, 수평 넘침, 보안 헤더를 확인한다.
- [ ] 비밀정보 스캔, `git diff --check`, 상태 확인 후 브랜치를 비공개 GitHub에 푸시한다.

