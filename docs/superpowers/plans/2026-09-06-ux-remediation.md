# 사용자 검수 개선 Implementation Plan

**Goal:** 승인된 사용자 검수 보고서의 16개 항목을 기존 보안·보유·승인 동작을 보존하며 개선한다.
**Spec:** docs/reviews/2026-09-06-user-experience-audit.md (사용자 승인 완료)
**Architecture:** 기존 Spring MVC/MyBatis/JSP를 유지한다. 공개 동의는 확정 스냅샷에서만 표시하고, 통계는 업무 건수와 경고 지표를 분리한다. 관리자 화면은 기존 조회 권한과 마스킹을 유지하며 사용자 요약을 제공한다.
**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis, JSP, JavaScript, 기존 CSS.

## Global Constraints

- 기존 파란 톤, 약 90% 화면 밀도, 가용 폭 전체 사용 유지.
- 일시 표시 `yyyy-MM-dd HH:mm:ss`, 기존 마스킹·조직 범위·권한·CSRF 유지.
- 스키마 변경, 실제 발송·승인·파기, 기존 증적 재생성 금지. 원본 스냅샷 변경 금지.
- 기존 dirty 변경 보존. 커밋·푸시하지 않는다.
- 관련 버그는 실패 재현 테스트 후 수정. UI는 동작 중심 JS/서버 렌더링 검증. 전체 suite는 안정화 후 1회.
- parallel-agent 스킬에 따라 파일 소유권이 겹치지 않는 아래 영역을 분리한다. 공통 CSS/JS/태그 함수는 주 에이전트 소유. 다른 작업의 파일이 필요하면 메시지로 조율한다.

## Task 1: 수신자 동의 (보고서 1, 14, 16)

소유: consent Java/mapper/JSP, public-consent.css 및 해당 테스트.

- [x] 확정 스냅샷과 공개 조회 모델 경계를 읽고 개인정보 항목/제공받는 자 누락의 실패 테스트 작성·실행.
- [x] 스냅샷 기반 공개 표시 보완; 누락된 과거 정보는 임의로 현재 양식에서 가져오지 않는다.
- [x] 다시 선택 시 세션의 기존 결정을 모델에 복원. 만료·다른 세션·미제출 완료 접근의 기존 검증 유지.
- [x] 완료 화면에 제목·제출시각·항목별 결과·철회 안내를 안전한 동일 세션 범위에서 표시. 연락처를 임의 생성하지 않는다.
- [x] 관련 단위·컨트롤러 테스트 및 self-review 결과를 docs/reviews/ux-public-implementation.md에 기록.

## Task 2: 통계·대시보드 (보고서 2, 11)

소유: statistics Java/mapper/JSP/CSS/JS, admin home controller/JSP 및 해당 테스트.

- [x] 권리요청 기한초과 1건이 상태별 총계에 더해지지 않는 테스트, 증적 검증 이력 분리 테스트부터 실행.
- [x] 도넛은 상호배타적 상태만 합산; 경고 지표와 기간 기준을 명시. 기존 조직 범위를 보존.
- [x] 대시보드 고정 정상/구축완료 문구 제거; 권한에 맞는 현재 처리할 일 및 필터된 목록 링크 제공. 이미 존재하는 조회 계층 재사용.
- [x] 관련 테스트 및 self-review 결과를 docs/reviews/ux-statistics-implementation.md에 기록.

## Task 3: 관리자 조회 맥락 (보고서 3, 6, 7, 9, 10 중 서버 조회)

소유: request/delivery/evidence/retention/rights Java와 mapper; requests/list.jsp, requests/detail.jsp, mail/list.jsp, rights/detail.jsp, retention/detail.jsp, evidence/detail.jsp 및 관련 테스트. 공통 JS/CSS는 변경하지 않음.

- [x] 기존 서비스 권한·마스킹 경계를 확인하고 수신자/동의서 요약 및 검색 테스트 작성.
- [x] 요청·메일 목록에 마스킹 대상과 동의서/제목을 표시하고 유용한 q 검색 제공. query 파라미터명 q.
- [x] 요청의 완료 증적 링크는 해당 요청에 범위 제한된 증적 상세로 이동하며 미생성 상태 안내.
- [x] 파기 상세에 대상 요약·한글 결과·담당자 표시. 실행 폼에 data-confirm-title와 data-confirm-message를 넣어 주 에이전트 공통 확인 동작과 연결.
- [x] 권리요청 상세에 대상·요청·접수일·기한 및 한글 업무 설명 표시.
- [x] 증적 상세는 사용자 요약 우선, 기술정보는 details로 접음. 안전한 PDF 직접 제공은 기존 다운로드 권한·경로 검증을 그대로 활용할 수 있을 때만 구현.
- [x] 관련 테스트 및 self-review 결과를 docs/reviews/ux-admin-implementation.md에 기록.

## Task 4: 공통 UI·입력 (보고서 4, 5, 8, 10 일부, 12, 13, 15)

소유: app.css, admin-data.css, admin-ui.js, pagination.jsp, policy/activity detail JSP, template form JS/JSP, recipient import preview, retention/list.jsp, 공통 표현 helper 및 관련 테스트.

- [x] 닫힌 메뉴 포커스 제외·복귀와 확인 취소/실행 동작 테스트를 작성하고 실패를 확인.
- [x] 모바일 1열 복원; inert와 데스크톱 전환 정리; destructive 확인 dialog와 중복 submit 방지/복원.
- [x] pagination은 collectionBasis와 q 등 실제 검색 조건 보존; 검색 초기화 링크 제공.
- [x] 정책·처리업무 상태 버튼에 서버와 동일 권한 guard.
- [x] HTML 입력을 안전한 간단 작성 도구로 보완; 필드별 오류 연결과 미저장 이탈 확인. 새 편집기 의존성 없음.
- [x] Excel 오류를 행별 한국어 안내로 바꾸고 빈 값과 파기 상태를 분리.
- [x] 보존 보류 요청 선택을 기존 권한 범위 내 검색/선택으로 보완하고 코드 대신 한글 안내.

## 통합 검증

- [x] 각 작업의 focused test 결과와 구현 diff 검토 (관련 중요 문제만 1회 수정/재검토).
- [x] Java 관련 전체 suite 1회, JavaScript 동작 테스트, JSP 컴파일/로컬 서버 응답 확인.
- [x] 가능하면 실제 브라우저 데스크톱·모바일 대표 화면 확인. 불가하면 제한을 명시.
- [x] 보고서 16항목별 완료/제한 내역 기록, 서버 실행물에 반영 후 health check.

## 진행 기록

- 계획 간 공유 파일은 위 소유권으로 분리. 공통 확인 인터페이스는 `data-confirm-title`, `data-confirm-message`; 공통 검색 키는 `q`.
- 기존 승인된 보고서를 설계 근거로 사용하며 재승인 단계는 만들지 않음. 사용자 지시의 위험 기반 절차에 따라 과도한 다단계 리뷰/커밋은 생략.

- 반영/부분 반영 및 검증 제한은 docs/reviews/2026-09-06-ux-remediation-result.md에 기록. 실서버 인증 화면 전체 클릭 검증과 선택적 확대 기능은 완료로 간주하지 않음.
