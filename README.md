# 개인정보 전자동의·증적 관리시스템

eGovFrame 5.0 기반 JSP 모듈형 모놀리스 프로젝트다. HTML 이메일은 개인정보 동의 내용을 안내하며, 동의·미동의 선택과 최종 제출은 기관 웹페이지의 일회용 보안 링크에서 처리한다.

## 확정 기술

| 구분 | 기준 |
|---|---|
| Java | 17 |
| eGovFrame | Runtime Maven artifacts 5.0.0 |
| Spring | Boot 3.5.6 / Framework 6.2.11 |
| View | JSP / JSTL |
| Persistence | MyBatis 3.5 계열 |
| DB | PostgreSQL 16.14 |
| Build | Maven Wrapper 3.3.4 / Maven 3.9.11 |
| Deploy | 단일 WAR, Tomcat 10.1 |
| Mail | `relay.govkorea.com:465`, SSL/TLS, 무인증 |

공식 eGovFrame 5.0 실행환경은 Java 17 이상을 지원한다. 프로젝트에는 공식 Maven 저장소에 실제 게시된 5.0.0 런타임 모듈을 사용한다.

## 로컬 준비

Java 17만 시스템에 설치하면 Maven은 Wrapper가 자동으로 준비한다.

```powershell
.\mvnw.cmd -version
```

PostgreSQL이 다른 Rocky Linux 장비에 있다면 `localhost` 대신 해당 장비의 내부 주소를 사용한다. DB를 외부 인터넷에 공개하지 않는다.

```powershell
$env:SEAL_DB_URL = 'jdbc:postgresql://DB내부주소:5432/seal_dev?currentSchema=seal'
$env:SEAL_DB_USERNAME = 'seal_app'
$env:SEAL_DB_PASSWORD = '로컬에서만 입력'
$env:SEAL_DATA_ACTIVE_KEY_ID = 'v1'
$env:SEAL_DATA_KEYS_FILE = 'C:\tmp\seal-keys\data.keys'
$env:SEAL_EMAIL_INDEX_HMAC_KEY_FILE = 'C:\tmp\seal-keys\email-index.key'
$env:SEAL_LINK_HMAC_KEY_FILE = 'C:\tmp\seal-keys\link-hmac.key'
```

개인정보 AES-256-GCM 키, 이메일 정확 검색 HMAC 키, 일회용 링크 HMAC 키는 서로 다른 32바이트 난수로 생성하고 저장소 밖의 접근 제한 파일로 관리한다. 키 원문과 `.env`는 커밋하지 않는다. `data.keys`는 `v1=<32바이트 Base64>` 형식이며 나머지 두 파일은 원시 32바이트 키 파일이다.

DB 생성과 초기 스키마 적용은 [database/README.md](database/README.md)를 따른다.

### DB 역할 분리

- `seal_owner`: 로그인할 수 없는 객체 소유자다. PostgreSQL 관리자가 스키마 변경 때만 `SET ROLE seal_owner`로 사용한다.
- `seal_app`: 애플리케이션 실행 계정이다. 필요한 조회·등록·수정 권한만 가지며 스키마 변경과 테이블 삭제는 할 수 없다.
- `audit_event`에는 `seal_app`의 조회·등록만 허용한다.

### 최초 관리자 생성

초기 SQL 적용 후 `admin_user`가 비어 있을 때만 아래 세 값을 모두 설정한다.

```powershell
$env:SEAL_BOOTSTRAP_ADMIN_ID = 'seal-admin'
$env:SEAL_BOOTSTRAP_ADMIN_NAME = '최초 관리자'
$env:SEAL_BOOTSTRAP_ADMIN_PASSWORD = '12자 이상의 임시 비밀번호'
```

한 명이라도 관리자가 있으면 추가 생성하지 않는다. 최초 로그인 후 비밀번호 변경 화면으로 강제 이동하며, 변경이 끝나면 세션을 종료하고 새 비밀번호로 다시 로그인한다. 초기 생성이 끝나면 세 환경변수를 실행 환경에서 제거한다.

로그인 실패 잠금값은 운영 설정이며 법령에 정해진 숫자가 아니다. 기본값은 5회·15분이고 환경변수로 조정할 수 있다.

```powershell
$env:SEAL_ADMIN_MAX_LOGIN_FAILURES = '5'
$env:SEAL_ADMIN_LOCK_DURATION = '15m'
```

## 테스트와 실행

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

브라우저 확인:

- `http://localhost:9090/`
- `http://localhost:9090/system/status`
- `http://localhost:9090/login`
- `http://localhost:9090/admin`
- `http://localhost:9090/admin/templates`
- `http://localhost:9090/admin/templates/new`
- `http://localhost:9090/admin/recipients`
- `http://localhost:9090/admin/recipients/new`
- `http://localhost:9090/admin/recipients/import`
- `http://localhost:9090/admin/requests`
- `http://localhost:9090/admin/requests/new`
- `http://localhost:9090/admin/mail-outbox`
- `http://localhost:9090/admin/evidence-packages`
- `http://localhost:9090/admin/rights-requests`
- `http://localhost:9090/admin/retention-jobs`
- `http://localhost:9090/admin/statistics`
- `http://localhost:9090/actuator/health`

로컬 프로필에서는 실제 메일 발송이 비활성화된다. SMTP 테스트는 Rocky Linux 공인 IP 등록 후 승인된 수신 주소로만 수행한다.

## 패키징

```powershell
.\mvnw.cmd clean package
```

산출물은 `target/seal.war`이다. 운영 배포는 [ops/rocky/README.md](ops/rocky/README.md)를 따른다.

## 보안 주의

- `.env`, DB 비밀번호, 관리자 비밀번호, SMTP 관련 비밀정보를 커밋하지 않는다.
- Spring 기본 인메모리 사용자는 사용하지 않는다. 관리자 인증은 PostgreSQL `admin_user`와 BCrypt strength 12를 사용한다.
- 역할 권한과 조직 범위는 서버에서 다시 검사한다. 외부 관리자 접속은 방화벽 허용 IP나 기관 VPN 같은 네트워크 통제를 함께 적용한다.
- 기본 5회 실패·15분 잠금은 변경 가능한 보안 운영값이며 법정 숫자로 표현하지 않는다.
- SMTP 접수 성공은 수신자의 최종 수신이나 열람을 의미하지 않는다.
- 토큰 원문, 비밀번호, 동의서 개인정보를 애플리케이션 로그에 기록하지 않는다.

## Phase 1A 검증 범위

로컬에서 단위·MVC 테스트, Spring Security DB Provider 구성, WAR 패키징과 공개/로그인 페이지 HTTP 스모크를 검증한다. 다음 항목은 PostgreSQL 16.14의 `seal_test` 또는 `seal_dev`에 초기 SQL을 적용한 뒤 별도로 통과해야 한다.

- `database/04_verify/001_phase1a_verify.sql`
- `seal_app`으로 실제 관리자 조회·로그인·실패 잠금·비밀번호 변경
- `seal_app`의 스키마 변경·DELETE 거부와 감사 테이블 UPDATE·DELETE 거부

실제 SMTP 발송은 공인 IP 허용과 발신주소 확인이 완료될 때까지 로컬 검증 범위에서 제외한다.

## Phase 2 동의서 템플릿

승인된 처리업무 버전을 기준으로 구조화된 동의 항목을 작성하고 검토·승인·발행한다. 안내문 HTML은 허용 태그만 서버에서 정제하며, 이메일과 공개 웹 동의서는 발행 버전의 동일한 구조화 데이터를 사용한다.

상태 흐름은 `DRAFT → REVIEW_REQUESTED → APPROVED → PUBLISHED → RETIRED`이며, 반려 시 `DRAFT`로 돌아간다. 작성자와 승인자는 달라야 하고 발행된 버전은 수정할 수 없다.

| 권한 | 허용 기능 |
|---|---|
| `CONSENT_TEMPLATE_VIEW` | 목록·상세·미리보기 조회 |
| `CONSENT_TEMPLATE_EDIT` | 초안 생성·수정·다음 버전 복제 |
| `CONSENT_TEMPLATE_REVIEW` | 검토 요청 |
| `CONSENT_TEMPLATE_APPROVE` | 승인·반려 |
| `CONSENT_TEMPLATE_PUBLISH` | 발행·폐기 |

역할 권한 외에도 현재 활성 계정의 조직·처리업무 범위를 서버와 DB에서 함께 검사한다. HTML 이메일 실발송, 외부 HTTPS와 일회용 링크 응답 수집은 후속 단계 범위다.

### Phase 2 검증 결과

- PostgreSQL 패치·검증: `20260824_001`, `20260824_002`, 검증 SQL `005`, `006`
- 전체 자동화 테스트: 138건, 실패·오류·건너뜀 0건
- 실행 산출물: `target/seal.war`
- 로컬 실행 포트: 9090

## Phase 3A 대상자·동의 요청

대상자 이름과 이메일은 AES-256-GCM으로 암호화하고, 이메일 정확 검색은 정규화 값의 HMAC으로 처리한다. 목록·상세·수정 화면에는 마스킹 값만 표시하며 수정하지 않은 개인정보는 화면에 원문으로 다시 노출하지 않는다.

동의 요청은 `DRAFT → READY → ACTIVE`로 진행한다. 초안 확정 시 발행 템플릿, 처리업무, 보유정책과 동의 항목을 개인정보 없는 canonical JSON 스냅샷으로 고정하고 SHA-256 해시를 저장한다. 취소와 만료는 종료 상태이며, 취소·만료 때 현재 링크도 함께 폐기한다.

일회용 링크 토큰은 32바이트 난수의 패딩 없는 Base64URL 문자열이다. 토큰 원문은 발급 직후 관리자 화면에 한 번만 표시하고 DB·감사 이력·URL 쿼리·로그에는 저장하지 않는다. DB에는 별도 키의 HMAC-SHA256만 저장하며 재발급하면 이전 링크는 즉시 `REVOKED`가 된다.

| 권한 | 허용 기능 |
|---|---|
| `RECIPIENT_VIEW` | 대상자 마스킹 목록·상세 조회와 정확 이메일 검색 |
| `RECIPIENT_EDIT` | 대상자 등록·정정·활성 상태 변경 |
| `CONSENT_REQUEST_VIEW` | 요청 목록·상세·확정 스냅샷 조회 |
| `CONSENT_REQUEST_EDIT` | 요청 초안 생성·수정·확정 |
| `CONSENT_REQUEST_ACTIVATE` | 최초 보안 링크 발급 |
| `CONSENT_REQUEST_CANCEL` | 진행 중 요청 취소 |
| `CONSENT_REQUEST_LINK_REISSUE` | 활성 요청의 링크 재발급 |

Phase 3A는 개별 대상자 등록과 요청·링크 기반을 제공한다. Excel 일괄 반입, 공개 링크 동의 제출, HTML 이메일·Outbox는 아래 Phase 3B~5에서 이어진다. 증적·철회·파기는 6단계에서 구현한다.

### Phase 3A 검증 결과

- PostgreSQL 패치·검증: `20260824_003`, 검증 SQL `007` (`seal_test`, `seal_dev` 통과)
- 전체 자동화 테스트: 171건, 실패·오류·건너뜀 0건
- 실행 산출물: `target/seal.war`
- 로컬 실행 포트: 9090

## Phase 3B~5 일괄반입·공개 동의·HTML 이메일

Excel 반입은 `.xlsx` 5 MiB·1,000행으로 제한하며 이름·이메일 두 열을 행별 검증한다. 원본 파일은 저장하지 않고 검증 행만 AES-256-GCM으로 최대 24시간 임시 보관한다. 확정 시 정상 행을 `EXCEL` 출처 대상자로 생성하고 정상·중복 행의 임시 암호문을 즉시 파기한다. 로컬 ClamAV는 기본 비활성이고, 운영 활성화 상태에서 검사 서버 연결 또는 판정에 실패하면 반입을 거부한다.

공개 동의는 링크 GET에서 상태를 변경하지 않는다. 사용자가 CSRF 보호 POST로 시작한 뒤 토큰 없는 흐름 URL에서 모든 항목을 각각 동의·미동의로 선택하고 최종 제출한다. 제출은 PostgreSQL 단일 트랜잭션과 멱등키로 요청·링크·세션·응답·감사를 한 번만 완료한다. 이 링크 보유 방식은 실명 본인확인이 아니다.

HTML 이메일은 확정 요청 스냅샷을 웹 화면과 함께 단일 원본으로 사용한다. 이메일 본문에는 입력 폼·스크립트·외부 이미지·추적 픽셀이 없고 기관 웹페이지 버튼만 포함한다. 수신주소와 HTML은 암호화 Outbox에 저장하며 `QUEUED`, `PROCESSING`, `SMTP_ACCEPTED`, `RETRY_SCHEDULED`, `SEND_FAILED`, `CANCELLED` 상태를 분리한다. 일시 오류는 1·5·30분 간격으로 최대 3회 재시도한다.

| 권한 | 허용 기능 |
|---|---|
| `RECIPIENT_IMPORT` | Excel 양식 다운로드·검증·확정 반입 |
| `MAIL_SEND` | 링크 발급과 HTML 메일 Outbox 등록·재발송 |
| `MAIL_VIEW` | 메일 상태·상세·권한 제한 HTML 미리보기 |
| `MAIL_RETRY` | 영구 실패 메일의 관리자 재시도 등록 |

### Phase 3B~5 검증 결과

- PostgreSQL 패치·검증: `20260824_004`, 검증 SQL `008` (`seal_test`, `seal_dev` 통과)
- 전체 자동화 테스트: 194건, 실패·오류·건너뜀 0건
- 실제 PostgreSQL 통합 흐름: Excel 암호화 반입·즉시 파기, 공개 GET 무상태·POST 완료·멱등, 링크 재발급·암호화 Outbox 취소/재등록
- 로컬 SMTP: `SEAL_MAIL_ENABLED=false`로 외부 발송 없이 대기열·HTML 미리보기만 검증
- 실행 산출물과 포트: `target/seal.war`, 9090

## Phase 6 증적·권리요청·보유기간 수명주기

동의 완료 트랜잭션은 증적 생성 작업을 한 번만 등록한다. 내부 워커는 `evidence.pdf`, 요청·결정·이벤트 JSON, canonical 매니페스트와 HMAC-SHA256을 개인정보가 없는 패키지로 생성한다. 관리자는 권한과 조직 범위 안에서 패키지를 조회·재검증하며, 다운로드 직전 HMAC과 파일 해시가 모두 유효할 때만 `no-store` ZIP을 받을 수 있다.

권리요청은 기관 외부 절차에서 본인확인을 마친 건만 관리자가 접수한다. 열람·정정·삭제·처리정지·철회의 상태와 암호화 메모를 관리하고 등록자와 결정자를 분리한다. 삭제는 긴급 파기 작업이 `CONFIRMED`된 뒤 완료되고, 처리정지는 보존 보류를 설정하며, 반려된 요청은 실제 개인정보 동작 없이 통지 완료만 기록한다.

보유기간 작업은 승인 정책과 기산일로 예정일을 계산한다. 프로그램 종료일·목적 달성일은 기존 값을 삭제하지 않는 버전 이력으로 등록하고, 법적·업무상 보존 보류는 근거와 만료일을 기록해 설정·해제한다. 파기는 요청·승인·실행·확인 행위자를 분리하고 같은 멱등키의 재실행만 허용한다. 현재 애플리케이션 파기 범위는 운영 주 DB의 암호화 메일·직접 식별자와 주 증적 저장소이며 백업본·물리 매체는 포함하지 않는다.

통계는 최근 30일을 기본으로 최대 1년 동안 조직 범위 내 요청, 응답·철회, 메일, 증적·검증 실패, 권리요청·기한 초과, 파기·보존 보류 건수만 제공한다. 이름·이메일·대상자 UUID·자유 입력 메모는 통계 DTO와 화면에 포함하지 않는다.

Phase 6 로컬 설정에는 서로 분리된 증적 HMAC 키가 추가된다.

```powershell
$env:SEAL_EVIDENCE_ROOT = 'C:\dev\workspace-egov\seal\.local-data\evidence'
$env:SEAL_EVIDENCE_HMAC_KEY_FILE = 'C:\tmp\seal-keys\evidence-hmac.key'
$env:SEAL_EVIDENCE_WORKER_ENABLED = 'true'
```

`SEAL_EVIDENCE_HMAC_KEY_FILE`은 저장소 밖의 32바이트 난수 파일이어야 한다. `.local-data`, 키 파일과 생성 증적은 Git에 포함하지 않는다. Phase 6 권한 13개는 SQL이 역할에 자동 부여하지 않으므로 역할 관리 화면에서 업무분장에 맞게 명시적으로 부여한다.

### Phase 6 검증 결과

- PostgreSQL 패치: `20260824_005`, 기존 완료 응답 증적 큐 보강 `20260824_006`
- 검증 SQL: `009`, `010` (`seal_test`, `seal_dev` 통과)
- 전체 자동화 테스트: 222건, 실패·오류·건너뜀 0건
- 실제 PostgreSQL 통합 흐름: 증적 큐·권리요청 암호화/상태·기산일 버전·보존 보류·이중 통제 파기·범위 제한 통계
- 실행 산출물과 포트: `target/seal.war`, 9090
- 미검증/제외: Rocky Linux, 백업·복구와 백업본 파기, 운영 HTTPS·도메인·Nginx, 실제 SMTP 발송
