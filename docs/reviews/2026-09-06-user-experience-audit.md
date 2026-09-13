# 사용자 관점 프로젝트 검수 — 2026-09-06

## 결론

색상·폰트 전면 교체보다 **동의 정보 누락, 통계 집계, 파기 대상 확인, 검색·이동 흐름**을 먼저 수정해야 합니다. 기존 파란 톤, 약 90%의 화면 밀도, 가용 폭 전체 사용 방향은 유지하는 것을 권장합니다.

이번 검수에서는 기능과 업무 데이터를 변경하지 않았습니다.

## 범위와 검증 한계

- 실행 작업 트리: `C:/dev/workspace-egov/seal/.worktrees/runtime-main`.
- JSP 62개를 포함하는 화면 구성을 파악하고, 주요 관리자 메뉴·공통 CSS/JS·수신자 동의 흐름·관련 컨트롤러와 조회 SQL을 검토했습니다. 모든 권한 조합과 실제 데이터 상태를 실행 검증했다는 의미는 아닙니다.
- 관리자 로그인 URL의 HTTP 200 응답을 확인했습니다.
- 브라우저 자동화가 실행 환경의 sandbox 초기화 오류로 연결되지 않아, 실제 화면 클릭·모바일 캡처·접근성 트리 검증은 수행하지 못했습니다. 아래는 코드로 확인한 문제 및 그에 따른 사용성 제안입니다.
- 메일 발송, 동의 제출, 승인, 파기 등 데이터가 변경되는 작업은 실행하지 않았습니다. PDF를 새로 생성하거나 출력물 전체를 재검증하지 않았습니다.
- UI/UX Pro Max와 [Web Interface Guidelines](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md)의 정보 명확성·오류 복구·파괴적 작업 확인·접근성 기준을 적용했습니다. 과거 스크린샷만으로 이미 수정된 문제를 다시 지적하지 않았습니다.

## 우선 수정할 문제

### 1. [높음 · 정보 누락] 실제 동의 화면에 개인정보 항목과 제공받는 자가 없습니다

관리자 미리보기에는 `개인정보 항목`과 `제공받는 자`가 표시되지만, 실제 수신자의 응답 화면에는 목적·보유기간·거부 안내만 표시됩니다. 공개 응답용 데이터 모델에도 해당 필드가 없습니다. 안내문에 사람이 별도로 적지 않으면 구조화된 항목이 전달되지 않습니다.

- 사용자 영향: 어떤 개인정보를 누구에게 제공하는지 확인하지 못한 채 동의할 수 있습니다.
- 수정 방향: 확정된 동의 내용의 개인정보 항목·제공받는 자를 실제 응답 화면에도 표시하고, 관리자 미리보기와 수신자 화면의 정보 일치 여부를 검증합니다.
- 근거: [실제 응답 화면](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/consent/respond.jsp:2), [미리보기](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/fragments/consent/template-content.jsp:4), [공개 항목 모델](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/java/kr/or/arte/seal/consent/application/PublicConsentItem.java:3).

### 2. [높음 · 데이터 표출] 통계의 총 건수와 도넛 비율에 중복이 포함됩니다

권리요청의 현재 상태별 건수에 `기한 초과` 건수를 별도로 추가하고, 화면은 이를 모두 더해 총 건수로 표시합니다. 예를 들어 검토 중이면서 기한을 넘긴 요청 1건은 두 범주에 포함됩니다. 증적도 패키지 상태 건수와 검증 실패 이력 건수를 같은 총계에 합산합니다.

- 사용자 영향: 실제 요청·증적 수보다 총 건수가 커지고 도넛 차트의 분모도 업무 건수와 달라집니다.
- 수정 방향: 서로 겹치지 않는 현재 상태만 도넛으로 표시합니다. 기한 초과·검증 실패는 별도 경고 지표로 분리하고, 기간이 생성일 기준인지 처리일 기준인지 명시합니다.
- 근거: [권리요청 추가 집계](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/resources/mybatis/mappers/statistics/StatisticsMapper.xml:59), [검증 이력 집계](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/resources/mybatis/mappers/statistics/StatisticsMapper.xml:46), [화면 총계](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/statistics/index.jsp:49).

### 3. [높음 · 실수 방지] 파기 상세에서 누구의 정보를 파기하는지 확인하기 어렵습니다

상세 화면은 작업 UUID·상태·날짜·처리 건수 중심이며, 대상 수신자와 동의서 제목이 없습니다. 권한과 승인 상태에 따른 실행 제한은 있지만, 실행 버튼을 누르는 마지막 순간 대상·삭제 범위를 확인하는 단계가 없습니다.

- 사용자 영향: 목록에서 잘못 들어왔거나 여러 탭을 열었을 때 다른 대상을 파기할 위험이 있습니다.
- 수정 방향: 상단에 마스킹된 수신자 이름·이메일, 동의서명, 요청번호, 파기 방식, 예정일을 표시합니다. 실행 직전 대상과 삭제 범위·복구 제한을 보여주고 명시적으로 확인받습니다. 기존 승인 절차는 유지합니다.
- 근거: [파기 상세](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp:18), [실행 폼](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp:32).

### 4. [높음 · 반응형] 모바일 1열 레이아웃이 뒤의 공통 CSS에 덮어써집니다

900px 이하에서 `grid-template-columns: 1fr`를 선언하지만, 뒤에 있는 같은 선택자의 `208px minmax(0, 1fr)`가 다시 적용됩니다. 이 크기에서는 메뉴가 fixed로 빠지므로 본문이 의도한 전체 폭을 사용하지 못하는 구조입니다.

- 사용자 영향: 작은 창·태블릿·모바일에서 본문 폭이 비정상적으로 좁아질 수 있습니다.
- 수정 방향: 데스크톱 열 정의를 해당 미디어 조건 안으로 제한하거나, 마지막 모바일 규칙에서 1열을 보장합니다. 390px·768px·데스크톱에서 확인합니다.
- 근거: [모바일 선언](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/resources/css/app.css:588), [뒤에서 덮어쓰는 선언](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/resources/css/app.css:843).

## 업무 흐름·검색 개선

### 5. [중간 · 기능] 다음 페이지로 가면 등록 근거 검색 조건이 빠집니다

수신자 검색은 `collectionBasis`를 받지만 공통 페이지 링크는 page·status·email만 전달합니다.

- 수정 방향: 모든 활성 검색 조건을 이전/다음 링크에 유지하고, 검색 실행 시 첫 페이지로 이동하도록 통일합니다.
- 확인 시나리오: 등록 근거 검색 결과를 20건 이상 준비 → 다음 페이지 → URL과 결과 범위에서 조건 유지 확인.
- 근거: [검색 필드](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/recipients/list.jsp:34), [페이지 링크](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/fragments/admin/pagination.jsp:8).

### 6. [중간 · 식별] 동의 요청·메일 목록만으로는 대상을 찾기 어렵습니다

동의 요청 목록은 요청번호·상태·만료일시·생성일시만, 메일 목록은 제목·상태·예약일시·시도 횟수만 표시합니다. 동일 양식을 여러 명에게 보냈을 때 개별 대상을 구분하기 어렵습니다.

- 수정 방향: 수신자 이름·이메일을 마스킹하여 표시하고 동의서 제목을 함께 보여줍니다. 요청번호·이메일 일부·동의서명 검색을 제공합니다. 요청번호는 보조 식별자로 유지합니다.
- 근거: [동의 요청 목록](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/requests/list.jsp:22), [메일 목록](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/mail/list.jsp:3).

### 7. [중간 · 이동] ‘완료 동의 증적 보기’가 해당 증적이 아닌 전체 목록으로 이동합니다

- 사용자 영향: 방금 확인한 요청의 증적을 다시 찾아야 합니다.
- 수정 방향: 해당 요청의 증적 상세로 연결합니다. 생성 중이면 생성 상태를 보여주고, 아직 없으면 그 이유를 안내합니다.
- 근거: [동의 요청 상세](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/requests/detail.jsp:11).

### 8. [중간 · 권한 UX] 누를 수 없는 검토·승인 버튼이 표시됩니다

보유정책·처리업무 상세의 버튼은 버전 상태만으로 표시합니다. 서버는 별도 검토·승인 권한을 검사하므로 조회만 가능한 사용자는 버튼을 누른 뒤 거절될 수 있습니다. 이는 서버 권한 우회 문제가 아니라 화면과 권한의 불일치입니다.

- 수정 방향: 서버 검증을 유지하면서 화면도 동일 권한으로 제어합니다. 필요한 경우 ‘승인 권한이 있는 담당자가 처리할 수 있습니다’라고 안내합니다.
- 근거: [보유정책 버튼](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/retention-policies/detail.jsp:9), [승인 권한 검사](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/java/kr/or/arte/seal/policy/web/RetentionPolicyController.java:87).

### 9. [중간 · 맥락] 권리요청 상세에 대상자와 처리기한이 보이지 않습니다

접수 화면에서는 대상자·연결 요청·접수일·처리기한을 입력하지만 상세는 요청코드·유형·상태·메모와 처리 버튼 중심입니다.

- 수정 방향: 대상자, 동의서/연결 요청, 접수일, 처리기한, 남은 일수 또는 기한 초과를 상단 요약으로 제공합니다. ‘암호화 상세정보’는 ‘요청 내용’으로 바꿉니다.
- 근거: [접수 폼](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/rights/form.jsp:6), [상세](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/rights/detail.jsp:2).

## 문구·입력·정보 계층 개선

### 10. [중간 · 이해도] 내부 코드와 기술 정보가 일반 업무 정보보다 앞에 나옵니다

- 파기 상세: 요청자·승인자가 UUID, 개인정보 처리 결과가 `PII true/false`로 출력됩니다.
- 파기 목록: 파기 방식 문자열을 그대로 출력합니다. 조회 SQL도 DB 코드를 그대로 가져옵니다.
- 보존 보류 등록: 동의요청 UUID·사유 코드를 직접 입력해야 합니다.
- 증적 상세: 패키지 UUID·HMAC·해시·저장 경로가 큰 비중을 차지하고, 다운로드도 ZIP 중심입니다.

수정 방향은 ‘담당자 이름 / 개인정보 삭제 완료·미삭제 / 한글 파기 방식 / 요청 검색 선택’입니다. 증적은 동의서·수신자·응답일·결과를 먼저 보여주고, 해시와 저장 경로는 기술정보 접기에 보존합니다. 권한 확인을 유지한 PDF 바로 보기·다운로드도 검토할 만합니다.

- 근거: [파기 결과](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp:26), [보존 보류 입력](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/retention/list.jsp:102), [증적 정보](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/evidence/detail.jsp:43).

### 11. [중간 · 대시보드] ‘구축 완료’보다 ‘지금 처리할 일’이 필요합니다

정상 운영 배지는 고정 문구이고, 주요 카드도 기능 구축 완료 설명입니다. DB 계정·BCrypt 등 구현 설명이 업무 우선순위를 대신합니다.

- 수정 방향: 승인 대기, 발송 실패, 미응답, 처리기한 초과, 파기 대기 건수와 해당 목록 링크를 제공합니다. 고정 ‘정상 운영’은 실제 점검 결과와 연결하거나 제거합니다.
- 근거: [대시보드](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/home.jsp:23).

### 12. [중간 · 입력 UX] 동의서 작성이 HTML 지식에 의존하고 오류 위치가 넓습니다

안내문은 HTML 태그 목록을 보여주는 textarea입니다. 동의 항목 내부 검증 오류도 항목 전체 단위로 출력하고, 공통 JS는 그 영역의 모든 입력을 오류 상태로 처리합니다. 사용자는 어떤 입력을 고쳐야 하는지 다시 찾아야 합니다. 확인한 클라이언트 스크립트에는 미저장 변경의 이탈 보호도 없습니다.

- 수정 방향: 문단·줄바꿈·목록·굵게 정도의 간단 편집 기능과 미리보기를 제공합니다. 오류는 정확한 필드 아래에 연결하고, 내용 변경 후 이탈 시 확인합니다. 사용자 요청대로 레이아웃을 밀어내는 큰 오류 박스는 다시 도입하지 않습니다.
- 근거: [안내문·항목 입력](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/templates/form.jsp:11), [오류 연결 처리](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/resources/js/admin-ui.js:83).

### 13. [중간 · 오류 복구] Excel 검증 결과가 오류 코드 중심입니다

행별 결과에 `errorCodes()`가 그대로 표시되며, 등록 근거가 비어 있으면 처리 상태와 관계없이 ‘파기 완료’로 표시합니다. 누락된 입력과 파기된 정보를 구분하기 어렵습니다.

- 수정 방향: ‘이메일 형식을 확인해 주세요’처럼 수정할 항목과 방법을 보여주고 기술 코드는 보조로 둡니다. ‘미입력’과 ‘파기 완료’는 실제 상태로 구분합니다. 오류 행을 수정해 재등록할 흐름을 안내합니다.
- 근거: [Excel 검증 결과](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/admin/recipients/import-preview.jsp:36).

## 수신자 경험·접근성

### 14. [중간 · 응답 수정] 최종 확인에서 ‘다시 선택’을 누르면 기존 선택이 복원되지 않습니다

확인 화면에는 다시 선택 링크가 있지만, 응답 화면 GET은 저장된 선택을 모델에 전달하지 않고 라디오 버튼도 checked 값을 렌더링하지 않습니다.

- 수정 방향: 기존 선택을 유지한 상태로 돌아가 필요한 항목만 수정하게 합니다.
- 근거: [공개 응답 컨트롤러](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/java/kr/or/arte/seal/consent/web/PublicConsentController.java:12), [응답 입력](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/consent/respond.jsp:2).

### 15. [중간 · 접근성] 닫힌 모바일 메뉴의 링크가 키보드 탐색에 남습니다

메뉴를 transform으로 화면 밖으로 옮길 뿐, 닫힌 상태의 링크를 inert/hidden 등으로 제외하지 않습니다.

- 사용자 영향: Tab 탐색 시 보이지 않는 메뉴로 포커스가 이동할 수 있습니다.
- 수정 방향: 닫힌 모바일 메뉴는 탐색에서 제외하고, 열면 메뉴로 진입·닫으면 버튼으로 복귀하게 합니다. 데스크톱 전환 시 제한을 해제합니다.
- 근거: [메뉴 닫기 처리](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/resources/js/admin-ui.js:20), [화면 밖 배치](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/resources/css/app.css:909).

### 16. [개선 · 제출 후 안내] 완료 화면에서 무엇을 언제 제출했는지 확인할 수 없습니다

현재는 제출 완료라는 일반 문구만 표시합니다. 만료·사용 불가 화면도 담당 부서에 문의하라고만 안내합니다.

- 수정 방향: 완료 화면에 동의서 제목·제출일시·항목별 결과를 제공하고, 변경 또는 철회 문의 방법을 안내합니다. 담당 연락처는 설정된 경우에만 표시합니다. 완료 링크 재방문 시 개인정보를 노출하지 않는 현재의 보안 경계는 유지합니다.
- 근거: [완료 화면](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/consent/complete.jsp:2), [만료 안내](C:/dev/workspace-egov/seal/.worktrees/runtime-main/src/main/webapp/WEB-INF/jsp/consent/entry.jsp:2).

## 유지할 점과 디자인 방향

- 일시 표시의 공통 포맷 함수, 이름·이메일 마스킹, 표 가로 스크롤 래퍼, 주요 상태 한글화는 유지합니다.
- 동의서 코드 자동 생성, 처리업무 항목 선택, 보유정책의 미응답 대체 기준 설명은 사용자 입력 부담을 줄이는 방향입니다.
- 파란색은 일반 주동작, 경고색은 파기 같은 위험 동작에 제한합니다. 로고·폰트·전체 크기를 다시 전면 변경하는 것은 이번 우선 과제가 아닙니다.
- 상세 화면의 상단에는 ‘대상·업무·상태·중요 날짜’, 다음에는 내용, 마지막에는 이력·기술정보를 둡니다.
- 검색 결과 없음과 아직 한 건도 등록되지 않음을 구분하고, 필터 초기화·총 결과 건수를 공통 제공하는 것이 좋습니다.
- 도움말 i 아이콘은 시각적 크기를 키우지 않더라도 클릭·터치 영역을 넓히는 방향으로 정리합니다.

## 권장 수정 순서와 인수 확인

1. **정보 정확성·실수 방지:** 1~4번. 실제 동의와 미리보기 대조, 통계 중복 사례, 파기 전 확인, 반응형 폭을 확인합니다.
2. **일상 업무 흐름:** 5~10번. 검색 다음 페이지, 관련 증적 이동, 조회 전용 계정, 권리요청 기한, 내부 코드 한글화를 확인합니다.
3. **입력·운영 편의:** 11~16번. 대시보드, 입력 오류, Excel 복구, 선택 복원, 키보드 탐색, 제출 후 안내를 확인합니다.

실제 브라우저 연결이 복구되면 데스크톱과 모바일 각 1개 대표 크기에서 확인하고, 조회 전용/승인 가능 계정 및 빈 목록/다건 목록/오류 상태를 집중 검증하는 것이 다음 단계입니다. 이번 결과만으로 전체 시스템의 실행 검증이나 출시 승인을 완료한 것으로 보아서는 안 됩니다.
