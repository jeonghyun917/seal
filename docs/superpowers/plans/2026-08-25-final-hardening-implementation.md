# Consent System Final Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 파기 후 조회, 증적 워커 동시성, 메일 링크 보안, 목록 페이지 처리와 관리자 UI를 운영 가능한 완료 기준으로 보강한다.

**Architecture:** 기존 Spring MVC/JSP/MyBatis 모듈 구조를 유지한다. 상태 전이는 DB 제약과 Java enum/read model을 일치시키고, 워커 소유권은 claim 시점의 `row_version`으로 검증한다. 공개 URL은 요청 헤더 대신 고정 설정을 사용하고, 공통 페이지 요청 모델을 각 목록 SQL에 적용한다.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring Security, JSP/JSTL, MyBatis, PostgreSQL 16+, JUnit 5, AssertJ

**Spec:** `docs/superpowers/specs/2026-08-25-security-reliability-admin-shell-design.md`

## Global Constraints

- 전자서명, SMS, OTP, PASS, 열람 추적 픽셀은 구축 범위에서 제외한다.
- 기존 PostgreSQL 수동 SQL 및 체크섬 절차를 유지한다.
- 관리자 권한과 조직 범위 검사는 서버에서 강제한다.
- 현재 격리 작업 트리의 사용자 변경을 보존하며 작업 중 자동 커밋하지 않는다.

---

### Task 1: 파기 상태 안전 조회

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/recipient/domain/RecipientStatus.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/application/MailOutboxStatus.java`
- Modify: `src/main/java/kr/or/arte/seal/recipient/application/RecipientQueryService.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/application/MailOutboxQueryService.java`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/recipients/detail.jsp`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/mail/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/recipient/application/RecipientQueryServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/delivery/application/MailOutboxQueryServiceTest.java`

**Interfaces:**
- Consumes: DB 상태 `DESTROYED`와 NULL 암호화 컬럼
- Produces: 파기 데이터의 복호화 없는 tombstone 조회

- [ ] 파기 행 조회가 현재 enum 변환 또는 복호화에서 실패하는 회귀 테스트를 작성하고 실패를 확인한다.
- [ ] 두 enum에 `DESTROYED`를 추가하고 조회 서비스가 파기 상태에서 고정 마스킹 값을 반환하게 한다.
- [ ] 상세 화면에서 파기 행의 수정·재활성화·미리보기·재발송 작업을 숨긴다.
- [ ] 관련 단위·PostgreSQL 통합 테스트를 실행해 통과를 확인한다.

### Task 2: 파기 파일 삭제 완료 게이트

**Files:**
- Modify: `database/03_patch/20260824_005_create_evidence_rights_retention_tables.sql` 또는 신규 불변 patch
- Modify: `src/main/java/kr/or/arte/seal/retention/application/RetentionJobService.java`
- Modify: `src/main/java/kr/or/arte/seal/retention/application/RetentionJobRecord.java`
- Modify: `src/main/resources/mybatis/mappers/retention/RetentionMapper.xml`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/retention/application/RetentionJobServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/persistence/RetentionPostgresIntegrationTest.java`

**Interfaces:**
- Consumes: `LocalEvidenceStorage.purgeJobQuarantine(UUID)` 반환 건수/예외
- Produces: 격리 파일 삭제 성공이 확인된 작업만 `CONFIRMED` 전이

- [ ] purge 실패 후 confirm이 거부되는 테스트를 작성하고 현재 동작의 실패를 확인한다.
- [ ] 삭제 완료 여부와 실패 사유를 영속화하는 신규 SQL patch와 mapper를 작성한다.
- [ ] execute 재시도가 삭제를 재수행하고, confirm이 삭제 완료 상태를 검사하도록 구현한다.
- [ ] UI에 삭제 대기·실패 상태 및 재시도 안내를 표시한다.
- [ ] 성공·실패·멱등 재시도 통합 테스트를 실행한다.

### Task 3: 증적 워커 claim 소유권

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceWorkItem.java`
- Modify: `src/main/java/kr/or/arte/seal/evidence/persistence/EvidenceWorkRow.java`
- Modify: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceRepository.java`
- Modify: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceWorker.java`
- Modify: `src/main/resources/mybatis/mappers/evidence/EvidenceMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/evidence/application/EvidenceWorkerTest.java`
- Test: `src/test/java/kr/or/arte/seal/evidence/persistence/EvidenceWorkerLeasePostgresIntegrationTest.java`

**Interfaces:**
- Consumes: claim SQL이 증가시킨 `row_version`
- Produces: `finalized/failed(packageId, claimedRowVersion, ...)` 소유권 조건부 갱신

- [ ] 이전 claim이 새 claim의 상태를 변경하지 못하는 동시성 테스트를 작성하고 실패를 확인한다.
- [ ] claim 결과에 `rowVersion`을 포함하고 repository 계약을 확장한다.
- [ ] finalize/fail SQL에 `row_version=#{claimedRowVersion}` 조건을 추가하고 0건 갱신을 소유권 상실로 처리한다.
- [ ] 워커 단위 테스트와 실제 PostgreSQL lease takeover 테스트를 통과시킨다.

### Task 4: 메일 및 링크 보안

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/kr/or/arte/seal/request/web/ConsentRequestController.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/web/MailOutboxController.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/application/MailOutboxQueryService.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/application/MailOutboxWorker.java`
- Modify: `src/main/resources/mybatis/mappers/delivery/MailOutboxMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/request/web/ConsentRequestControllerTest.java`
- Test: `src/test/java/kr/or/arte/seal/delivery/application/MailOutboxWorkerTest.java`

**Interfaces:**
- Consumes: `seal.public-base-url` 고정 설정과 outbox의 `secure_link_id`
- Produces: 승인 도메인 링크, 토큰이 비활성화된 미리보기, 현재 링크만 발송

- [ ] Host 헤더가 메일 링크를 바꾸지 못하고 미리보기 토큰이 클릭 불가능한지 테스트한다.
- [ ] `SEAL_PUBLIC_BASE_URL` 필수 설정을 추가하고 컨트롤러의 요청 기반 URL 생성을 제거한다.
- [ ] 미리보기 HTML의 동의 href를 비활성 placeholder로 치환한다.
- [ ] 발송 claim/전송 직전 현재 활성 링크 여부를 검증해 구 링크 outbox를 `CANCELLED` 처리한다.
- [ ] 관련 MVC·worker·PostgreSQL 테스트를 실행한다.

### Task 5: 공통 페이지 처리

**Files:**
- Create: `src/main/java/kr/or/arte/seal/common/web/PageRequest.java`
- Create: `src/main/java/kr/or/arte/seal/common/web/PageSlice.java`
- Modify: 목록 controller/service/repository/mapper와 관리자 목록 JSP
- Test: 각 목록 모듈의 controller 및 PostgreSQL integration test

**Interfaces:**
- Consumes: `page`(1 이상), `size`(기본 20, 최대 100), 기존 상태 검색조건
- Produces: 정렬이 안정적인 `LIMIT size+1 OFFSET (page-1)*size` 결과와 이전/다음 링크

- [ ] 101개 행에서 두 번째 페이지가 조회되는 대표 통합 테스트를 작성하고 실패를 확인한다.
- [ ] 공통 페이지 입력 검증과 slice 모델을 구현한다.
- [ ] 대상자·동의요청·메일·증적·권리요청·파기 목록의 고정 `LIMIT 100`을 페이지 SQL로 교체한다.
- [ ] JSP에 검색조건을 유지하는 이전/다음 탐색을 추가한다.
- [ ] 대표 모듈 101건 테스트와 전체 목록 MVC 테스트를 실행한다.

### Task 6: 관리자 UI 및 개발 검증 개선

**Files:**
- Modify: `src/main/webapp/WEB-INF/jsp/admin/rights/form.jsp`
- Modify: `src/main/webapp/WEB-INF/jsp/fragments/admin/sidebar.jsp`
- Modify: `.github/workflows/verify.yml`
- Create: `.gitattributes`
- Create/Modify: 테스트 전용 설정과 임시 키 생성 코드
- Test: `src/test/java/kr/or/arte/seal/admin/web/AdminShellJspTest.java`
- Test: 권리요청 MVC 테스트

**Interfaces:**
- Consumes: 관리자 authority와 범위 내 대상자/동의요청 검색
- Produces: 권한별 메뉴, 검색 선택형 권리요청, 비밀 `.env` 없이 재현 가능한 `mvn verify`

- [ ] 권한 없는 메뉴가 출력되지 않고 UUID 직접 입력이 필요하지 않은 화면 테스트를 작성한다.
- [ ] sidebar에 Spring Security tag 기반 권한 조건을 적용한다.
- [ ] 권리요청 폼을 범위 내 대상자·동의요청 선택 방식으로 변경한다.
- [ ] test profile이 임시 키와 격리 저장소를 자동 준비하게 한다.
- [ ] CI whitespace 검사를 PR 기준 커밋 범위로 바꾸고 `.gitattributes`로 줄바꿈을 고정한다.
- [ ] `.env` 미주입 상태에서 전체 `mvn verify`와 `git diff --check`를 실행한다.

## Final Verification

- [ ] 신규 회귀 테스트 각각의 RED→GREEN 기록을 확인한다.
- [ ] 루트 `.env`를 주입하지 않고 `./mvnw verify`를 실행해 실패 0건을 확인한다.
- [ ] `git diff --check`와 비밀정보 패턴 검사를 실행한다.
- [ ] 9090에서 관리자 로그인, 각 목록 페이지, 파기 tombstone, 공개 동의 링크를 대표 스모크 테스트한다.
- [ ] 변경 파일과 잔여 비차단 개선사항을 최종 보고한다.
