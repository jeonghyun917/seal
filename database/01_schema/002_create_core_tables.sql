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

CREATE TABLE organization_unit (
    organization_unit_id UUID PRIMARY KEY,
    organization_code    VARCHAR(30) NOT NULL UNIQUE,
    organization_name    VARCHAR(100) NOT NULL,
    parent_id             UUID REFERENCES organization_unit (organization_unit_id),
    root_unit             BOOLEAN NOT NULL DEFAULT FALSE,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_organization_code CHECK (organization_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_organization_name CHECK (length(btrim(organization_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_organization_parent CHECK (parent_id IS DISTINCT FROM organization_unit_id),
    CONSTRAINT ck_organization_root_parent CHECK (NOT root_unit OR parent_id IS NULL),
    CONSTRAINT ck_organization_version CHECK (version_no >= 0)
);

CREATE UNIQUE INDEX ux_organization_single_root
    ON organization_unit (root_unit) WHERE root_unit;
CREATE INDEX ix_organization_parent ON organization_unit (parent_id);

CREATE TABLE admin_user (
    admin_user_id        UUID PRIMARY KEY,
    login_id             VARCHAR(50) NOT NULL,
    display_name         VARCHAR(100) NOT NULL,
    organization_unit_id UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    password_hash        VARCHAR(100) NOT NULL,
    account_status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at        TIMESTAMPTZ,
    created_by           UUID REFERENCES admin_user (admin_user_id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           UUID REFERENCES admin_user (admin_user_id),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_admin_login_id CHECK (login_id ~ '^[a-z0-9.-]{4,50}$'),
    CONSTRAINT ck_admin_display_name CHECK (length(btrim(display_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_admin_password_hash CHECK (length(password_hash) BETWEEN 59 AND 100),
    CONSTRAINT ck_admin_account_status CHECK (account_status IN ('ACTIVE', 'LOCKED', 'INACTIVE')),
    CONSTRAINT ck_admin_failures CHECK (consecutive_failures >= 0),
    CONSTRAINT ck_admin_version CHECK (version_no >= 0)
);

CREATE UNIQUE INDEX ux_admin_user_login_id_ci ON admin_user (lower(login_id));
CREATE INDEX ix_admin_user_organization ON admin_user (organization_unit_id);

CREATE TABLE admin_role (
    role_id      UUID PRIMARY KEY,
    role_code    VARCHAR(50) NOT NULL UNIQUE,
    role_name    VARCHAR(100) NOT NULL,
    system_role  BOOLEAN NOT NULL DEFAULT FALSE,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_admin_role_code CHECK (role_code ~ '^[A-Z][A-Z0-9_]{2,49}$'),
    CONSTRAINT ck_admin_role_name CHECK (length(btrim(role_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_admin_role_version CHECK (version_no >= 0)
);

CREATE TABLE permission (
    permission_id   UUID PRIMARY KEY,
    permission_code VARCHAR(80) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    CONSTRAINT ck_permission_code CHECK (permission_code ~ '^[A-Z][A-Z0-9_]{2,79}$'),
    CONSTRAINT ck_permission_name CHECK (length(btrim(permission_name)) BETWEEN 1 AND 100)
);

CREATE TABLE user_role (
    admin_user_id UUID NOT NULL REFERENCES admin_user (admin_user_id),
    role_id       UUID NOT NULL REFERENCES admin_role (role_id),
    assigned_by   UUID REFERENCES admin_user (admin_user_id),
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_user_id, role_id)
);

CREATE INDEX ix_user_role_role ON user_role (role_id, admin_user_id);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL REFERENCES admin_role (role_id),
    permission_id UUID NOT NULL REFERENCES permission (permission_id),
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX ix_role_permission_permission ON role_permission (permission_id, role_id);

-- Phase 1A는 조직 범위만 허용한다. 처리업무 범위는 대상 테이블과 함께 Phase 1B에서 확장한다.
CREATE TABLE user_scope (
    user_scope_id       UUID PRIMARY KEY,
    admin_user_id       UUID NOT NULL REFERENCES admin_user (admin_user_id),
    scope_type          VARCHAR(30) NOT NULL,
    scope_target_id     UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_by         UUID REFERENCES admin_user (admin_user_id),
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_scope UNIQUE (admin_user_id, scope_type, scope_target_id),
    CONSTRAINT ck_user_scope_type CHECK (scope_type = 'ORGANIZATION')
);

CREATE INDEX ix_user_scope_target ON user_scope (scope_type, scope_target_id);

CREATE TABLE audit_event (
    event_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at     TIMESTAMPTZ NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_type      VARCHAR(30) NOT NULL,
    actor_id        UUID,
    action_code     VARCHAR(100) NOT NULL,
    target_type     VARCHAR(100) NOT NULL,
    target_id       VARCHAR(100),
    result          VARCHAR(30) NOT NULL,
    reason_code     VARCHAR(100),
    correlation_id  UUID NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_actor_type CHECK (actor_type IN ('ADMIN', 'SYSTEM', 'ANONYMOUS')),
    CONSTRAINT ck_audit_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_audit_metadata_object CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX ix_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX ix_audit_event_actor ON audit_event (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX ix_audit_event_correlation ON audit_event (correlation_id);

REVOKE ALL ON ALL TABLES IN SCHEMA seal FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA seal FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE ON
    organization_unit,
    admin_user,
    admin_role,
    permission,
    user_role,
    role_permission,
    user_scope
TO seal_app;

GRANT SELECT, INSERT ON audit_event TO seal_app;
REVOKE UPDATE, DELETE, TRUNCATE ON audit_event FROM seal_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA seal TO seal_app;

ALTER DEFAULT PRIVILEGES FOR ROLE seal_owner IN SCHEMA seal
    REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES FOR ROLE seal_owner IN SCHEMA seal
    REVOKE ALL ON SEQUENCES FROM PUBLIC;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '01_schema/002_create_core_tables.sql',
    :'script_checksum',
    :'app_version',
    '관리자 인증, 역할, 조직 범위와 추가 전용 감사 테이블 생성'
);

COMMIT;
