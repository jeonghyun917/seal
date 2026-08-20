# PostgreSQL 16.14 개발 DB 구성

모든 SQL 파일은 UTF-8이며 `psql 16.14`에서 실행한다. Flyway와 Liquibase는 사용하지 않는다.

## 1. DB와 전용 계정 생성

Rocky Linux에서 PostgreSQL 관리 계정으로 실행한다.

```bash
sudo -u postgres psql -f database/00_admin/001_create_dev_database.sql
```

스크립트가 `seal_app` 비밀번호를 대화형으로 묻는다. 비밀번호를 명령행 인자나 셸 이력에 직접 적지 않는다.

## 2. 스키마와 초기 데이터 적용

```bash
psql -X -v ON_ERROR_STOP=1 -U seal_app -d seal_dev -f database/01_schema/001_create_schema.sql
psql -X -v ON_ERROR_STOP=1 -U seal_app -d seal_dev -f database/01_schema/002_create_core_tables.sql
psql -X -v ON_ERROR_STOP=1 -U seal_app -d seal_dev -f database/02_data/001_common_codes.sql
```

비밀번호는 `.pgpass` 파일을 권한 `0600`으로 설정하거나 실행 시 프롬프트로 입력한다.

## 3. 확인

```sql
SHOW server_version;
SHOW timezone;
SHOW search_path;
SELECT script_name, applied_by, applied_at
FROM seal.db_change_history
ORDER BY change_id;
```

예상 서버 버전은 `16.14`, 타임존은 `Asia/Seoul`, 기본 스키마는 `seal, public`이다.

## 주의사항

- 초기 SQL은 빈 `seal_dev` 데이터베이스에 한 번만 실행한다.
- 적용 후 기존 파일을 수정하지 않는다.
- 운영 DB 이름은 `seal_prod`, 계정은 `seal_app`처럼 환경과 역할을 명확히 분리한다.
- 애플리케이션 계정에는 SUPERUSER, CREATEDB, CREATEROLE 권한을 부여하지 않는다.
