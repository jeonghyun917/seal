# 관리자 조회 맥락 구현 기록

## 구현 결과

- 동의 요청 목록은 기존 조직 범위 안에서 `q`로 요청번호·동의서 제목·양식명을 검색하고, 기존 수신자 조회 서비스가 제공하는 마스킹 이름/이메일과 동의서명을 표시한다.
- 메일 목록은 같은 범위 안에서 `q`로 메일 제목·요청번호·동의서 제목을 검색하고, 마스킹 대상자와 요청번호를 함께 표시한다.
- 완료 요청의 증적 링크는 요청 ID로 조직 범위가 검증된 최신 증적 하나를 찾아 상세로 이동한다. 아직 없으면 요청 상세에 안내한다.
- 파기 상세는 마스킹 대상자·요청번호, 담당자, 한국어 파기 결과를 표시하며 실행/확인 폼에 `data-confirm-title`과 `data-confirm-message`를 제공한다.
- 권리요청 상세는 마스킹 대상자, 한국어 요청 유형, 접수일·처리기한, 연결 요청과 요청 내용을 우선 표시한다.
- 증적 상세는 동의서/요청/상태 사용자 요약을 먼저 표시하고 경로·해시·파일 목록은 `details`에 접었다.
- 보존 화면에 `GET /admin/retention/request-options?q=&page=` 검색 API를 제공한다. 응답은 `AdminPageResult`이며 각 항목은 `requestId`, `requestCode`, `recipientLabel`, `templateName`이다.
- 파기 상세 담당자 이름은 파기 대상 요청의 `visible_scope`가 확인되는 동일 retention mapper 조회에서 관리자 표시명을 조인한다.
- 파기 확인 문구에는 서버에서 안전하게 출력한 마스킹 이름·이메일과 요청번호를 포함한다. 파기 전에는 결과 카운터 대신 미실행 상태를 표시한다.

## 권한·개인정보 검토

- 검색 SQL은 기존 `visible_scope` CTE와 결합된 상태에서만 실행된다.
- 요청·메일 목록 검색은 상세 조회와 동일한 `user_processing_activity_scope` 조건을 LIMIT/OFFSET 전에 적용한다. 같은 조직의 허용되지 않은 처리업무 행은 페이지와 보존 요청 선택 API에 들어오지 않는다.
- 이름과 이메일 검색/표시는 암호문을 SQL에서 직접 취급하지 않고 기존 `RecipientQueryService`의 마스킹 결과를 사용한다.
- 증적의 요청별 조회도 `visible_scope`를 통과하며 기존 상세/다운로드 권한은 변경하지 않았다.
- 실제 메일 발송, 승인, 파기, 증적 재생성 동작은 수행하지 않았다.

## 검증 증거

- RED: 신규 request/mail `q` 위임 테스트가 누락된 5-인자 API 때문에 컴파일 실패하는 것을 확인했다. 당시 동시 작업 중이던 통계 테스트의 별도 컴파일 실패도 함께 존재했다.
- GREEN: `./mvnw.cmd -Dtest=AdminContextJspTest,ConsentRequestQueryServiceTest,ConsentRequestControllerTest,MailOutboxQueryServiceTest,MailOutboxControllerTest,EvidenceQueryServiceTest,MyBatisEvidenceRepositoryTest,RetentionDetailJspTest,RightsRequestControllerTest test` — 25 tests, 0 failures/errors, BUILD SUCCESS.
- 컴파일/패키징: `./mvnw.cmd -DskipTests package` — BUILD SUCCESS.
- 모든 Maven 실행은 `Local\\SealUxMaven` 뮤텍스로 직렬화했다.
- 활동 범위 회귀: 로컬 PostgreSQL에서 같은 조직의 허용/비허용 처리업무 요청·메일·파기 작업·보존 보류를 함께 생성한 뒤 제한 관리자의 목록/상세 조회에 허용 행만 포함됨을 확인했다. 테스트 1건, 0 failures/errors, BUILD SUCCESS.

## 변경 파일

- request: `ConsentRequestRepository`, `ConsentRequestQueryService`, MyBatis repository/mapper/XML, controller, list/detail JSP, query/JSP tests
- delivery: `MailOutboxRepository`, `MailOutboxQueryService`, MyBatis repository/mapper/XML, controller, list JSP, query test
- evidence: repository/service/MyBatis mapper/XML/controller, detail JSP
- retention: controller, detail JSP
- rights: controller, detail JSP

## 제한

- 암호화된 수신자 이름/이메일은 개인정보 보호와 인덱스 우회를 피하기 위해 `q` 검색 대상에서 제외했다. 검색 후 목록에는 마스킹 값이 표시된다.
- PDF 단독 제공은 ZIP 다운로드의 기존 무결성 검증과 안전 경로 검증을 그대로 재사용할 명확한 단일 파일 API가 없어 추가하지 않았다.
- 브라우저 실서버 검증과 전체 테스트 suite는 통합 단계의 주 에이전트가 수행한다.
