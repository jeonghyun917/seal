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
7. `20260824_004_create_import_consent_delivery_tables.sql`
8. `20260824_005_create_evidence_rights_retention_tables.sql`
9. `20260824_006_backfill_evidence_queue.sql`
10. `20260824_007_add_residual_destruction.sql`
11. `20260825_008_harden_rights_scope_integrity.sql`
12. `20260825_009_add_crypto_key_versions.sql`
13. `20260825_010_allow_legal_hold_reasons.sql`
14. `20260826_011_add_template_soft_delete.sql`
15. `20260826_012_add_recipient_affiliation.sql`
16. `20260827_013_add_recipient_job_title.sql`
17. `20260827_014_add_import_row_collection_basis.sql`
18. `20260901_015_add_automatic_retention_start_events.sql`

각 패치는 대응하는 `database/04_verify` 검증 SQL과 함께 적용한다. 템플릿 테이블 패치는 처리업무 승인 버전과 개인정보 항목을 복합 외래키로 고정하므로 반드시 기존 1~3번 패치 다음에 실행한다. 5번 패치는 4번 테이블을 전제로 하며 애플리케이션에 직접 DELETE 권한을 주지 않고 범위 안의 초안 항목만 교체하게 한다. 6번부터는 대상자·요청·메일·동의·증적·권리요청·파기 순으로 기능을 확장한다.

11번 패치는 권리요청 생성과 보존 예외 처리를 관리자 조직 범위 안으로 제한하고, 동의 철회·삭제·처리정지 요청이 원 동의 요청을 참조하도록 무결성을 강화한다. 12번 패치는 권리요청 암호문과 증적 HMAC에 키 식별자를 저장해 키 교체 이후에도 과거 데이터를 검증·복호화할 수 있게 한다. 13번 패치는 같은 동의 요청에서 서로 다른 사유의 활성 보존 보류가 공존하도록 한다. 14번 패치는 템플릿을 물리 삭제하지 않고 삭제자·삭제시각·사유를 보존하는 소프트 삭제와 복구를 지원한다. 15번 패치는 내부 관리 부서와 별도로 동의 수신자의 외부 소속기관을 선택 입력하고 Excel 반입 임시값까지 동일하게 관리한다. 16번 패치는 동의 수신자의 선택 직책을 수동 등록과 Excel 반입에서 함께 관리하고 파기 시 제거한다. 17번 패치는 Excel 공통 입력값이던 수신자 정보 등록 근거를 각 반입 행에 필수로 저장하고 반입 완료·만료 시 임시값을 제거한다.

18번 패치는 기존 정책 값을 보존하면서 메일 최초 발송일과 동의 요청 만료일을 자동 보유기간 시작 기준으로 추가한다.

`database/04_verify/001_phase1a_verify.sql`과 `002_phase1b_verify.sql`은 초기 스키마 검증이며, 패치별 검증은 3번부터 순서대로 대응한다. 적용 이력의 파일명과 SHA-256이 현재 파일과 일치하는지 확인한 후 다음 패치를 적용한다.
