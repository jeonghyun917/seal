# Phase 6 증적·권리요청·수명주기 설계 기준서

**기준일:** 2026-08-24

**상위 기준:** `docs/superpowers/specs/2026-08-21-consent-system-full-build-design.md`

**선행 구현:** Phase 1~5, 커밋 `205124bf8c27f507da15535c1f49a61252fc2883`

## 1. 목표와 범위

Phase 6은 완료된 동의 응답을 변경하지 않으면서 다음 애플리케이션 기능을 추가한다.

1. PDF·JSON·매니페스트·HMAC으로 구성된 최소 증적 패키지
2. 권한이 제한된 증적 조회·다운로드와 무결성 재검증
3. 기관 외부 절차로 본인 확인을 마친 철회·권리요청의 관리자 처리
4. 승인 보유정책에 따른 기산일·만료일 계산과 파기 작업
5. 보존 보류, 파기 승인·실행·확인의 이력과 상호 분리
6. 개인정보가 없는 운영 현황 통계

Phase 6은 `6A 증적 → 6B 권리요청 → 6C 보존·파기·통계` 순서로 구현한다. 세 증분은 하나의 기능 브랜치에서 통합하고 각 증분마다 DB 검증과 자동화 테스트를 수행한다.

## 2. 제외 범위

다음 항목은 Phase 7 운영 인수 때 처리하며 Phase 6 완료 조건에 포함하지 않는다.

- Rocky Linux 설치 및 파일 권한 검증
- DB·증적 파일의 백업·복구·롤백 훈련
- 백업본·스냅샷·물리 매체의 파기 확인
- 운영 HTTPS, 도메인, Nginx와 실제 SMTP 발송 인수
- 외부 전자문서관리시스템, 오브젝트 스토리지, Redis, Kafka와 KMS
- 전자서명, OTP, PASS, 휴대전화 본인확인과 공개 권리요청 셀프서비스

애플리케이션 파일 삭제는 `주 저장소 파기`로 기록한다. 백업본과 물리 매체까지 완전히 삭제했다는 표현은 사용하지 않는다.

## 3. 공통 원칙

- 기존 `consent_response`와 `consent_response_item`은 UPDATE·DELETE하지 않는다.
- 증적과 철회는 원응답을 덮어쓰지 않는 별도 불변 이력이다.
- 관리자 URL 권한과 조직·처리업무 데이터 범위를 서버와 SQL에서 모두 검사한다.
- 이름, 이메일, 암호문, 토큰, 권리요청 상세와 증적 본문은 로그·감사 메타데이터에 기록하지 않는다.
- 목록과 통계는 직접 식별자를 표시하지 않는다.
- 작업 재시도는 UUID 멱등키와 행 잠금으로 중복 생성·실행을 차단한다.
- 모든 상태 변경 POST는 Spring Security CSRF 보호를 유지한다.
- DB 패치는 `20260824_005`, 검증 SQL은 `009`로 추가하며 적용된 기존 SQL을 수정하지 않는다.

## 4. 모듈과 의존 방향

### 4.1 evidence

- `evidence.application`: 생성 요청, 패키지 조립, 검증, 다운로드 정책
- `evidence.domain`: 상태, 매니페스트와 파일 종류
- `evidence.persistence`: MyBatis 조회·잠금·상태 저장
- `evidence.infrastructure`: PDF 렌더링, 로컬 파일 저장, HMAC
- `evidence.web`: 관리자 목록·상세·다운로드·검증·재시도

`consent`는 `EvidenceQueue` application 인터페이스만 호출한다. `evidence` Mapper를 직접 호출하지 않는다. 동의 최종 제출 트랜잭션에서 증적 작업 `QUEUED` 행을 함께 생성하되 실제 파일 생성은 별도 워커가 수행한다.

### 4.2 rights

- `rights.application`: 접수, 외부 본인확인 기록, 검토·결정·완료
- `rights.domain`: 유형·상태 전이·처리기한
- `rights.persistence`: 암호화 상세와 상태 이력
- `rights.web`: 관리자 목록·등록·상세·처리 화면

삭제 요청은 개인정보를 직접 삭제하지 않고 `RetentionJobRequestPort`로 긴급 파기 검토 작업을 생성한다. 철회 완료는 별도 `consent_withdrawal` 불변 이력을 생성한다.

### 4.3 retention

- `retention.application`: 기산일 기록, 만료 계산, 작업 생성, 보존 보류와 파기 수명주기
- `retention.domain`: 기산 이벤트, 상태 전이, 이중 통제 규칙
- `retention.persistence`: 만료 검색, 행 잠금과 개인정보 제거 SQL
- `retention.web`: 작업 목록·상세·보류·승인·실행·확인

### 4.4 statistics

통계는 별도 저장 집계 테이블을 만들지 않는다. PostgreSQL의 범위 제한 집계 쿼리로 요청·응답·메일·증적·권리요청·파기 상태 수를 계산한다. 외부 차트 라이브러리 없이 JSP, HTML, CSS로 카드와 표를 표시한다.

## 5. 증적 패키지

### 5.1 구성

완료된 요청마다 다음 파일을 생성한다.

| 파일 | 내용 |
|---|---|
| `evidence.pdf` | 기관, 처리업무, 요청 UUID, 완료시각, 항목별 결정, 보유정책과 무결성 식별자 |
| `request.json` | 고정 요청 스냅샷과 스냅샷 SHA-256 |
| `decisions.json` | 응답 UUID, 상태, 제출시각과 항목별 결정 |
| `events.json` | 요청·링크·제출의 주요 상태 이벤트 |
| `manifest.json` | 패키지 버전, 파일명, 파일별 SHA-256, 생성기·글꼴·템플릿 버전 |
| `manifest.hmac` | canonical `manifest.json`의 HMAC-SHA-256 Base64URL 값 |

PDF와 JSON에는 이름·이메일을 넣지 않는다. 대상자는 내부 `recipient_id`와 `request_id`로만 연결한다. 이 방식은 전자서명이나 제3자 타임스탬프가 아니며 시스템 내부 무결성 검증이다.

### 5.2 파일 저장

- 설정 키: `SEAL_EVIDENCE_ROOT`, 로컬 기본값 `target/seal-evidence`
- HMAC 키: `SEAL_EVIDENCE_HMAC_KEY_FILE`, 기존 데이터·이메일·링크 키와 다른 32바이트 파일
- 경로: `<root>/<organization-id>/<request-id>/<package-id>/`
- 파일명에는 개인정보를 사용하지 않는다.
- 같은 루트 아래 UUID 임시 디렉터리에 모두 생성하고 파일 `fsync` 후 최종 디렉터리로 원자 이동한다.
- DB에는 루트 기준 상대 경로, 크기, SHA-256, HMAC과 생성기 버전만 저장한다.
- 경로 정규화 결과가 설정 루트를 벗어나면 요청을 거부한다.

### 5.3 생성 상태

```text
QUEUED → PROCESSING → FINALIZED
              └────→ FAILED → QUEUED (권한 있는 재시도)
FINALIZED → DESTROYED (승인된 파일 파기 작업만 가능)
```

- 같은 `response_id`에는 활성 증적 패키지를 하나만 허용한다.
- 10분 이상 `PROCESSING`인 작업은 임대 만료 후 다시 가져올 수 있다.
- 재시도는 동일 행의 시도 횟수를 증가시키고 임시 디렉터리를 새로 만든다.
- `FINALIZED` 이후 파일 목록·해시·HMAC은 수정하지 않는다.

### 5.4 PDF와 글꼴

- `io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.76`
- `org.apache.pdfbox:pdfbox:3.0.8`
- 고정 글꼴: `NotoSansKR-Regular.ttf`, SIL Open Font License 1.1
- PDF 생성 HTML은 외부 URL, 스크립트, 폼, 이미지와 CSS import를 포함하지 않는다.
- PDFBox로 PDF 헤더, 페이지 수, 텍스트 추출 가능 여부를 생성 테스트에서 확인한다.

### 5.5 검증과 다운로드

- 검증은 매니페스트 HMAC을 먼저 확인한 뒤 각 파일 SHA-256을 확인한다.
- 검증 결과는 `VALID`, `INVALID_HMAC`, `INVALID_HASH`, `MISSING_FILE` 중 하나로 추가 기록한다.
- 검증 실패가 원본 패키지 상태를 덮어쓰지 않으며 별도 이력과 감사 이벤트를 남긴다.
- ZIP 다운로드 직전에 무결성 검증을 다시 수행한다. 실패하면 다운로드하지 않는다.
- ZIP은 JDK `ZipOutputStream`으로 생성하고 응답에 `Cache-Control: no-store`와 고정 파일명 `<package-id>.zip`을 사용한다.

## 6. 철회·권리요청

### 6.1 접수 경계

본인확인은 시스템 밖의 기관 승인 절차에서 수행한다. 관리자는 확인 채널 코드와 확인시각만 기록하며 신분증 사본이나 인증번호를 저장하지 않는다.

권리요청 유형은 다음으로 고정한다.

- `ACCESS`
- `CORRECTION`
- `ERASURE`
- `RESTRICTION`
- `WITHDRAWAL`

처리기한은 법정 기간을 코드에 고정하지 않는다. 관리자가 기관 절차에 따라 `due_date`를 입력하며 접수일보다 이른 날짜는 거부한다.

### 6.2 상태

```text
RECEIVED → IDENTITY_VERIFIED → IN_REVIEW → APPROVED → COMPLETED
                                  └──────→ REJECTED → COMPLETED
RECEIVED ─────────────────────────────────────────→ CANCELLED
IDENTITY_VERIFIED ────────────────────────────────→ CANCELLED
```

- `IN_REVIEW` 전환 전에 외부 본인확인 채널·확인자·확인시각이 필요하다.
- 승인·반려 결정자는 요청 등록자와 달라야 한다.
- `APPROVED` 권리요청만 실행 완료할 수 있다.
- `REJECTED`는 사유 코드를 필수로 하고 통지 완료시 `COMPLETED`로 전환한다.
- 완료·취소된 요청은 수정하지 않는다.

### 6.3 개인정보 저장

- 목록에는 요청번호, 유형, 상태, 접수일, 기한, 조직과 처리업무만 표시한다.
- 요청 상세, 외부 확인 메모와 처리 결과 메모는 `PersonalDataCipher`로 AES-256-GCM 암호화한다.
- 암호화 컨텍스트는 `RIGHTS_REQUEST/<request-id>/<organization-id>/<field>`를 사용한다.
- 감사 이벤트에는 메모 원문을 넣지 않고 유형·상태·사유 코드만 기록한다.

### 6.4 유형별 완료 동작

- `ACCESS`: 권한 있는 관리자가 검증된 증적 패키지를 내려받았다는 실행 이력을 기록한다.
- `CORRECTION`: 기존 대상자 정정 화면에서 처리하고 관련 대상자 ID와 완료시각만 연결한다.
- `ERASURE`: 긴급 파기 검토 작업을 만들고 해당 작업이 `CONFIRMED`된 뒤 권리요청을 완료한다.
- `RESTRICTION`: 활성 요청의 추가 메일 발송과 신규 요청 생성을 제한하는 보존 보류를 설정한다.
- `WITHDRAWAL`: `consent_withdrawal`을 추가하고 이후 신규 활용·재발송을 차단한다. 기존 응답은 변경하지 않는다.

## 7. 보존·파기

### 7.1 기산일

보유정책의 `start_event`는 다음과 같이 해석한다.

| 기산 이벤트 | 출처 |
|---|---|
| `CONSENT_SUBMITTED_AT` | `consent_response.submitted_at` |
| `REQUEST_COMPLETED_AT` | `consent_request.completed_at` |
| `PROGRAM_END_AT` | 관리자가 등록한 `retention_anchor_event` |
| `PURPOSE_ACHIEVED_AT` | 관리자가 등록한 `retention_anchor_event` |

후자의 두 이벤트가 없으면 파기 예정일을 추정하지 않고 `ANCHOR_REQUIRED`로 표시한다. 등록·정정은 권한과 감사 이벤트가 필요하며 기존 이벤트는 삭제하지 않고 새 버전으로 대체한다.

기준 시간대는 `Asia/Seoul`이다. `due_date`는 기산일의 현지 날짜에 정책의 일·월·연 기간을 더한 뒤 `grace_period_days`를 더해 계산한다. 월말 계산은 Java `LocalDate.plusMonths`, 윤년 계산은 `plusYears` 규칙을 그대로 사용한다.

### 7.2 작업 생성과 상태

일일 스케줄러는 파기 예정일이 도래했고 같은 요청의 활성 작업이 없는 항목을 멱등하게 생성한다.

```text
DUE → PENDING_APPROVAL → APPROVED → EXECUTING → EXECUTED → CONFIRMED
 │          │                              └──→ FAILED → APPROVED
 └──────────┴────────→ HELD → DUE
DUE 또는 PENDING_APPROVAL → CANCELLED
```

- 시스템 자동 생성은 `DUE`, 권리요청 긴급 생성은 `PENDING_APPROVAL`로 시작한다.
- 보존 보류에는 사유 코드, 근거 설명, 설정자와 만료일이 필요하다.
- 활성 보존 보류가 있으면 승인·실행할 수 없다.
- 승인자는 작업 요청자와 달라야 한다.
- 실행자는 승인자와 같을 수 있으나 확인자는 실행자와 달라야 한다.
- `EXECUTING` 전환과 실제 파기 결과 저장은 같은 서비스 경계에서 처리한다.
- 같은 멱등키의 재실행은 기존 결과를 반환하고 다른 키의 중복 실행은 거부한다.

### 7.3 파기 대상

작업은 요청별 데이터를 우선 파기한다.

- 암호화 메일 수신주소·HTML 본문
- 공개 동의 세션의 잔여 흐름 정보
- 만료된 Excel staging 암호문
- 권리요청에서 승인된 요청별 암호화 메모
- 정책이 `FILE_SECURE_DELETE`인 경우 증적 파일 본문

대상자의 이름·이메일은 같은 `recipient_id`에 아직 보유기간이 남았거나 보존 보류된 다른 요청이 없을 때만 제거한다. 제거할 때 이름·이메일 암호문, 키 ID와 이메일 검색 HMAC을 `NULL`로 만들고 대상자 상태를 `DESTROYED`로 변경한다. 요청·응답·항목 결정·감사 이벤트와 UUID 관계는 최소 증적으로 보존한다.

`DELETE`와 `ANONYMIZE`는 현재 데이터 모델에서 직접 식별자 제거라는 동일한 안전 동작을 사용하되 실행 결과에 정책 원래 방식을 기록한다. `FILE_SECURE_DELETE`는 직접 식별자 제거와 주 저장소 증적 파일 삭제를 함께 수행한다.

### 7.4 파기 확인

실행자는 대상 건수, 제거 필드, 삭제 파일 수, 실패 건수와 내부 사유 코드를 기록한다. 확인자는 원문을 조회하지 않고 DB 필드 NULL 여부, 상태, 파일 부재와 증적 메타데이터의 `DESTROYED` 상태를 검증한 후 `CONFIRMED`로 전환한다.

## 8. 통계

`/admin/statistics`는 선택 기간과 허용 조직·처리업무 범위 안에서 다음 집계를 제공한다.

- 요청 상태별 수
- 제출 완료, 전체 미동의와 철회 수
- 메일 상태별 수
- 증적 생성·실패·검증 실패 수
- 권리요청 유형·상태·기한 초과 수
- 파기 예정·보존 보류·실행·확인·실패 수

기본 기간은 최근 30일, 최대 조회 기간은 1년이다. 통계 응답에는 이름, 이메일, 대상자 UUID와 자유 입력 메모를 포함하지 않는다. CSV 내보내기와 외부 BI 연동은 포함하지 않는다.

## 9. 권한

| 권한 | 기능 |
|---|---|
| `EVIDENCE_VIEW` | 증적 목록·상세·상태 조회 |
| `EVIDENCE_GENERATE` | 실패 증적 재시도 |
| `EVIDENCE_DOWNLOAD` | 검증된 증적 ZIP 다운로드 |
| `EVIDENCE_VERIFY` | 무결성 재검증 |
| `RIGHTS_VIEW` | 권리요청 목록·상세 조회 |
| `RIGHTS_EDIT` | 접수·본인확인·검토·실행 기록 |
| `RIGHTS_APPROVE` | 승인·반려 결정 |
| `RETENTION_VIEW` | 파기 작업 조회 |
| `RETENTION_MANAGE` | 기산일·보존 보류·작업 생성 |
| `RETENTION_APPROVE` | 파기 승인 |
| `RETENTION_EXECUTE` | 파기 실행 |
| `RETENTION_CONFIRM` | 파기 결과 확인 |
| `STATISTICS_VIEW` | 범위 제한 통계 조회 |

권한은 SQL에서 자동으로 `SYSTEM_ADMIN`에 모두 부여하지 않는다. 초기 패치는 권한 코드만 생성한다. 개발 DB에서는 관리자·역할 화면을 통해 `PRIVACY_OFFICER` 역할과 테스트 사용자에게 필요한 권한을 명시적으로 부여한다. `AUDITOR`에는 조회·검증·통계만 허용하고 다운로드·결정·파기는 허용하지 않는다.

## 10. 관리자 URL

| 화면 | URL |
|---|---|
| 증적 목록 | `/admin/evidence-packages` |
| 증적 상세·검증 | `/admin/evidence-packages/{packageId}` |
| 증적 ZIP 다운로드 | `/admin/evidence-packages/{packageId}/download` |
| 권리요청 목록 | `/admin/rights-requests` |
| 권리요청 등록 | `/admin/rights-requests/new` |
| 권리요청 상세·처리 | `/admin/rights-requests/{rightsRequestId}` |
| 파기 작업 목록 | `/admin/retention-jobs` |
| 파기 작업 상세·승인·실행·확인 | `/admin/retention-jobs/{jobId}` |
| 운영 통계 | `/admin/statistics` |

민감한 상세·다운로드 응답은 `Cache-Control: no-store`, CSP, `nosniff`, `frame-ancestors 'none'`를 적용한다.

## 11. DB 변경

`20260824_005_create_evidence_rights_retention_tables.sql`은 다음을 추가한다.

- `evidence_package`
- `evidence_file`
- `evidence_verification`
- `rights_request`
- `rights_request_event`
- `consent_withdrawal`
- `retention_anchor_event`
- `legal_hold`
- `retention_job`
- `retention_job_event`
- Phase 6 권한 코드
- 파기된 대상자를 허용하는 `recipient` nullable·상태 제약 변경

불변 테이블인 `evidence_file`, `evidence_verification`, `rights_request_event`, `consent_withdrawal`, `retention_job_event`에는 `seal_app` UPDATE·DELETE 권한을 주지 않는다. 작업 현재 상태 테이블에는 필요한 SELECT·INSERT·제한 UPDATE만 부여하고 DELETE·TRUNCATE는 금지한다.

검증 SQL `009_evidence_rights_retention_verify.sql`은 테이블·소유자·FK·CHECK·부분 유일 인덱스·권한·불변 테이블 UPDATE 거부·스크립트 체크섬을 검증한다.

## 12. 오류와 감사

- 파일 생성 실패: `EVIDENCE_GENERATION_FAILED`
- HMAC 불일치: `EVIDENCE_HMAC_INVALID`
- 파일 해시 불일치: `EVIDENCE_FILE_HASH_INVALID`
- 권리요청 상태 충돌: `RIGHTS_STATE_CONFLICT`
- 본인확인 미완료: `RIGHTS_IDENTITY_REQUIRED`
- 보존 보류 중 파기 시도: `RETENTION_LEGAL_HOLD_ACTIVE`
- 이중 통제 위반: `RETENTION_SEPARATION_REQUIRED`
- 다른 활성 요청 존재: 파기 실패가 아니라 `RECIPIENT_PII_RETAINED` 결과 코드

화면에는 한국어 설명만 표시하고 내부 오류 코드와 추적 ID는 운영자 로그·감사 이벤트에 사용한다. 감사 메타데이터에는 객체 UUID, 상태, 건수와 코드만 저장한다.

## 13. 테스트 전략

### 13.1 단위 테스트

- canonical 매니페스트와 HMAC 결정성
- 파일 경로 이탈 차단과 원자 이동
- PDF 생성·한글 텍스트 추출
- 패키지 변조별 검증 결과
- 권리요청 상태 전이와 작성자·결정자 분리
- 철회 시 원응답 불변성
- 일·월·연·월말·윤년·유예기간 계산
- 보존 보류와 파기 이중 통제
- 다른 활성 요청이 있는 대상자의 PII 유지

### 13.2 PostgreSQL 통합 테스트

- 동의 완료와 증적 작업 원자 등록
- `FOR UPDATE SKIP LOCKED` 작업 단일 선점과 10분 임대 복구
- 같은 응답 증적·같은 요청 파기 작업 중복 생성 차단
- 권한·조직·처리업무 범위가 다른 관리자 접근 거부
- 불변 이벤트 UPDATE·DELETE 거부
- 실제 파기 후 암호문·HMAC NULL, 상태와 최소 관계 보존
- 통계 집계의 범위 제한과 개인정보 컬럼 부재

### 13.3 MVC·브라우저 테스트

- 모든 변경 POST의 CSRF 거부
- 권한 없는 직접 객체 접근 404 또는 403
- 다운로드 `no-store`, 안전 파일명과 ZIP 내용
- 데스크톱과 360px에서 목록·상세·오류 화면 수평 넘침 없음
- 키보드 포커스, 레이블과 상태 안내

## 14. 완료 기준

1. Phase 6 DB 패치가 `seal_test`, `seal_dev`에 적용되고 검증 SQL이 통과한다.
2. 동의 완료 후 증적이 한 번만 생성되고 변조 검증과 제한 다운로드가 동작한다.
3. 권리요청의 접수·본인확인·검토·결정·완료와 철회·삭제 연결이 동작한다.
4. 보유기간 계산, 기산일 보완, 보존 보류, 승인·실행·확인의 상태와 이중 통제가 동작한다.
5. 다른 유효 요청이 있는 대상자 개인정보를 과잉 파기하지 않는다.
6. 통계가 권한·범위 안의 집계만 반환하고 개인정보를 포함하지 않는다.
7. 자동화 전체 테스트, WAR 패키징과 9090 대표 화면 검증이 통과한다.
8. 실제 비밀키, 증적 파일, 개인정보 샘플과 `.env`는 Git에 포함되지 않는다.
9. Rocky Linux, 백업·복구, 운영 HTTPS·SMTP와 물리 매체 파기는 미검증 상태로 명확히 남긴다.
