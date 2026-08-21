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

ALTER TABLE role_permission
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN revoked_by UUID REFERENCES admin_user (admin_user_id),
    ADD CONSTRAINT ck_role_permission_lifecycle CHECK (
        (active = TRUE AND revoked_at IS NULL AND revoked_by IS NULL)
        OR (active = FALSE AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL)
    );

CREATE INDEX ix_role_permission_active_role
    ON role_permission (role_id, permission_id) WHERE active = TRUE;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260821_003_add_role_permission_lifecycle.sql',
    :'script_checksum',
    :'app_version',
    '역할 권한 배정의 비삭제 해제 수명주기 추가'
);

COMMIT;
