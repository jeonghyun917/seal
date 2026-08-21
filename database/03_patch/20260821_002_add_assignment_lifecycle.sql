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

ALTER TABLE user_role
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN revoked_by UUID REFERENCES admin_user (admin_user_id),
    ADD CONSTRAINT ck_user_role_lifecycle CHECK (
        (active = TRUE AND revoked_at IS NULL AND revoked_by IS NULL)
        OR (active = FALSE AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL)
    );

ALTER TABLE user_scope
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN revoked_by UUID REFERENCES admin_user (admin_user_id),
    ADD CONSTRAINT ck_user_scope_lifecycle CHECK (
        (active = TRUE AND revoked_at IS NULL AND revoked_by IS NULL)
        OR (active = FALSE AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL)
    );

CREATE INDEX ix_user_role_active_user ON user_role (admin_user_id, role_id) WHERE active = TRUE;
CREATE INDEX ix_user_scope_active_user ON user_scope (admin_user_id, scope_target_id) WHERE active = TRUE;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260821_002_add_assignment_lifecycle.sql',
    :'script_checksum',
    :'app_version',
    '역할과 조직 범위 배정의 비삭제 해제 수명주기 추가'
);

COMMIT;
