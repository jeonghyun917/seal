# DB 패치 파일 규칙

Flyway나 Liquibase를 사용하지 않으므로 운영 반영 SQL은 이 폴더에서 수동 관리한다.

- 파일명: `YYYYMMDD_NNN_설명.sql`
- 모든 파일 첫 줄에 `\set ON_ERROR_STOP on`을 둔다.
- 가능한 변경은 `BEGIN`과 `COMMIT` 사이에서 수행한다.
- 이미 적용된 파일은 수정하지 않고 새 패치 파일을 추가한다.
- 적용 직전에 파일의 SHA-256을 계산하고 `seal.db_change_history`에 기록한다.
- DDL과 애플리케이션 배포 순서, 롤백 SQL, 예상 잠금 시간을 배포 작업서에 기록한다.
- 파괴적인 컬럼 삭제와 데이터 삭제는 백업 및 복구 시험 승인 전에는 실행하지 않는다.

## 현재 적용 순서

1. `20260821_001_create_policy_tables.sql`
2. `20260821_002_add_assignment_lifecycle.sql`
3. `20260821_003_add_role_permission_lifecycle.sql`
4. `20260824_001_create_consent_template_tables.sql`
5. `20260824_002_add_template_draft_replace_function.sql`
6. `20260824_003_create_recipient_request_tables.sql`

각 패치는 대응하는 `database/04_verify` 검증 SQL과 함께 적용한다. 템플릿 테이블 패치는 처리업무 승인 버전과 개인정보 항목을 복합 외래키로 고정하므로 반드시 기존 1~3번 패치 다음에 실행한다. 5번 패치는 4번 테이블을 전제로 하며 애플리케이션에 직접 DELETE 권한을 주지 않고 범위 안의 초안 항목만 교체하게 한다. 6번 패치는 발행 템플릿을 전제로 하며 암호화 대상자, 불변 요청 스냅샷, 요청 이벤트와 일회용 링크 기반을 추가한다.
