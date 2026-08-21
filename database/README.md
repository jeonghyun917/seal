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

적용 순서:

1. `database/01_schema/001_create_schema.sql`
2. `database/01_schema/002_create_core_tables.sql`
3. `database/02_data/001_common_codes.sql`

각 파일은 PostgreSQL 관리자가 실행하지만 트랜잭션 안에서 `SET LOCAL ROLE seal_owner`를 사용하므로 생성 객체의 소유자는 `seal_owner`가 된다.

## 3. 검증

초기 SQL을 모두 적용한 뒤 관리 계정으로 실행한다.

```bash
sudo -u postgres psql -X -v ON_ERROR_STOP=1 \
  -f database/04_verify/001_phase1a_verify.sql
```

성공 시 `Phase 1A database verification passed`를 출력한다. 역할, 객체 소유권, 필수 테이블, 앱 권한, 감사 테이블의 추가 전용 권한 또는 변경 이력이 기준과 다르면 오류로 종료한다.

## 수동 변경 원칙

- 초기 SQL은 빈 `seal_dev` 데이터베이스에 한 번만 실행한다.
- 적용된 SQL 파일은 수정하지 않고 다음 일련번호의 새 SQL을 추가한다.
- 적용 전 SHA-256을 계산하고 파일 경로, 체크섬, 애플리케이션 버전, 적용자와 적용시각을 `seal.db_change_history`에 기록한다.
- 운영 DB 이름은 `seal_prod`처럼 환경을 구분하되 역할 책임은 동일하게 유지한다.
- 비밀번호는 권한 `0600`의 `.pgpass` 또는 보안 비밀 저장소로 전달한다.
