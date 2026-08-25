# 보안·신뢰성·관리자 공통 셸 보완 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 전체 검수에서 발견한 P0/P1 결함을 제거하고 모든 관리자 업무 페이지에 지속되는 공통 사이드바를 적용한다.

**Architecture:** 기존 JSP 모듈형 모놀리스를 유지하면서 DB 제약과 actor-scoped SQL을 방어의 최종선으로 사용한다. 파일 작업은 짧은 DB 트랜잭션 사이의 멱등 단계로 분리하고, 암호화/HMAC은 필드·패키지별 키 ID로 검증 키를 선택한다.

**Tech Stack:** Java 17, eGovFrame Runtime 5.0.0, Spring Boot 3.5, JSP/JSTL, MyBatis, PostgreSQL 16, JUnit 5, AssertJ

**Spec:** `docs/superpowers/specs/2026-08-25-security-reliability-admin-shell-design.md`

## Global Constraints

- 전자서명, SMS·알림톡, OTP, 실명 본인확인, 민감정보·고유식별정보와 만 14세 미만 기능은 추가하지 않는다.
- Flyway·Liquibase, Redis·Kafka, 유료 KMS는 추가하지 않는다.
- 데이터 변경은 수동 SQL 패치·체크섬 절차를 유지한다.
- 모든 신규 업무 행동은 실패 테스트를 먼저 확인한 뒤 구현한다.
- 외부 운영서버 배포·백업 검증은 이번 구현 범위에서 제외한다.

---

### Task 1: 권리요청 조직 범위와 복합 참조 무결성

**Files:**
- Modify: `rights` controller/service/repository/mapper
- Create: `database/03_patch/20260825_008_harden_rights_and_keys.sql`
- Create: `database/04_verify/012_security_reliability_verify.sql`
- Test: rights service/MVC/PostgreSQL integration tests

**Interfaces:**
- `RightsRequestService.create(UUID actorId, CreateRightsRequestCommand command)`
- Repository insert는 영향 행 수 1건을 보장한다.

- [ ] 범위 밖 조직·대상자·요청 조합을 거부하는 실패 테스트를 작성하고 실패를 확인한다.
- [ ] 유형별 동의요청 필수 검증 테스트를 작성하고 실패를 확인한다.
- [ ] actor-scoped `INSERT ... SELECT`와 DB 복합 제약을 구현한다.
- [ ] 긴급 파기·처리제한 보류 SQL에도 actor scope를 적용한다.
- [ ] 관련 단위·PostgreSQL 테스트를 통과시킨다.

### Task 2: AES/HMAC 키 버전 수명주기

**Files:**
- Modify: rights record/service/mapper and schema patch
- Modify: evidence signer/generator/query/repository and properties
- Modify: link/email HMAC loaders and lookup repositories
- Test: key rotation unit/PostgreSQL integration tests

- [ ] 권리요청 필드별 키 ID 회전 실패 테스트를 작성한다.
- [ ] 증적 HMAC `v1` 생성 후 `v2` 활성화 검증 테스트를 작성한다.
- [ ] 링크·이메일 후보 키 조회 테스트를 작성한다.
- [ ] 스키마와 키링 구현을 최소 변경으로 추가한다.
- [ ] 기존 원시 키 파일 호환 테스트와 전체 암호화 테스트를 통과시킨다.

### Task 3: 파기 단계 실행과 복구

**Files:**
- Modify: retention service/repository/mapper/storage
- Modify: database patch and verification SQL
- Test: retention unit/PostgreSQL failure-injection tests

- [ ] `EXECUTING` 이벤트 누락을 재현하는 테스트를 작성한다.
- [ ] 파일 격리 뒤 DB 실패와 동일 멱등키 재실행 테스트를 작성한다.
- [ ] 짧은 트랜잭션의 begin/complete 단계와 파일 격리 저널을 구현한다.
- [ ] 복원·재개 경로와 정확한 상태 이벤트를 구현한다.
- [ ] 파기 전체 회귀와 잔여정보 검증 SQL을 통과시킨다.

### Task 4: 증적 생성 멱등성과 복구

**Files:**
- Modify: `LocalEvidenceStorage`, generator, worker, repository/mapper
- Test: storage/worker/PostgreSQL integration tests

- [ ] 동일 파일 재생성 성공과 불일치 충돌 테스트를 작성한다.
- [ ] 파일 생성 뒤 DB 확정 실패·재시도 테스트를 작성한다.
- [ ] 해시 검증 재사용과 고아 파일 정리 경로를 구현한다.
- [ ] 최종화 영향 행 수 1건을 강제한다.
- [ ] 증적 생성·검증·ZIP 테스트를 통과시킨다.

### Task 5: SMTP 결과 기록과 오류 분류

**Files:**
- Modify: mail message/transport/worker/repository/mapper
- Test: mail worker/transport tests

- [ ] SMTP 성공 뒤 DB accepted 실패 테스트를 작성한다.
- [ ] SMTP 4xx/5xx 분류와 고정 Message-ID 테스트를 작성한다.
- [ ] 복호화, transport, 상태 기록 예외 경계를 분리한다.
- [ ] 행 버전·영향 건수 검증과 중앙 감사를 구현한다.
- [ ] SMTP 비활성/성공/재시도 회귀 테스트를 통과시킨다.

### Task 6: 중앙 감사·오류·입출력 안전성

**Files:**
- Modify: rights/retention/evidence/delivery services
- Create: 관리자 공통 예외 처리기와 오류 JSP
- Create: 권리요청 Form DTO
- Modify: 직접 EL 출력 JSP
- Test: audit/MVC/JSP contract tests

- [ ] 누락된 중앙 감사 행동 테스트를 작성한다.
- [ ] 권리요청 검증 오류의 400/404/409 매핑 테스트를 작성한다.
- [ ] 관리자 표시명 마크업 출력 회귀 테스트를 작성한다.
- [ ] 최소 구현 후 관련 MVC·보안 테스트를 통과시킨다.

### Task 7: 관리자 공통 사이드바

**Files:**
- Create: `src/main/webapp/WEB-INF/jsp/fragments/admin/header.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/fragments/admin/sidebar.jsp`
- Modify: 표준 관리자 목록·상세·등록 JSP
- Modify: admin CSS
- Test: JSP contract/MVC tests

- [ ] 증적·권리요청·파기·통계 페이지의 메뉴 누락 테스트를 작성하고 실패를 확인한다.
- [ ] 공통 fragment와 URL 기반 활성 메뉴를 구현한다.
- [ ] 모든 표준 관리자 페이지를 공통 셸로 감싼다.
- [ ] 데스크톱·모바일 CSS와 대표 MVC 렌더링 테스트를 통과시킨다.

### Task 8: CI·상태 화면·문서 정합성

**Files:**
- Create: `.github/workflows/verify.yml`
- Modify: `pom.xml`, status controller/security, README
- Test: status/security integration tests

- [ ] PostgreSQL 16 CI와 Java 17 `mvnw verify`를 구성한다.
- [ ] 포맷·의존성 검사 게이트를 추가한다.
- [ ] 상태 화면을 실제 런타임 값과 프로필별 권한으로 수정한다.
- [ ] 파기 2인 교차통제와 at-least-once 메일 문서를 정정한다.

### Task 9: 최종 검증

- [ ] 전체 `mvnw clean verify`를 한 번 실행한다.
- [ ] DB 패치 체크섬과 검증 SQL 001~012를 실행한다.
- [ ] 9090에서 로그인·권한거부·메뉴 지속·공개 동의·증적·파기 대표 흐름을 스모크한다.
- [ ] 저장소 비밀정보·`git diff --check`·Git 상태를 확인한다.
- [ ] 검증된 정확한 커밋 SHA를 기록하고 커밋·푸시 준비 상태를 보고한다.
