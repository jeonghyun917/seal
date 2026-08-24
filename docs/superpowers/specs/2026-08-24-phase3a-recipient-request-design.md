# 3A단계 대상자·동의 요청 설계 기준서

**문서 버전:** v1.0

**상태:** 사용자 검토 요청

**상위 기준:** `docs/superpowers/specs/2026-08-21-consent-system-full-build-design.md`

**선행 기능:** 관리자·정책 기반, 발행 동의서 템플릿

**범위:** 대상자 개별 등록, 마스킹 조회, 요청 초안, 불변 스냅샷, 만료·취소, 일회용 링크 발급·재발급 기반

## 1. 목표와 단계 경계

승인된 처리업무와 발행 템플릿을 대상자에게 연결해 동의 요청을 만들고, 발송·응답 이후에도 바뀌지 않는 요청 스냅샷을 생성한다. 이름과 이메일은 최소 수집·암호화하고 목록에서는 마스킹한다. 요청을 활성화할 때 256비트 일회용 링크 토큰을 발급하되 DB에는 HMAC만 저장한다.

3A는 관리자 측 요청 준비 기능까지 구현한다. 다음 기능은 포함하지 않는다.

- Excel 일괄 반입, 원본 파일 파기와 ClamAV 연계: 3B
- 공개 링크 GET·POST 진입, 모바일 동의, 최종 제출: 4단계
- HTML 이메일, Outbox, SMTP 발송·재시도: 5단계
- PDF·JSON 증적, 철회·권리요청·파기: 6단계

전자서명, SMS·알림톡, OTP, 휴대전화/PASS 본인확인, 추적 픽셀, 민감정보·고유식별정보, 만 14세 미만, SSO·LDAP, Redis·Kafka·유료 KMS는 전체 범위에서 계속 제외한다.

## 2. 검토한 접근과 선택

### 2.1 선택: 대상자·요청·스냅샷·링크 분리

대상자는 조직별 재사용 가능한 논리 객체로 관리하고, 요청은 특정 승인 처리업무·발행 템플릿·대상자를 참조한다. 표시 내용은 요청 확정 시 별도 불변 스냅샷과 항목 행으로 복제한다. 링크 상태는 요청 상태와 별도 테이블에서 관리한다.

이 구조는 동일 대상자에 대한 여러 요청에서 개인정보 중복을 줄이고, 정책이나 템플릿이 바뀌어도 과거 요청의 표시 내용을 유지하며, 요청·링크·향후 메일·응답 상태를 서로 혼동하지 않게 한다.

### 2.2 제외: 요청마다 이름·이메일 직접 저장

구현은 단순하지만 동일 개인정보가 요청 수만큼 중복되고 정정·파기 범위를 통제하기 어렵다. 대상자와 요청의 생명주기도 분리할 수 없어 채택하지 않는다.

### 2.3 제외: 이름·이메일 평문 저장

DB 권한과 화면 마스킹만으로도 기능은 만들 수 있지만 DB 백업·운영 조회에서 직접 식별자가 그대로 노출된다. 유료 KMS 없이도 Java JCE와 서버 외부 키 파일로 암호화할 수 있으므로 평문 저장을 채택하지 않는다.

## 3. 모듈 경계

기존 모듈형 모놀리스에 다음 패키지를 추가한다.

- `recipient`: 대상자 등록·정정·비활성화, 암호화, 정규화, 중복 검사와 마스킹
- `request`: 요청 상태, 스냅샷 생성, 요청 항목, 만료·취소와 관리자 조회
- `link`: 토큰 생성·HMAC, 발급·재발급·폐기 상태. 4단계 공개 진입이 이 모듈을 사용한다.

`request`는 `policy`의 승인 처리업무 조회 인터페이스와 `template`의 발행 버전 조회·렌더링 인터페이스만 사용한다. 다른 모듈의 MyBatis Mapper나 내부 테이블 행 객체를 직접 참조하지 않는다.

## 4. 대상자 데이터 모델과 보호

### 4.1 `recipient`

- `recipient_id`: UUID 기본키
- `organization_unit_id`: 개인정보 소유 조직
- `recipient_name_ciphertext`: AES-256-GCM 암호문 `BYTEA`
- `recipient_email_ciphertext`: AES-256-GCM 암호문 `BYTEA`
- `encryption_key_id`: 암호화에 사용한 키 버전 1~30자
- `email_lookup_hmac`: 정규화 이메일의 HMAC-SHA-256 64자리
- `collection_source`: `MANUAL`; 3B에서 `EXCEL`을 추가한다.
- `collection_basis`: 반입·등록 근거 5~500자
- `recipient_status`: `ACTIVE`, `INACTIVE`
- 생성·수정 관리자와 시각
- `version_no`: 낙관적 잠금 번호

한 조직 안에서 활성 대상자의 `email_lookup_hmac`는 유일하다. 동일 이메일이 이미 있으면 기존 대상자를 반환하지 않고 중복 안내 후 기존 대상자 선택 화면으로 이동한다. 조직이 다르면 별도 대상자로 관리한다.

### 4.2 암호화와 검색

- JDK `AES/GCM/NoPadding`, 256비트 키와 매 암호화마다 새로운 96비트 nonce를 사용한다.
- 저장 바이트 형식은 `버전 1바이트 + nonce 12바이트 + GCM 암호문·태그`다.
- AAD에는 `recipient`, 필드명, `recipient_id`, `organization_unit_id`를 고정 순서로 넣어 다른 행·필드로 암호문을 옮겨도 복호화되지 않게 한다.
- 이메일은 앞뒤 공백 제거, Unicode NFC, 소문자 변환 후 HMAC 검색키로 색인한다. 원래 표기 이메일은 암호화해 보존한다.
- 이름은 앞뒤 공백 제거와 Unicode NFC만 적용하며 부분검색용 평문·별도 색인을 만들지 않는다.
- 목록 검색은 요청 코드·상태·기간과 정확한 이메일 검색만 제공한다. 이름 부분검색은 개인정보 색인 확대를 피하기 위해 제공하지 않는다.
- 복호화 오류는 일반 사용자에게 내부 원인을 표시하지 않고 감사 이벤트 `RECIPIENT_DECRYPT_FAILURE`를 남긴다.

### 4.3 키 제공

- `SEAL_DATA_ACTIVE_KEY_ID`: 현재 암호화 키 식별자
- `SEAL_DATA_KEYS_FILE`: `key-id=Base64(32바이트)` 형식의 키 파일 경로
- `SEAL_EMAIL_INDEX_HMAC_KEY_FILE`: 이메일 색인용 32바이트 이상 키 파일 경로
- `SEAL_LINK_HMAC_KEY_FILE`: 일회용 링크 HMAC용 32바이트 이상 키 파일 경로

키 파일은 저장소·DB·WAR 밖에 두고 로컬에서는 사용자만, Rocky Linux에서는 Tomcat 실행 계정만 읽도록 한다. 같은 목적의 키를 서로 재사용하지 않는다. 활성 암호화 키를 바꾼 뒤에도 기존 `encryption_key_id`의 키는 재암호화 완료 전까지 파일에 유지한다. 키 원문·경로·복호화 결과를 로그와 감사 메타데이터에 남기지 않는다.

## 5. 요청과 스냅샷 데이터 모델

### 5.1 `consent_request`

- `consent_request_id`: UUID 기본키
- `request_code`: 사용자에게 노출 가능한 기관 내 유일 코드 `CR-YYYYMMDD-영문숫자8자`
- `organization_unit_id`: 요청 소유 조직
- `recipient_id`: 활성 대상자
- `processing_activity_id`, `processing_activity_version_id`: 활성·승인 처리업무 버전
- `consent_template_id`, `consent_template_version_id`: 해당 처리업무의 현재 또는 과거 `PUBLISHED` 버전
- `retention_policy_version_id`: 처리업무 승인 버전에 연결된 승인 보유정책 버전
- `request_status`: `DRAFT`, `READY`, `ACTIVE`, `COMPLETED`, `EXPIRED`, `CANCELLED`
- `expires_at`: 생성 시점 기준 1시간 이상 30일 이하
- `idempotency_key`: UUID, 조직 안에서 유일
- `current_secure_link_id`: 현재 유효 링크, 발급 전 NULL
- `snapshot_id`: 요청 확정 전 NULL
- 취소 관리자·시각·사유
- 생성·수정 관리자와 시각
- `version_no`: 낙관적 잠금 번호

처리업무·템플릿·보유정책의 논리 ID와 버전 ID 일치는 복합 외래키로 강제한다. 대상자와 요청의 `organization_unit_id`도 복합 외래키로 일치시킨다.

`current_secure_link_id`는 순환 생성 문제를 피하기 위해 요청 생성 때 NULL로 두고 링크 발급 트랜잭션에서 설정한다. `(current_secure_link_id, consent_request_id)`가 `secure_link (secure_link_id, consent_request_id)`를 참조하는 복합 외래키로 현재 링크가 다른 요청에 연결되는 것을 차단한다.

### 5.2 `consent_request_snapshot`

요청당 하나이며 생성 후 수정·삭제하지 않는다.

- `consent_request_snapshot_id`: UUID 기본키
- `consent_request_id`: UNIQUE 외래키
- `snapshot_schema_version`: `1`
- `snapshot_json`: 직접 식별자를 제외한 canonical JSONB
- `snapshot_hash`: canonical JSON UTF-8 SHA-256 64자리
- `template_content_hash`: 발행 템플릿 해시
- `created_at`, `created_by`

스냅샷에는 기관명, 요청 코드, 처리 목적, 법적 근거 유형·상세, 보유기간, 제목, 정제 안내문, 거부권, 철회 안내, 만료시각과 구조화 동의 항목을 저장한다. 대상자 이름·이메일, 관리자 식별자, 토큰, 접속 정보는 넣지 않는다. 이메일과 공개 웹은 이 스냅샷을 같은 입력으로 사용하고 대상자 이름은 렌더링 시 권한 있는 `recipient` 조회 결과로만 결합한다.

### 5.3 `consent_request_item`

- `consent_request_item_id`: UUID 기본키
- `consent_request_snapshot_id`: 소속 스냅샷
- `item_order`, `item_code`, `item_type`, `item_title`, `required`
- 목적, 보유기간, 거부권, 제3자 제공받는 자
- 선택된 일반 개인정보 항목의 ID·순서·명칭을 별도 자식 테이블 `consent_request_item_data`에 복제

스냅샷 JSON과 항목 행은 같은 트랜잭션에서 생성한다. 항목 행은 4단계 제출 검증과 6단계 증적 생성에서 사용한다. 두 표현의 해시 입력은 동일한 canonical snapshot 객체 하나에서 파생해 불일치를 막는다.

### 5.4 `consent_request_event`

- `consent_request_event_id`: UUID 기본키
- `consent_request_id`
- `event_type`: `CREATED`, `FINALIZED`, `ACTIVATED`, `LINK_REISSUED`, `CANCELLED`, `EXPIRED`, 이후 `COMPLETED`
- `actor_id`, `reason_code`, `created_at`

현재 상태와 별도로 상태 이력을 보존한다. `seal_app`은 SELECT·INSERT만 가능하며 UPDATE·DELETE할 수 없다.

## 6. 상태와 불변 규칙

```text
DRAFT → READY → ACTIVE → COMPLETED
  │       │        ├──→ EXPIRED
  └───────┴────────└──→ CANCELLED
```

- `DRAFT`에서만 대상자, 처리업무, 템플릿과 만료시각을 변경한다.
- 생성·수정·확정 시 `expires_at`은 해당 처리시각보다 최소 1시간 이후, 최대 30일 이후여야 한다. 오래 방치해 남은 시간이 1시간 미만인 초안은 만료시각을 다시 정해야 확정할 수 있다.
- `READY` 전환 시 승인 처리업무·보유정책과 발행 템플릿을 다시 조회하고 스냅샷·항목을 생성한다.
- `READY` 이후 요청 기준과 스냅샷은 수정하지 않는다.
- `READY`에서 링크를 최초 발급하면 `ACTIVE`가 된다.
- `ACTIVE` 링크 재발급은 요청 상태를 유지하고 이전 `ISSUED` 링크를 `REVOKED`로 바꾼다.
- 링크의 `expires_at`은 요청 만료시각과 같으며 재발급으로 요청 만료시각을 연장하지 않는다.
- `DRAFT`, `READY`, `ACTIVE`는 사유 5~500자로 취소할 수 있다. 취소 시 현재 링크도 함께 폐기한다.
- 만료시각이 지나면 접근 시 즉시, 그리고 1분 주기 작업에서 `EXPIRED`로 바꾸고 현재 링크도 만료한다.
- `COMPLETED`, `EXPIRED`, `CANCELLED`는 최종 상태이며 수정·재활성화·재발급할 수 없다.
- `COMPLETED` 전환과 최종 응답 저장은 4단계의 한 트랜잭션에서 수행한다.
- 동일 `idempotency_key` 재요청은 새 요청을 만들지 않고 기존 요청 ID를 반환한다.

## 7. 일회용 링크 기반

### 7.1 `secure_link`

- `secure_link_id`: UUID 기본키
- `consent_request_id`
- `token_hmac`: HMAC-SHA-256 64자리, UNIQUE
- `link_status`: `ISSUED`, `ACCESSED`, `COMPLETED`, `EXPIRED`, `REVOKED`
- `expires_at`, 발급·접속·완료·폐기 시각
- `issued_by`, `revoked_by`, `revoke_reason`
- `version_no`

### 7.2 토큰 처리

- Java `SecureRandom`으로 256비트를 생성하고 패딩 없는 Base64URL로 URL 토큰을 만든다.
- DB에는 `HmacSHA256(SEAL_LINK_HMAC_KEY, UTF8(token))`만 저장한다.
- 원문 토큰은 발급 서비스 반환값에서 한 번만 사용하고 DB·로그·감사 이벤트·URL 쿼리 외 위치에 저장하지 않는다.
- 관리자 수동 발급 화면은 권한 있는 사용자에게 링크를 한 번만 표시하고 새로고침하면 다시 표시하지 않는다. 운영에서는 이메일 발송 작업이 새 링크를 발급해 Outbox HTML에 직접 넣는다.
- URL 형식은 `/c/{token}`이며 성명, 이메일, 요청 코드와 내부 UUID를 포함하지 않는다.
- GET 동작과 링크 스캐너 안전 진입은 4단계에서 구현한다. 3A에서는 생성·HMAC 조회·재발급·폐기 서비스와 관리자 상태만 구현한다.

## 8. 권한과 데이터 범위

다음 권한을 추가한다.

- `RECIPIENT_VIEW`: 마스킹 목록·상세 조회
- `RECIPIENT_EDIT`: 개별 등록·정정·비활성화
- `CONSENT_REQUEST_VIEW`: 요청 목록·상세·스냅샷 미리보기
- `CONSENT_REQUEST_EDIT`: 요청 초안 생성·수정·확정
- `CONSENT_REQUEST_ACTIVATE`: 최초 링크 발급과 요청 활성화
- `CONSENT_REQUEST_CANCEL`: 요청 취소
- `CONSENT_REQUEST_LINK_REISSUE`: 활성 요청 링크 재발급

권한 외에 다음 범위를 모두 검사한다.

1. 대상자의 조직이 사용자의 활성 조직 범위에 포함된다.
2. 요청의 처리업무가 사용자의 조직 및 처리업무 범위에 포함된다.
3. 요청 대상자 조직과 요청 소유 조직이 일치한다.

권한 없음은 403, 권한은 있으나 객체가 범위 밖이면 404다. 이름·이메일 복호화는 범위 검사 이후에만 수행한다. 시스템 관리자 역할에는 새 권한을 활성 배정한다.

처리업무 범위는 기존 정책·템플릿 모듈과 같은 의미를 사용한다. 활성 `user_processing_activity_scope`가 하나 이상인 사용자는 그 목록으로 추가 제한하고, 별도 처리업무 범위가 없는 사용자는 활성 조직 범위 안의 처리업무를 사용할 수 있다.

## 9. 관리자 화면과 URL

### 9.1 대상자

- `GET /admin/recipients`: 마스킹 이름·이메일, 조직, 등록 근거, 상태와 정확한 이메일 검색
- `GET /admin/recipients/new`: 개별 등록 폼
- `POST /admin/recipients`: 대상자 등록
- `GET /admin/recipients/{recipientId}`: 마스킹 상세와 요청 이력
- `GET /admin/recipients/{recipientId}/edit`: 정정 폼
- `POST /admin/recipients/{recipientId}`: 정정
- `POST /admin/recipients/{recipientId}/status`: 활성·비활성 전환

등록·정정 완료 후 목록과 상세에는 이름과 이메일을 마스킹한다. 정정 폼에서도 기본값은 마스킹하며 `개인정보 확인` 권한을 별도로 만들지 않고 이번 단계에서는 현재 값 전체를 재표시하지 않는다. 수정할 필드만 새 값으로 입력하고 빈 필드는 기존 암호문을 유지한다.

### 9.2 요청

- `GET /admin/requests`: 요청 코드, 마스킹 대상자, 처리업무, 상태, 만료시각 검색
- `GET /admin/requests/new`: 대상자·승인 처리업무·발행 템플릿·만료 선택
- `POST /admin/requests`: 멱등 요청 생성
- `GET /admin/requests/{requestId}`: 상태·이력·링크 상태 상세
- `GET /admin/requests/{requestId}/edit`: 초안 편집
- `POST /admin/requests/{requestId}`: 초안 수정
- `POST /admin/requests/{requestId}/finalize`: 스냅샷 생성과 `READY` 전환
- `GET /admin/requests/{requestId}/preview`: 스냅샷 기반 읽기 전용 미리보기
- `POST /admin/requests/{requestId}/activate`: 최초 링크 발급과 `ACTIVE` 전환
- `POST /admin/requests/{requestId}/links`: 링크 재발급
- `POST /admin/requests/{requestId}/cancel`: 요청 취소

모든 변경 요청은 CSRF 보호, POST-Redirect-GET과 낙관적 잠금을 사용한다. 원문 링크는 서버 세션의 한 번성 flash 속성으로만 전달하며 링크 원문을 query string이나 애플리케이션 로그에 넣지 않는다.

## 10. 감사와 개인정보 최소화

다음 작업을 성공·실패·거부 결과로 기록한다.

- `RECIPIENT_CREATE`, `RECIPIENT_UPDATE`, `RECIPIENT_STATUS_CHANGE`
- `RECIPIENT_DECRYPT_FAILURE`
- `CONSENT_REQUEST_CREATE`, `CONSENT_REQUEST_UPDATE`, `CONSENT_REQUEST_FINALIZE`
- `CONSENT_REQUEST_ACTIVATE`, `CONSENT_REQUEST_CANCEL`
- `SECURE_LINK_ISSUE`, `SECURE_LINK_REISSUE`, `SECURE_LINK_REVOKE`, `CONSENT_REQUEST_EXPIRE`

감사 메타데이터에는 객체 UUID, 상태, 처리업무 ID, 템플릿 버전 ID, 스냅샷 해시와 이유 코드만 넣는다. 이름, 이메일, 등록 근거 원문, 스냅샷 JSON, 토큰·HMAC과 암호문은 넣지 않는다. 대상자 상세 조회는 직접 식별자를 전체 표시하지 않으므로 건별 감사 이벤트를 만들지 않지만 내보내기 기능은 3B에서 별도 감사한다.

## 11. DB 권한과 변경

- 새 테이블과 권한은 다음 순번의 `database/03_patch` SQL로 추가하고 SHA-256을 `db_change_history`에 기록한다.
- `seal_app`은 대상자·요청·링크 현재 상태 테이블에 SELECT·INSERT·UPDATE만 가진다.
- 스냅샷, 요청 항목, 요청 이벤트에는 SELECT·INSERT만 허용한다.
- 모든 새 테이블에 대한 DELETE·TRUNCATE·DDL은 거부한다.
- 암호화 키와 HMAC 키는 DB 테이블·변경 이력에 저장하지 않는다.
- 복합 외래키와 CHECK·UNIQUE 제약으로 조직·처리업무·템플릿 버전 교차 연결을 DB에서도 차단한다.

## 12. 오류·동시성·복구

- 검증 오류는 개인정보 원문을 포함하지 않는 필드별 한국어 메시지로 반환한다.
- 중복 이메일은 409가 아니라 폼 검증 결과로 처리하고 마스킹된 기존 대상자 링크를 제시한다.
- 허용되지 않은 상태 전이, 버전 충돌과 이미 재발급된 링크 작업은 409다.
- 범위 밖 객체와 HMAC이 일치하지 않는 링크는 404다.
- 요청 확정·링크 발급·재발급·취소는 요청 행을 잠그고 상태, 링크, 이벤트와 감사를 한 트랜잭션에서 저장한다.
- 링크 HMAC 충돌은 새 토큰을 최대 3회 생성하고 계속 충돌하면 내부 오류코드 `LINK_TOKEN_COLLISION`으로 중단한다.
- 암호화 키 파일이 없거나 길이가 잘못되면 애플리케이션 시작을 실패시킨다. 키 ID가 없는 기존 암호문은 복호화를 시도하지 않는다.
- 스케줄러 만료 작업은 `FOR UPDATE SKIP LOCKED`와 요청 ID 멱등 처리를 사용한다.

## 13. 테스트와 인수 기준

### 13.1 자동 테스트

- 암호화: 매 암호화 nonce 차이, 정상 복호화, AAD 교차 사용 실패, 키 ID 선택, 로그 비노출
- 정규화·HMAC: 이메일 대소문자·공백 중복, 조직별 유일성, 마스킹
- 대상자: 등록·정정·비활성, 조직 범위 404, 권한 403, 낙관적 충돌
- 요청 도메인: 상태 전이, 1시간·30일 경계, 최종 상태 불변, 멱등성
- 스냅샷: 발행 템플릿과 승인 정책 고정, 직접 식별자 제외, canonical hash 결정성
- 링크: 256비트 Base64URL, HMAC만 저장, 재발급 시 이전 링크 폐기, 원문 로그 비노출
- MVC: CSRF, POST-Redirect-GET, 마스킹 출력, 링크 한 번 표시, 오류 입력 보존
- PostgreSQL: 복합 FK·CHECK·UNIQUE, 최소권한, 불변 테이블 UPDATE·DELETE 거부, 동시 확정·재발급
- 만료 작업: 경계시각, 중복 실행, 취소·완료 요청 미변경

### 13.2 완료 조건

1. 조직 범위가 있는 관리자가 이름·이메일·등록 근거로 대상자를 개별 등록한다.
2. DB에서 이름·이메일 평문을 조회할 수 없고 정확한 이메일 HMAC 검색만 가능하다.
3. 목록·상세·요청 화면에 이름과 이메일이 일관되게 마스킹된다.
4. 승인 처리업무와 발행 템플릿으로 만료기간 1시간~30일 요청 초안을 만든다.
5. 확정 후 정책·템플릿 변경이 요청 스냅샷과 항목에 영향을 주지 않는다.
6. 동일 멱등성 키 재전송은 요청을 중복 생성하지 않는다.
7. 링크 재발급 시 이전 링크가 즉시 폐기되고 DB에는 토큰 HMAC만 남는다.
8. 취소·만료된 요청은 활성화·재발급할 수 없다.
9. 권한·조직·처리업무 범위 우회가 403 또는 404로 차단된다.
10. 모든 중요 변경은 개인정보 원문 없이 감사 이벤트로 남는다.
11. PostgreSQL 검증 SQL, 전체 테스트, 실행형 WAR와 9090 대표 화면 검수가 통과한다.

## 14. 운영·개발 준비값

3A 실행 전에 로컬과 Rocky Linux에 서로 다른 키 파일을 준비한다. 키 파일은 Git에 커밋하지 않으며 `.env.example`에는 실제 값이 아닌 경로 변수명만 기록한다.

로컬 `seal_dev`에는 운영 대상자 데이터를 넣지 않는다. 화면 검수는 가상 이름과 테스트 도메인 이메일만 사용하며 완료 후 파기 단계가 구현되기 전까지 테스트 데이터임을 명확히 표시한다.
