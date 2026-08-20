\set ON_ERROR_STOP on
\encoding UTF8

BEGIN;
SET LOCAL search_path = seal, public;

CREATE TABLE system_setting (
    setting_key    VARCHAR(150) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   VARCHAR(500),
    updated_by    VARCHAR(100) NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_system_setting_no_secret
        CHECK (lower(setting_key) !~ '(password|secret|token|credential)')
);

CREATE TABLE common_code (
    code_group   VARCHAR(50) NOT NULL,
    code_value   VARCHAR(50) NOT NULL,
    code_name    VARCHAR(100) NOT NULL,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    description  VARCHAR(500),
    PRIMARY KEY (code_group, code_value)
);

CREATE TABLE mail_outbox (
    mail_id              UUID PRIMARY KEY,
    request_id           UUID NOT NULL,
    idempotency_key      VARCHAR(100) NOT NULL UNIQUE,
    recipient_email      VARCHAR(320) NOT NULL,
    sender_email         VARCHAR(320) NOT NULL,
    sender_name          VARCHAR(100) NOT NULL,
    subject              VARCHAR(300) NOT NULL,
    html_body            TEXT NOT NULL,
    mail_status          VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count        INTEGER NOT NULL DEFAULT 0,
    next_attempt_at      TIMESTAMPTZ,
    smtp_message_id      VARCHAR(500),
    last_error_code      VARCHAR(100),
    last_error_message   VARCHAR(1000),
    smtp_accepted_at     TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mail_outbox_status CHECK (
        mail_status IN ('QUEUED', 'PROCESSING', 'SMTP_ACCEPTED', 'SEND_FAILED', 'RETRY_SCHEDULED')
    ),
    CONSTRAINT ck_mail_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_mail_outbox_sender_domain CHECK (lower(sender_email) LIKE '%@arte.or.kr')
);

CREATE INDEX ix_mail_outbox_dispatch
    ON mail_outbox (mail_status, next_attempt_at, created_at);
CREATE INDEX ix_mail_outbox_request
    ON mail_outbox (request_id, created_at DESC);

CREATE TABLE secure_link (
    secure_link_id UUID PRIMARY KEY,
    request_id      UUID NOT NULL,
    token_hash      CHAR(64) NOT NULL UNIQUE,
    link_status     VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    expires_at      TIMESTAMPTZ NOT NULL,
    first_access_at TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_secure_link_status CHECK (
        link_status IN ('ISSUED', 'ACCESSED', 'EXPIRED', 'REVOKED', 'COMPLETED')
    ),
    CONSTRAINT ck_secure_link_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_secure_link_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_secure_link_request
    ON secure_link (request_id, created_at DESC);
CREATE INDEX ix_secure_link_expiry
    ON secure_link (link_status, expires_at);

CREATE TABLE consent_response (
    response_id       UUID PRIMARY KEY,
    request_id        UUID NOT NULL UNIQUE,
    response_status   VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    decision_snapshot JSONB,
    submitted_at      TIMESTAMPTZ,
    server_received_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_consent_response_status CHECK (
        response_status IN ('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'DECLINED_ALL')
    ),
    CONSTRAINT ck_consent_response_submission CHECK (
        (response_status IN ('SUBMITTED', 'DECLINED_ALL') AND submitted_at IS NOT NULL AND server_received_at IS NOT NULL)
        OR response_status IN ('NOT_STARTED', 'IN_PROGRESS')
    )
);

CREATE TABLE audit_event (
    event_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at            TIMESTAMPTZ NOT NULL,
    received_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_type             VARCHAR(30) NOT NULL,
    actor_id               VARCHAR(100),
    action                 VARCHAR(100) NOT NULL,
    target_type            VARCHAR(100) NOT NULL,
    target_id              VARCHAR(100),
    result                 VARCHAR(30) NOT NULL,
    reason_code            VARCHAR(100),
    source_ip_policy_value VARCHAR(100),
    user_agent_policy_value VARCHAR(500),
    correlation_id         UUID NOT NULL,
    metadata               JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_event_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX ix_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX ix_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX ix_audit_event_correlation ON audit_event (correlation_id);

INSERT INTO db_change_history (script_name, description)
VALUES ('01_schema/002_create_core_tables.sql', '시스템 설정, 메일, 보안 링크, 응답, 감사 기반 테이블 생성');

COMMIT;
