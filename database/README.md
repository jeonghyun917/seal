# PostgreSQL 16.14 개발 DB 구성

모든 SQL 파일은 UTF-8이며 `psql 16.14`에서 실행한다. Flyway와 Liquibase는 사용하지 않는다.

## 계정 책임

- `seal_owner`: `NOLOGIN` 객체 소유자다. 비밀번호가 없으며 PostgreSQL 관리자가 `SET ROLE seal_owner`로 스키마 변경을 수행한다.
- `seal_app`: 애플리케이션 런타임 로그인 계정이다. 필요한 SELECT·INSERT·UPDATE와 시퀀스 사용 권한만 가진다.
- 애플리케이션 계정에는 스키마 CREATE, 테이블 DELETE·TRUNCATE, SUPERUSER, CREATEDB, CREATEROLE 권한을 주지 않는다.
- `audit_event`는 `seal_app`에 SELECT·INSERT만 허용한다.

## 1. DB와 역할 생성

Rocky Linux에서 PostgreSQL 관리 계정으로 실행한다.

```bash
sudo -u postgres psql -X -f database/00_admin/001_create_dev_database.sql
```

스크립트가 `seal_app` 비밀번호를 대화형으로 묻는다. 비밀번호를 명령행 인자나 셸 이력에 직접 적지 않는다.

## 2. 스키마와 초기 데이터 적용

각 변경 파일의 SHA-256과 배포할 애플리케이션 버전을 반드시 전달한다. 아래 예시는 파일 하나를 적용하는 형식이며, 세 파일을 경로 순서대로 각각 실행한다.

```bash
sha256sum database/01_schema/001_create_schema.sql
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -v script_checksum='<sha256sum 결과>' \
  -v app_version='0.1.0-SNAPSHOT' \
  -f database/01_schema/001_create_schema.sql
```

최초 구성 적용 순서:

1. `database/01_schema/001_create_schema.sql`
2. `database/01_schema/002_create_core_tables.sql`
3. `database/02_data/001_common_codes.sql`

기존 Phase 1 DB의 Phase 2 추가 적용 순서:

1. `database/03_patch/20260824_001_create_consent_template_tables.sql`
2. `database/03_patch/20260824_002_add_template_draft_replace_function.sql`

두 패치도 각각 SHA-256을 계산해 `script_checksum`으로 전달한다. `20260824_002`는 앱 계정에 DELETE 권한을 주지 않고, 활성 사용자·조직 범위·초안 상태를 검사한 뒤 해당 초안의 항목만 교체하는 `SECURITY DEFINER` 함수를 추가한다.

Phase 3A 대상자·요청 기반은 다음 패치를 추가 적용한다.

1. `database/03_patch/20260824_003_create_recipient_request_tables.sql`

이 패치는 `target_database` psql 변수를 생략하면 `seal_dev`에 적용한다. 테스트 DB에는 `-v target_database=seal_test`를 명시한다. 적용 전 체크섬을 계산해 `script_checksum`으로 전달한다.

Phase 3B~5 Excel 반입·공개 동의·메일 Outbox는 다음 패치를 추가 적용한다.

1. `database/03_patch/20260824_004_create_import_consent_delivery_tables.sql`

이 패치는 임시 반입·공개 세션·불변 응답·암호화 메일 Outbox 테이블과 관련 권한을 만들고, 공개 제출 이벤트의 `PUBLIC` 행위자 유형을 추가한다. `seal_test` 적용과 검증을 먼저 통과시킨 뒤 동일 체크섬을 `seal_dev`와 운영 DB에 적용한다.

Phase 6 증적·권리요청·보유기간 수명주기는 다음 패치를 순서대로 적용한다.

1. `database/03_patch/20260824_005_create_evidence_rights_retention_tables.sql`
2. `database/03_patch/20260824_006_backfill_evidence_queue.sql`

`005`는 증적·검증·권리요청·철회·기산일·보존 보류·파기 작업 테이블과 13개 권한 코드를 추가한다. `006`은 패치 전에 이미 완료된 응답의 요청 완료시각을 보완하고 증적 큐를 멱등하게 생성한다. 두 파일 모두 별도 SHA-256을 계산해 순서대로 적용하며, 이미 적용된 `005`를 수정해 재사용하지 않는다.

각 파일은 PostgreSQL 관리자가 실행하지만 트랜잭션 안에서 `SET LOCAL ROLE seal_owner`를 사용하므로 생성 객체의 소유자는 `seal_owner`가 된다.

## 3. 검증

초기 SQL을 모두 적용한 뒤 관리 계정으로 실행한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -f database/04_verify/001_phase1a_verify.sql
```

성공 시 `Phase 1A database verification passed`를 출력한다. 역할, 객체 소유권, 필수 테이블, 앱 권한, 감사 테이블의 추가 전용 권한 또는 변경 이력이 기준과 다르면 오류로 종료한다.

Phase 2 패치 후에는 다음 검증을 순서대로 실행한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -f database/04_verify/005_consent_template_verify.sql
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -f database/04_verify/006_template_draft_replace_verify.sql
```

첫 검증은 템플릿 테이블·복합 외래키·권한·감사 이력·기본 역할 권한을 확인한다. 두 번째 검증은 초안 항목 교체 함수의 소유자·실행 권한·고정 검색 경로와 앱 계정의 직접 DELETE 거부를 확인한다.

Phase 3A 패치 후에는 패치 파일의 SHA-256을 `expected_checksum`으로 전달한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -v target_database=seal_dev \
  -v expected_checksum='<sha256sum 결과>' \
  -f database/04_verify/007_recipient_request_verify.sql
```

성공 시 `Phase 3A recipient request verification passed`를 출력한다. 대상자·요청·스냅샷·이력·링크 테이블의 소유권, 복합 외래키, 최소권한, 원문 토큰 컬럼 부재와 SYSTEM_ADMIN 권한을 확인한다.

Phase 3B~5 패치 후에는 같은 방식으로 체크섬을 전달해 검증한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -v target_database=seal_dev \
  -v expected_checksum='<sha256sum 결과>' \
  -f database/04_verify/008_import_consent_delivery_verify.sql
```

성공 시 `Phase 3B-5 schema verification passed`를 출력한다. 새 테이블 소유권, 앱 계정의 최소권한, 불변 응답·발송시도 수정 거부, Excel 출처와 공개 행위자 제약조건, SYSTEM_ADMIN 권한, 적용 체크섬을 확인한다.

Phase 6 패치 후에는 두 패치의 체크섬을 각각 전달해 검증한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -v target_database=seal_dev \
  -v expected_checksum='<20260824_005 sha256>' \
  -f database/04_verify/009_evidence_rights_retention_verify.sql
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -v target_database=seal_dev \
  -v expected_checksum='<20260824_006 sha256>' \
  -f database/04_verify/010_evidence_queue_backfill_verify.sql
```

`009`는 테이블·FK·CHECK·부분 유일 인덱스·소유권·최소권한과 불변 테이블의 UPDATE/DELETE 거부를 확인한다. `010`은 완료 응답마다 활성 증적 큐가 정확히 하나이고 두 번째 패치의 적용 체크섬이 일치하는지 확인한다.

## 수동 변경 원칙

- 초기 SQL은 빈 `seal_dev` 데이터베이스에 한 번만 실행한다.
- 적용된 SQL 파일은 수정하지 않고 다음 일련번호의 새 SQL을 추가한다.
- 적용 전 SHA-256을 계산하고 파일 경로, 체크섬, 애플리케이션 버전, 적용자와 적용시각을 `seal.db_change_history`에 기록한다.
- 운영 DB 이름은 `seal_prod`처럼 환경을 구분하되 역할 책임은 동일하게 유지한다.
- 비밀번호는 권한 `0600`의 `.pgpass` 또는 보안 비밀 저장소로 전달한다.
