# Phase 3B~5 대상자 일괄반입·공개 동의·이메일 설계

**상태:** 승인됨  
**승인일:** 2026-08-24  
**상위 기준:** `2026-08-21-consent-system-full-build-design.md` v2.3  
**선행 구현:** Phase 3A 대상자·동의 요청·일회용 링크

## 1. 범위

이번 묶음은 세 개의 독립 모듈을 순서대로 연결한다.

1. Phase 3B: `.xlsx` 대상자 일괄반입, 행별 검증, 확인 반입, 임시 개인정보 파기
2. Phase 4: 링크 스캐너에 안전한 공개 진입, 모바일 항목 결정, 확인, 멱등 최종 제출
3. Phase 5: 요청 스냅샷 기반 HTML 이메일, 암호화 Outbox, 예약·재시도·재발송, 상태 조회

전자서명, SMS·알림톡, OTP, 실명 본인확인, 추적 픽셀, 민감정보·고유식별정보, 만 14세 미만은 계속 제외한다. 이메일 본문에는 입력 폼을 넣지 않고 기관 웹페이지로만 연결한다.

## 2. Phase 3B Excel 반입

### 2.1 입력과 제한

- 형식은 `.xlsx`만 허용하고 최대 5 MiB, 최대 데이터 1,000행으로 제한한다.
- 첫 행의 필수 헤더는 `이름`, `이메일`이며 알 수 없는 추가 열은 거부한다.
- 조직과 수집 근거는 업로드 화면에서 배치 공통값으로 받는다.
- 이름은 1~100자, 이메일은 기존 개별 등록과 같은 정규화·검증 규칙을 사용한다.
- 파일 확장자, ZIP 시그니처, POI 통합문서 파싱을 모두 확인한다. 수식은 실행하지 않고 표시 문자열로도 받지 않는다.

### 2.2 악성코드 검사

`UploadMalwareScanner` 경계 뒤에 ClamAV `INSTREAM` 연결을 둔다. 로컬 기본값은 `seal.clamav.enabled=false`이며 화면에 검사 비활성 상태를 표시한다. 운영에서 활성화했는데 연결 또는 검사에 실패하면 업로드 전체를 거부한다. 감염 판정 파일은 파싱하지 않는다.

### 2.3 미리보기와 반입

업로드 파일은 디스크나 DB에 보관하지 않는다. 파싱한 정상 행만 기존 AES-256-GCM과 이메일 HMAC으로 암호화해 `recipient_import_row`에 최대 24시간 임시 저장한다. 오류 행은 원문 대신 행 번호, 오류 코드와 마스킹 값만 저장한다.

배치는 `VALIDATED → IMPORTED` 또는 `EXPIRED`로 진행한다. 미리보기에는 정상·중복·오류 건수와 행별 마스킹 결과만 표시한다. 확인 POST는 배치 행을 잠그고 정상·비중복 행을 `recipient.collection_source='EXCEL'`로 저장한 뒤 임시 암호문을 지운다. 동일 배치 재확인은 기존 결과를 반환한다. 24시간이 지난 미확정 배치는 스케줄러가 암호문을 지우고 `EXPIRED`로 바꾼다.

## 3. Phase 4 공개 동의

### 3.1 스캐너 안전 진입

- `GET /consent/{token}`은 HMAC으로 유효성만 확인하며 링크 상태를 변경하지 않는다.
- 첫 화면은 기관명, 유효/만료/완료 여부와 `동의서 확인` 버튼만 표시하고 개인정보나 동의 항목을 표시하지 않는다.
- `POST /consent/{token}/start`는 CSRF를 검증하고 링크 행을 잠근 뒤 `ACCESSED`로 전환한다.
- 서버는 256비트 공개 흐름 식별자의 HMAC만 `public_consent_session`에 저장하고 원문은 HttpOnly 세션에 연결한다.
- 이후 URL은 토큰이 없는 `/consent/respond/{flowId}`를 사용한다. 흐름 식별자는 현재 HTTP 세션과 DB HMAC이 모두 일치해야 하며 30분 후 만료한다.

### 3.2 결정과 제출

공개 화면은 확정 스냅샷과 `consent_request_item`만 읽는다. 이름·이메일은 표시하지 않는다. 모든 항목에 `AGREE` 또는 `DISAGREE`가 필요하고 기본 선택 및 일괄동의는 제공하지 않는다.

첫 POST는 선택값을 서버 세션에 보관하고 확인 화면으로 리다이렉트한다. 최종 POST는 숨은 항목값을 신뢰하지 않고 서버 세션 값과 DB 항목 집합을 다시 검증한다. 요청 행을 잠근 단일 트랜잭션에서 `consent_response`, `consent_response_item`, 요청 `COMPLETED`, 링크 `COMPLETED`, 요청 이벤트와 감사 이벤트를 저장한다. 제출 멱등키는 요청별 유일하며 같은 키의 재요청은 기존 완료 결과를 반환한다. 완료 응답은 수정하지 않는다.

### 3.3 웹 보안

공개 POST에도 Spring Security CSRF를 유지한다. 공개 화면은 자체 CSS만 사용하고 `Content-Security-Policy: default-src 'self'; object-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`를 적용한다. 오류 화면은 토큰·요청번호·개인정보를 출력하지 않는다.

## 4. Phase 5 HTML 이메일과 Outbox

### 4.1 단일 원본과 암호화 저장

메일 렌더러는 `consent_request_snapshot` JSON과 대상자 연락처를 입력으로 사용한다. 제목, 기관명, 목적, 구조화 항목, 보유기간, 거부권, 만료일과 공개 링크 버튼을 포함하되 폼, JavaScript, 외부 이미지와 추적 픽셀은 넣지 않는다.

수신 이메일과 렌더링 HTML은 AES-256-GCM으로 암호화해 `mail_outbox`에 저장한다. 제목은 개인정보 없이 요청 코드 기반으로 저장한다. HTML SHA-256을 함께 저장해 재시도 시 내용이 바뀌지 않음을 확인한다. 토큰 원문은 `secure_link`에는 저장하지 않으며 암호화 Outbox 본문 안에서만 발송 완료 또는 보유정책까지 제한적으로 존재한다.

### 4.2 발송 상태와 재시도

메일은 `QUEUED → PROCESSING → SMTP_ACCEPTED`, 일시 오류는 `RETRY_SCHEDULED`, 영구 오류 또는 3회 실패는 `SEND_FAILED`, 링크 재발급으로 아직 보내지 않은 이전 작업은 `CANCELLED`로 진행한다. 작업 선점은 `FOR UPDATE SKIP LOCKED`, 처리 중 장애 복구는 10분 임대 만료를 사용한다. 재시도 간격은 1분, 5분, 30분이다.

`seal.mail.enabled=false`인 로컬에서는 Outbox를 생성하지만 스케줄러가 SMTP를 호출하지 않는다. 관리자 화면은 이를 `발송 비활성화(로컬)`로 표시하고, 권한이 있는 사용자에게 암호화 HTML 미리보기를 제공한다. 활성화 시 `JavaMailSender`를 통해 SSL 465, 무인증, 환경변수 발신주소로 전송한다. `SMTP_ACCEPTED`는 릴레이 접수일 뿐 최종 수신이나 열람을 뜻하지 않는다.

### 4.3 요청과의 연결

READY 요청의 `메일 발송`은 링크 발급과 Outbox 생성을 같은 트랜잭션에서 수행한다. ACTIVE 요청의 `링크 재발급·메일 재발송`은 이전 링크와 미발송 Outbox를 폐기한 뒤 새 링크와 새 Outbox를 생성한다. 요청, 링크, 메일, 응답 상태는 각각 별도 열과 화면으로 표시한다.

## 5. 권한과 감사

- `RECIPIENT_IMPORT`: 양식 다운로드, 업로드, 확인 반입
- `CONSENT_PUBLIC`은 관리자 권한이 아니라 공개 Security 경로 식별용이다.
- `MAIL_SEND`: 링크 발급과 메일 Outbox 생성
- `MAIL_VIEW`: 마스킹 목록·상세와 로컬 HTML 미리보기
- `MAIL_RETRY`: 실패 메일 수동 재대기

배치 업로드·확인·만료, 링크 접근, 결정 제출, Outbox 생성·접수·재시도·실패를 감사한다. 감사 메타데이터에는 배치·요청·메일 UUID, 상태, 건수와 이유 코드만 넣고 이름, 이메일, HTML, 토큰·HMAC·암호문은 넣지 않는다.

## 6. DB와 라이브러리

- 수동 패치 `20260824_004`와 검증 SQL `008`을 추가한다.
- 테이블: `recipient_import_batch`, `recipient_import_row`, `public_consent_session`, `consent_response`, `consent_response_item`, `mail_outbox`, `mail_delivery_attempt`.
- Apache POI `poi-ooxml`을 추가한다. ClamAV 프로토콜은 JDK 소켓으로 구현해 별도 클라이언트 의존성을 추가하지 않는다.
- 메일은 이미 포함된 `spring-boot-starter-mail`을 사용한다.

## 7. 완료 기준

- Excel 정상·오류·중복·재확인·만료와 실제 PostgreSQL 반입 검증
- GET 무상태, POST 접근 전환, 모든 항목 검증, 멱등·동시 제출 차단 검증
- 이메일 HTML 단일 원본, 암호화 저장, 로컬 비발송, SMTP 성공·일시·영구 실패 재시도 검증
- 권한거부, 조직 범위, CSRF, 마스킹, 비밀정보 로그 미포함 검증
- 전체 테스트, WAR 패키징, DB 검증, 9090 데스크톱·360px 브라우저 스모크 통과

