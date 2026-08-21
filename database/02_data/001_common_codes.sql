\set ON_ERROR_STOP on
\encoding UTF8

\if :{?script_checksum}
\else
  \echo 'script_checksum psql variable is required'
  \quit 3
\endif
\if :{?app_version}
\else
  \echo 'app_version psql variable is required'
  \quit 3
\endif

\connect seal_dev

BEGIN;
SET LOCAL ROLE seal_owner;
SET LOCAL search_path = seal, public;

INSERT INTO organization_unit (
    organization_unit_id, organization_code, organization_name, root_unit
) VALUES (
    '00000000-0000-0000-0000-000000000001', 'ARTE', '한국문화예술교육진흥원', TRUE
);

INSERT INTO admin_role (role_id, role_code, role_name, system_role) VALUES
    ('00000000-0000-0000-0001-000000000001', 'SYSTEM_ADMIN', '시스템 관리자', TRUE),
    ('00000000-0000-0000-0001-000000000002', 'PRIVACY_OFFICER', '개인정보 보호책임 업무담당자', TRUE),
    ('00000000-0000-0000-0001-000000000003', 'DEPARTMENT_MANAGER', '부서 관리자', TRUE),
    ('00000000-0000-0000-0001-000000000004', 'OPERATOR', '업무 담당자', TRUE),
    ('00000000-0000-0000-0001-000000000005', 'AUDITOR', '감사 조회자', TRUE);

INSERT INTO permission (permission_id, permission_code, permission_name) VALUES
    ('00000000-0000-0000-0002-000000000001', 'ORGANIZATION_VIEW', '조직 조회'),
    ('00000000-0000-0000-0002-000000000002', 'ORGANIZATION_MANAGE', '조직 관리'),
    ('00000000-0000-0000-0002-000000000003', 'USER_VIEW', '관리자 조회'),
    ('00000000-0000-0000-0002-000000000004', 'USER_MANAGE', '관리자 관리'),
    ('00000000-0000-0000-0002-000000000005', 'USER_UNLOCK', '관리자 잠금 해제'),
    ('00000000-0000-0000-0002-000000000006', 'USER_RESET_PASSWORD', '관리자 비밀번호 초기화'),
    ('00000000-0000-0000-0002-000000000007', 'ROLE_VIEW', '역할 조회'),
    ('00000000-0000-0000-0002-000000000008', 'ROLE_MANAGE', '역할 관리'),
    ('00000000-0000-0000-0002-000000000009', 'RETENTION_POLICY_VIEW', '보유정책 조회'),
    ('00000000-0000-0000-0002-000000000010', 'RETENTION_POLICY_EDIT', '보유정책 편집'),
    ('00000000-0000-0000-0002-000000000011', 'RETENTION_POLICY_REVIEW', '보유정책 검토 요청'),
    ('00000000-0000-0000-0002-000000000012', 'RETENTION_POLICY_APPROVE', '보유정책 승인'),
    ('00000000-0000-0000-0002-000000000013', 'PROCESSING_ACTIVITY_VIEW', '처리업무 조회'),
    ('00000000-0000-0000-0002-000000000014', 'PROCESSING_ACTIVITY_EDIT', '처리업무 편집'),
    ('00000000-0000-0000-0002-000000000015', 'PROCESSING_ACTIVITY_REVIEW', '처리업무 검토 요청'),
    ('00000000-0000-0000-0002-000000000016', 'PROCESSING_ACTIVITY_APPROVE', '처리업무 승인'),
    ('00000000-0000-0000-0002-000000000017', 'AUDIT_VIEW', '감사 이벤트 조회');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM admin_role r
CROSS JOIN permission p
WHERE r.role_code = 'SYSTEM_ADMIN';

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '02_data/001_common_codes.sql',
    :'script_checksum',
    :'app_version',
    '루트 조직, 기본 역할, 권한과 시스템 관리자 역할 권한 등록'
);

COMMIT;
