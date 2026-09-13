# 관리자 상태 한글화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 관리자 목록·표·필터와 연결 상세 화면에서 내부 영문 상태 코드 대신 일관된 한글 명칭을 표시한다.

**Architecture:** 내부 enum·DB·검색 파라미터는 변경하지 않는다. 공통 `StatusLabels.label(domain, code)` 변환기와 JSP EL 함수를 두고 모든 사용자 노출 지점이 이를 사용하도록 한다.

**Tech Stack:** Java 17, Spring MVC, JSP/JSTL, JUnit 5

**Spec:** 사용자 승인 요구사항(2026-08-26) — 프로젝트 모든 목록·표의 상태 한글화

## Global Constraints

- 내부 상태 코드와 상태 전이 조건은 변경하지 않는다.
- 알 수 없는 상태 코드는 숨기지 않고 원본 코드로 표시한다.
- 동일 코드라도 업무 문맥별 의미가 다르면 domain으로 구분한다.
- HTML 출력은 `c:out`으로 이스케이프한다.

---

### Task 1: 공통 상태 한글명 변환기

**Files:**
- Create: `src/main/java/kr/or/arte/seal/common/web/StatusLabels.java`
- Create: `src/main/webapp/WEB-INF/tld/seal-functions.tld`
- Create: `src/test/java/kr/or/arte/seal/common/web/StatusLabelsTest.java`

**Interfaces:**
- Produces: `StatusLabels.label(String domain, String code): String`
- Produces: JSP EL function `seal:statusLabel(domain, code)`

- [x] 지원할 모든 domain/code/한글명을 검증하는 실패 테스트를 작성한다.
- [x] `mvnw.cmd -Dtest=StatusLabelsTest test`로 클래스 부재 실패를 확인한다.
- [x] 불변 Map 기반 변환기와 TLD 함수 선언을 작성한다.
- [x] 같은 테스트를 다시 실행해 통과시킨다.

### Task 2: JSP 상태 출력 교체

**Files:**
- Modify: `src/main/webapp/WEB-INF/jsp/admin/**`의 상태 목록·표·필터·연결 상세 JSP
- Modify: `src/main/webapp/WEB-INF/jsp/admin/statistics/index.jsp`

**Interfaces:**
- Consumes: `${seal:statusLabel(domain, code)}`

- [x] 각 JSP에 seal 함수 taglib를 선언한다.
- [x] 필터 option의 value는 영문 상태 코드를 유지하고 표시문구만 변환한다.
- [x] 표와 상세 화면의 상태 직접 출력을 공통 함수로 교체한다.
- [x] 조건문 속 영문 상태 비교는 업무 로직이므로 변경하지 않는다.

### Task 3: 노출 누락 방지 검수

**Files:**
- Create: `src/test/java/kr/or/arte/seal/common/web/StatusLabelViewCoverageTest.java`

**Interfaces:**
- Consumes: 상태를 노출하는 JSP 경로 목록

- [x] 대상 JSP가 공통 함수를 참조하는지 검사하는 실패 테스트를 작성한다.
- [x] 누락 JSP를 수정하고 정적 검사를 통과시킨다.
- [x] `mvnw.cmd -Dtest=StatusLabelsTest,StatusLabelViewCoverageTest test`를 실행한다.

### Task 4: 통합 검증

**Files:**
- Verify only

- [x] 전체 Maven 테스트를 한 번 실행한다.
- [x] 9090 서버를 재시작한다.
- [x] 로그인 후 요청·대상자·메일·증적·권리요청·파기·템플릿·통계 화면의 HTTP 200과 한글 상태 출력을 확인한다.
- [x] 런타임 로그에 JSP/EL 오류가 없는지 확인한다.
