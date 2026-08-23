# 2단계 동의서 템플릿 설계 기준서

**문서 버전:** v1.1  
**상태:** 설계 검수 승인  
**확정 기준:** `docs/superpowers/specs/2026-08-21-consent-system-full-build-design.md`  
**범위:** 구조화된 동의서 템플릿 작성, 검토, 승인, 발행, 폐기와 관리자 미리보기

## 1. 목표

승인된 개인정보 처리업무 버전을 바탕으로 동의서 템플릿을 작성하고, 작성자와 승인자를 분리한 검토 절차를 거쳐 불변 발행 버전을 만든다. 발행 버전의 구조화 데이터는 다음 단계의 요청 스냅샷, HTML 이메일과 정보주체 모바일 웹 동의서가 함께 사용하는 단일 원본이다.

이번 단계는 템플릿을 발행하는 데서 끝난다. 대상자 등록, 요청 생성, 일회용 링크, 이메일 발송과 정보주체 제출은 3~5단계에서 구현한다.

## 2. 선택한 접근

### 2.1 구조화 데이터 중심

템플릿 전체를 자유 형식 HTML 한 덩어리로 저장하지 않는다. 제목, 안내문과 동의 결정 단위를 분리하고 각 결정 단위에 목적, 개인정보 항목, 보유기간, 거부권, 제3자 제공받는 자를 구조화해 저장한다. 안내문만 제한된 HTML을 허용한다.

이 방식은 단순 HTML 편집기보다 입력 항목이 많지만 필수 고지 누락을 서버에서 검증할 수 있고, 이메일과 모바일 화면이 같은 데이터를 사용하며, 향후 요청 스냅샷과 증적 JSON을 안정적으로 만들 수 있다.

### 2.2 처리업무 승인 버전 고정

템플릿 버전은 논리 처리업무가 아니라 `processing_activity_version`의 승인 버전을 참조한다. 처리업무의 목적, 법적 근거, 개인정보 항목과 보유정책이 바뀌어도 이미 발행된 템플릿의 기준은 변하지 않는다.

동의 템플릿은 `legal_basis_type = CONSENT`인 승인 처리업무 버전에서만 만들 수 있다. 다른 적법 근거로 처리하는 업무는 이번 동의 템플릿에 포함하지 않는다.

### 2.3 서버 렌더링

JSP 폼과 서버 측 HTML 미리보기를 사용한다. 별도 SPA, jQuery와 프런트엔드 빌드 도구를 추가하지 않는다. 동적 항목 행 추가·삭제는 네이티브 JavaScript로 구현하되 모든 검증은 서버에서 다시 수행한다.

## 3. 업무 객체와 데이터 모델

### 3.1 `consent_template`

논리 템플릿을 나타낸다.

- `consent_template_id`: UUID 기본키
- `template_code`: 기관 내 유일 코드, 영문 대문자·숫자·하이픈 2~30자
- `template_name`: 관리자 식별용 명칭 1~100자
- `processing_activity_id`: 템플릿이 속한 논리 처리업무
- `template_status`: `ACTIVE` 또는 `INACTIVE`
- `current_published_version_id`: 현재 발행 버전, 발행 전에는 NULL
- 생성·수정 관리자와 시각
- `version_no`: 낙관적 잠금 번호

한 처리업무에 여러 템플릿을 둘 수 있다. 프로그램이나 모집 차수별 안내 문구가 다를 수 있기 때문이다.

### 3.2 `consent_template_version`

검토와 발행의 단위다.

- `consent_template_version_id`: UUID 기본키
- `consent_template_id`: 논리 템플릿
- `version_number`: 템플릿 내 1부터 증가
- `processing_activity_id`: 논리 템플릿과 처리업무 버전의 일치를 보장하는 FK 구성 컬럼
- `processing_activity_version_id`: 승인된 처리업무 버전
- `workflow_status`: `DRAFT`, `REVIEW_REQUESTED`, `APPROVED`, `PUBLISHED`, `RETIRED`
- `title`: 정보주체에게 표시할 제목 5~200자
- `introduction_html`: 정제된 안내문 HTML, 정제 후 텍스트 10~4000자
- 검토요청자·시각, 승인자·시각, 발행자·시각
- 반려 의견 1~1000자
- `content_hash`: 발행할 때 계산한 SHA-256 소문자 64자리, 발행 전에는 NULL
- 생성·수정 관리자와 시각
- `version_no`: 낙관적 잠금 번호

### 3.3 `consent_item_definition`

정보주체가 각각 동의 또는 미동의를 결정하는 단위다.

- `consent_item_definition_id`: UUID 기본키
- `consent_template_version_id`: 소속 템플릿 버전
- `processing_activity_version_id`: 템플릿 버전과 개인정보 항목의 동일 처리업무 버전을 보장하는 FK 구성 컬럼
- `item_order`: 버전 안에서 1부터 연속된 표시 순서
- `item_code`: 버전 안에서 유일한 영문 대문자·숫자·하이픈 2~30자
- `item_type`: `COLLECTION_USE`, `THIRD_PARTY_PROVISION`, `OPTIONAL`
- `item_title`: 항목 제목 5~200자
- `required`: 업무 제공에 필수인 결정인지 여부
- `purpose_text`: 목적 5~1000자
- `retention_text`: 정보주체 표시용 보유·이용기간 5~1000자
- `refusal_notice`: 동의 거부권과 거부 시 불이익 안내 10~1000자
- `third_party_recipient`: 제3자 제공 항목의 제공받는 자 1~500자, 다른 유형은 NULL

`OPTIONAL` 항목은 `required = false`여야 한다. `THIRD_PARTY_PROVISION`은 제공받는 자가 필수이며 다른 유형에는 제공받는 자를 저장하지 않는다.

`COLLECTION_USE`와 `OPTIONAL`의 `retention_text`는 관리자가 임의 입력하지 않고 연결된 승인 보유정책 버전에서 서버가 생성해 저장한다. `THIRD_PARTY_PROVISION`은 제공받는 자의 보유·이용기간이 기관 보유정책과 다를 수 있으므로 관리자가 구조화 입력한다. 검토 요청과 발행 때 전자의 문구를 승인 보유정책에서 다시 생성해 불일치를 차단한다.

### 3.4 `consent_item_data_selection`

동의 결정 단위와 처리업무의 일반 개인정보 항목을 연결한다.

- `consent_item_definition_id`
- `processing_activity_version_id`
- `processing_data_item_id`
- 복합 기본키

각 동의 항목은 개인정보 항목을 하나 이상 선택해야 한다. 선택 항목은 템플릿 버전이 참조하는 동일한 처리업무 버전에 속해야 한다. 민감정보와 고유식별정보는 처리업무 단계에서 허용되지 않으므로 여기에서도 선택할 수 없다.

DB에서도 교차 업무 연결을 막는다. `consent_template_version`은 `(consent_template_id, processing_activity_id)`와 `(processing_activity_version_id, processing_activity_id)` 복합 FK를 사용한다. `consent_item_definition`과 `consent_item_data_selection`은 `processing_activity_version_id`를 함께 저장하고 각각 템플릿 버전 및 `processing_data_item`과의 복합 FK를 사용한다. 이를 위해 참조 대상에는 대응 UNIQUE 제약을 추가한다.

### 3.5 `template_review`

검토 과정의 추가 기록이다.

- `template_review_id`: UUID 기본키
- `consent_template_version_id`
- `review_action`: `REQUESTED`, `APPROVED`, `REJECTED`, `PUBLISHED`, `RETIRED`
- `actor_id`, `comment`, `created_at`

버전 행의 현재 상태와 별도로 모든 검토·발행 행위를 보존한다. 애플리케이션 계정은 이 테이블을 UPDATE 또는 DELETE할 수 없다.

## 4. 상태와 불변 규칙

```text
DRAFT → REVIEW_REQUESTED → APPROVED → PUBLISHED → RETIRED
   ↑             │
   └── REJECTED ─┘
```

- 초안만 내용과 항목을 수정할 수 있다.
- 초안은 동의 항목이 하나 이상이고 모든 서버 검증을 통과해야 검토 요청할 수 있다.
- 검토 요청된 버전만 승인 또는 반려할 수 있다.
- 작성자와 승인자는 달라야 한다.
- 승인된 버전만 발행할 수 있다.
- 발행 시 같은 논리 템플릿의 이전 `PUBLISHED` 버전을 `RETIRED`로 바꾸고 새 버전을 현재 발행 버전으로 지정한다.
- `APPROVED`, `PUBLISHED`, `RETIRED` 버전의 내용과 동의 항목은 수정할 수 없다.
- 반려 시 상태를 `DRAFT`로 되돌리고 반려 의견을 보존한다.
- 발행된 템플릿을 변경하려면 현재 발행 버전을 복제한 다음 번호의 새 `DRAFT` 버전을 만든다.
- 이미 발행된 버전은 삭제하지 않는다.

## 5. 필수 검증

검토 요청과 발행 직전에 다음 조건을 모두 서버에서 검사한다.

1. 연결된 처리업무와 처리업무 버전이 활성·승인 상태다.
2. 처리업무 버전의 법적 근거가 `CONSENT`다.
3. 연결된 보유정책 버전이 여전히 `APPROVED` 상태다.
4. 제목과 안내문이 길이 제한을 만족한다.
5. 동의 결정 항목이 하나 이상이다.
6. 항목 순서는 1부터 중복 없이 연속된다.
7. 모든 항목은 목적, 보유기간, 거부권 안내와 일반 개인정보 항목을 가진다.
8. `OPTIONAL`은 필수로 표시할 수 없다.
9. `THIRD_PARTY_PROVISION`은 제공받는 자가 있고 다른 유형은 제공받는 자가 없다.
10. 선택한 개인정보 항목이 참조 처리업무 버전에 속한다.
11. 항목 코드가 버전 안에서 중복되지 않는다.
12. 승인 또는 발행 시 DB에서 처리업무·보유정책 상태를 다시 조회해 오래된 화면의 제출을 차단한다.
13. 논리 템플릿이 `ACTIVE` 상태다.

## 6. HTML 정제와 콘텐츠 해시

`introduction_html`은 OWASP Java HTML Sanitizer로 저장 전에 정제한다. 허용 태그는 `p`, `br`, `ul`, `ol`, `li`, `strong`, `em`, `h3`, `h4`이고 속성, 외부 이미지, 스크립트, 스타일, iframe과 링크는 허용하지 않는다. 정제 결과의 텍스트가 비어 있거나 10자를 충족하지 않으면 거부한다.

발행할 때 다음 값을 명시된 순서와 UTF-8로 정규화한 canonical JSON을 만들고 SHA-256을 계산한다. 문자열의 줄바꿈은 LF로 통일하고 입력 검증에서 정의한 앞뒤 공백 제거 결과를 사용한다. Jackson은 객체 속성과 Map 키 정렬을 활성화하며 목록은 아래 업무 순서를 그대로 유지한다.

- 템플릿 코드와 버전 번호
- 처리업무 승인 버전 ID
- 제목과 정제된 안내문 HTML
- 표시 순서대로 정렬한 모든 동의 항목 필드
- 각 항목에서 이름순이 아닌 처리업무 항목 순서로 정렬한 개인정보 항목 ID와 명칭

`content_hash`는 발행 이후 바뀌지 않는다. 향후 요청 스냅샷은 이 해시와 발행 버전 ID를 함께 저장한다.

## 7. 권한과 데이터 범위

다음 작업 권한을 추가한다.

- `CONSENT_TEMPLATE_VIEW`: 목록, 상세, 버전, 미리보기 조회
- `CONSENT_TEMPLATE_EDIT`: 템플릿과 초안 버전 생성·수정, 발행 버전에서 새 초안 복제
- `CONSENT_TEMPLATE_REVIEW`: 검토 요청
- `CONSENT_TEMPLATE_APPROVE`: 승인·반려
- `CONSENT_TEMPLATE_PUBLISH`: 발행과 논리 템플릿 활성·비활성화

모든 접근은 권한 외에 연결된 처리업무의 조직·처리업무 범위를 검사한다. 권한이 없으면 403, 권한은 있지만 객체 범위 밖이면 404로 응답한다. 시스템 관리자 역할에는 새 권한을 추가하되 역할 관리 화면을 통해 개인정보 보호책임 업무담당자와 부서 관리자에게 필요한 조합을 배정할 수 있다.

## 8. 관리자 화면과 URL

### 8.1 화면

- 템플릿 목록: 코드, 명칭, 처리업무, 현재 발행 버전, 상태와 검색 조건
- 템플릿 생성: 승인 처리업무 선택, 코드, 명칭, 첫 버전 제목·안내문·동의 항목
- 템플릿 상세: 논리 템플릿 정보와 전체 버전 이력
- 템플릿 기본정보 수정: 관리자 식별용 명칭과 활성 상태 변경. 템플릿 코드와 연결 처리업무는 생성 후 변경 금지
- 초안 편집: 안내문과 구조화 동의 항목 편집
- 미리보기: 이메일/모바일이 공유할 읽기 전용 서버 렌더링 조각
- 검토 화면: 검증 결과와 승인·반려 작업

### 8.2 URL

- `GET /admin/templates`
- `GET /admin/templates/new`
- `POST /admin/templates`
- `GET /admin/templates/{templateId}`
- `GET /admin/templates/{templateId}/edit`
- `POST /admin/templates/{templateId}`
- `POST /admin/templates/{templateId}/status`
- `GET /admin/templates/{templateId}/versions/{versionId}/edit`
- `POST /admin/templates/{templateId}/versions/{versionId}`
- `GET /admin/templates/{templateId}/versions/{versionId}/preview`
- `POST /admin/templates/{templateId}/versions/{versionId}/request-review`
- `POST /admin/templates/{templateId}/versions/{versionId}/approve`
- `POST /admin/templates/{templateId}/versions/{versionId}/reject`
- `POST /admin/templates/{templateId}/versions/{versionId}/publish`
- `POST /admin/templates/{templateId}/versions`

모든 변경 요청은 Spring Security CSRF 보호와 POST-Redirect-GET을 사용한다.

새 버전 생성 요청은 `sourceVersionId`를 필수로 받고 해당 템플릿의 가장 최근 `PUBLISHED` 또는 `RETIRED` 버전을 복제한다. 서버가 다음 버전 번호를 계산하며 동시에 두 요청이 들어오면 유일 제약과 행 잠금으로 하나만 성공시킨다.

## 9. 오류와 동시성

- 폼 검증 오류는 입력값을 보존하고 필드별 한국어 메시지를 표시한다.
- 허용되지 않은 상태 전이는 409 업무 충돌로 처리하고 사용자에게 최신 상태를 다시 확인하도록 안내한다.
- 낙관적 잠금 실패도 409로 처리한다.
- 존재하지 않거나 범위 밖인 객체는 404로 처리한다.
- DB 유일 제약 위반은 코드 중복 메시지로 변환한다.
- 승인·발행 트랜잭션은 대상 템플릿과 버전 행을 잠그고 검증, 상태 변경, 검토 이력, 감사 이벤트를 한 트랜잭션에서 저장한다.

## 10. 감사 이벤트

성공·실패·거부 결과에 대해 다음 작업 코드를 사용한다.

- `CONSENT_TEMPLATE_CREATE`
- `CONSENT_TEMPLATE_UPDATE`
- `CONSENT_TEMPLATE_VERSION_CREATE`
- `CONSENT_TEMPLATE_REVIEW_REQUEST`
- `CONSENT_TEMPLATE_APPROVE`
- `CONSENT_TEMPLATE_REJECT`
- `CONSENT_TEMPLATE_PUBLISH`

감사 메타데이터에는 템플릿·버전 ID, 상태와 변경 전후 해시만 포함한다. 제목, 안내문, 개인정보 항목 원문과 반려 의견은 감사 메타데이터에 복제하지 않는다.

미리보기는 개인정보를 포함하지 않는 일반 읽기 작업이므로 별도 감사 이벤트를 만들지 않는다. 권한 거부와 범위 거부는 공통 보안 감사 정책에 따라 기록한다.

## 11. DB 변경과 라이브러리

- DB 패치: `database/03_patch/20260824_001_create_consent_template_tables.sql`
- 검증 SQL: `database/04_verify/005_consent_template_verify.sql`
- 적용 SQL은 수정하지 않고 SHA-256과 애플리케이션 버전을 `db_change_history`에 기록한다.
- `seal_app`에는 템플릿 본문 테이블 SELECT·INSERT·UPDATE만 허용하고 DELETE·TRUNCATE·DDL은 허용하지 않는다.
- `template_review`는 SELECT·INSERT만 허용한다.
- HTML 정제는 `org.owasp.html:owasp-java-html-sanitizer`를 사용한다.
- SHA-256과 canonical JSON 직렬화는 JDK `MessageDigest`와 현재 Spring Boot Jackson을 사용한다.

## 12. 테스트와 인수 기준

### 12.1 자동 테스트

- 도메인: 상태 전이, 작성자·승인자 분리, 항목 유형별 검증, 발행 불변성
- 애플리케이션: 승인 처리업무 재검증, 이전 발행 버전 폐기, 해시 결정성, 감사 이벤트
- 웹 MVC: 권한별 200/302/403, 범위 밖 404, CSRF 거부, 폼 오류 보존
- PostgreSQL: 외래키·CHECK·UNIQUE, 최소 권한, UPDATE/DELETE 거부, 동시 발행
- HTML: XSS 문자열 제거, 허용 태그 유지, 미리보기 출력 이스케이프

### 12.2 완료 조건

1. 승인된 `CONSENT` 처리업무로 템플릿과 초안을 생성할 수 있다.
2. 누락되거나 잘못된 동의 항목은 검토 요청할 수 없다.
3. 작성자가 자신의 버전을 승인할 수 없다.
4. 승인 버전을 발행하면 이전 발행 버전은 폐기되고 현재 버전 포인터가 바뀐다.
5. 발행된 내용과 항목은 수정할 수 없다.
6. 미리보기에 이메일과 모바일에서 재사용할 구조화 내용이 표시된다.
7. 권한·범위·CSRF·낙관적 잠금과 감사기록이 적용된다.
8. SQL 검증, 전체 Maven 테스트, WAR 패키징과 9090 대표 화면 검증이 통과한다.

## 13. 제외 범위

- 대상자 및 Excel 반입
- 요청 스냅샷과 일회용 링크
- 실제 이메일 HTML과 SMTP 발송
- 정보주체 동의·미동의 제출
- PDF·JSON 증적, 권리요청과 파기
- 민감정보, 고유식별정보와 만 14세 미만 동의
- 전자서명, OTP, SMS, 본인확인과 메일 열람 추적
