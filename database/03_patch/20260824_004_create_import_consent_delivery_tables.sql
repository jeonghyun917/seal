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
\if :{?target_database}
\else
  \set target_database seal_dev
\endif
\connect :target_database

BEGIN;
SET LOCAL ROLE seal_owner;
SET LOCAL search_path = seal, public;

ALTER TABLE recipient DROP CONSTRAINT ck_recipient_source;
ALTER TABLE recipient ADD CONSTRAINT ck_recipient_source
    CHECK (collection_source IN ('MANUAL', 'EXCEL'));

ALTER TABLE consent_request_event DROP CONSTRAINT ck_request_event_actor_type;
ALTER TABLE consent_request_event DROP CONSTRAINT ck_request_event_actor;
ALTER TABLE consent_request_event ADD CONSTRAINT ck_request_event_actor_type
    CHECK (actor_type IN ('ADMIN', 'SYSTEM', 'PUBLIC'));
ALTER TABLE consent_request_event ADD CONSTRAINT ck_request_event_actor
    CHECK ((actor_type = 'ADMIN' AND actor_id IS NOT NULL)
        OR (actor_type IN ('SYSTEM', 'PUBLIC') AND actor_id IS NULL));

ALTER TABLE audit_event DROP CONSTRAINT ck_audit_actor_type;
ALTER TABLE audit_event ADD CONSTRAINT ck_audit_actor_type
    CHECK (actor_type IN ('ADMIN', 'SYSTEM', 'ANONYMOUS', 'PUBLIC'));

CREATE TABLE recipient_import_batch (
    recipient_import_batch_id UUID PRIMARY KEY,
    organization_unit_id UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    file_sha256 CHAR(64) NOT NULL,
    malware_scan_status VARCHAR(20) NOT NULL,
    collection_basis VARCHAR(500) NOT NULL,
    import_status VARCHAR(20) NOT NULL DEFAULT 'VALIDATED',
    total_count INTEGER NOT NULL,
    valid_count INTEGER NOT NULL,
    duplicate_count INTEGER NOT NULL,
    error_count INTEGER NOT NULL,
    imported_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    version_no BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_import_batch_sha CHECK (file_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_import_batch_scan CHECK (malware_scan_status IN ('CLEAN','DISABLED')),
    CONSTRAINT ck_import_batch_status CHECK (import_status IN ('VALIDATED','IMPORTED','EXPIRED')),
    CONSTRAINT ck_import_batch_counts CHECK (total_count >= 0 AND valid_count >= 0 AND duplicate_count >= 0
        AND error_count >= 0 AND imported_count >= 0
        AND total_count = valid_count + duplicate_count + error_count),
    CONSTRAINT ck_import_batch_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_import_batch_owner ON recipient_import_batch (created_by, created_at DESC);
CREATE INDEX ix_import_batch_expiry ON recipient_import_batch (expires_at)
    WHERE import_status = 'VALIDATED';

CREATE TABLE recipient_import_row (
    recipient_import_batch_id UUID NOT NULL REFERENCES recipient_import_batch (recipient_import_batch_id),
    row_number INTEGER NOT NULL,
    recipient_name_ciphertext BYTEA,
    recipient_email_ciphertext BYTEA,
    encryption_key_id VARCHAR(30),
    email_lookup_hmac CHAR(64),
    masked_name VARCHAR(120) NOT NULL,
    masked_email VARCHAR(260) NOT NULL,
    row_status VARCHAR(20) NOT NULL,
    error_codes VARCHAR(500),
    imported_recipient_id UUID REFERENCES recipient (recipient_id),
    PRIMARY KEY (recipient_import_batch_id, row_number),
    CONSTRAINT ck_import_row_number CHECK (row_number BETWEEN 2 AND 1001),
    CONSTRAINT ck_import_row_status CHECK (row_status IN ('VALID','DUPLICATE','ERROR','IMPORTED','PURGED')),
    CONSTRAINT ck_import_row_cipher CHECK (
        (row_status IN ('VALID','DUPLICATE') AND recipient_name_ciphertext IS NOT NULL
            AND recipient_email_ciphertext IS NOT NULL AND encryption_key_id IS NOT NULL
            AND email_lookup_hmac IS NOT NULL)
        OR row_status IN ('ERROR','IMPORTED','PURGED'))
);

CREATE TABLE public_consent_session (
    public_consent_session_id UUID PRIMARY KEY,
    flow_hmac CHAR(64) NOT NULL UNIQUE,
    consent_request_id UUID NOT NULL REFERENCES consent_request (consent_request_id),
    secure_link_id UUID NOT NULL REFERENCES secure_link (secure_link_id),
    session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_public_session_request UNIQUE (public_consent_session_id, consent_request_id),
    CONSTRAINT ck_public_session_hmac CHECK (flow_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_public_session_status CHECK (session_status IN ('ACTIVE','COMPLETED','EXPIRED')),
    CONSTRAINT ck_public_session_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_public_session_expiry ON public_consent_session (expires_at)
    WHERE session_status = 'ACTIVE';

CREATE TABLE consent_response (
    consent_response_id UUID PRIMARY KEY,
    consent_request_id UUID NOT NULL UNIQUE REFERENCES consent_request (consent_request_id),
    public_consent_session_id UUID NOT NULL,
    response_status VARCHAR(20) NOT NULL,
    idempotency_key UUID NOT NULL,
    snapshot_hash CHAR(64) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_response_request_idempotency UNIQUE (consent_request_id, idempotency_key),
    CONSTRAINT fk_response_session_request FOREIGN KEY (public_consent_session_id, consent_request_id)
        REFERENCES public_consent_session (public_consent_session_id, consent_request_id),
    CONSTRAINT ck_response_status CHECK (response_status IN ('SUBMITTED','DECLINED_ALL')),
    CONSTRAINT ck_response_snapshot_hash CHECK (snapshot_hash ~ '^[0-9a-f]{64}$')
);

CREATE TABLE consent_response_item (
    consent_response_id UUID NOT NULL REFERENCES consent_response (consent_response_id),
    consent_request_item_id UUID NOT NULL REFERENCES consent_request_item (consent_request_item_id),
    decision VARCHAR(20) NOT NULL,
    item_order INTEGER NOT NULL,
    PRIMARY KEY (consent_response_id, consent_request_item_id),
    CONSTRAINT ck_response_item_decision CHECK (decision IN ('AGREE','DISAGREE')),
    CONSTRAINT ck_response_item_order CHECK (item_order >= 1)
);

CREATE TABLE mail_outbox (
    mail_outbox_id UUID PRIMARY KEY,
    consent_request_id UUID NOT NULL REFERENCES consent_request (consent_request_id),
    secure_link_id UUID NOT NULL REFERENCES secure_link (secure_link_id),
    recipient_email_ciphertext BYTEA NOT NULL,
    html_ciphertext BYTEA NOT NULL,
    encryption_key_id VARCHAR(30) NOT NULL,
    mail_subject VARCHAR(200) NOT NULL,
    html_sha256 CHAR(64) NOT NULL,
    mail_status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    scheduled_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMPTZ,
    smtp_accepted_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    created_by UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_mail_outbox_email CHECK (octet_length(recipient_email_ciphertext) >= 30),
    CONSTRAINT ck_mail_outbox_html CHECK (octet_length(html_ciphertext) >= 30),
    CONSTRAINT ck_mail_outbox_sha CHECK (html_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_mail_outbox_status CHECK (mail_status IN
        ('QUEUED','PROCESSING','SMTP_ACCEPTED','RETRY_SCHEDULED','SEND_FAILED','CANCELLED')),
    CONSTRAINT ck_mail_outbox_attempt CHECK (attempt_count BETWEEN 0 AND 4)
);
CREATE INDEX ix_mail_outbox_due ON mail_outbox (next_attempt_at, mail_outbox_id)
    WHERE mail_status IN ('QUEUED','RETRY_SCHEDULED','PROCESSING');
CREATE INDEX ix_mail_outbox_request ON mail_outbox (consent_request_id, created_at DESC);

CREATE TABLE mail_delivery_attempt (
    mail_delivery_attempt_id UUID PRIMARY KEY,
    mail_outbox_id UUID NOT NULL REFERENCES mail_outbox (mail_outbox_id),
    attempt_number INTEGER NOT NULL,
    attempt_result VARCHAR(30) NOT NULL,
    reason_code VARCHAR(100),
    attempted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_mail_attempt_number CHECK (attempt_number BETWEEN 1 AND 4),
    CONSTRAINT ck_mail_attempt_result CHECK (attempt_result IN ('SMTP_ACCEPTED','RETRY_SCHEDULED','SEND_FAILED'))
);

INSERT INTO permission (permission_id, permission_code, permission_name) VALUES
    ('00000000-0000-0000-0002-000000000030', 'RECIPIENT_IMPORT', '대상자 Excel 반입'),
    ('00000000-0000-0000-0002-000000000031', 'MAIL_SEND', '동의 메일 발송'),
    ('00000000-0000-0000-0002-000000000032', 'MAIL_VIEW', '동의 메일 조회'),
    ('00000000-0000-0000-0002-000000000033', 'MAIL_RETRY', '동의 메일 재시도');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM admin_role r CROSS JOIN permission p
 WHERE r.role_code = 'SYSTEM_ADMIN'
   AND p.permission_code IN ('RECIPIENT_IMPORT','MAIL_SEND','MAIL_VIEW','MAIL_RETRY');

REVOKE ALL ON recipient_import_batch, recipient_import_row, public_consent_session,
    consent_response, consent_response_item, mail_outbox, mail_delivery_attempt FROM PUBLIC;
GRANT SELECT, INSERT, UPDATE ON recipient_import_batch, recipient_import_row,
    public_consent_session, mail_outbox TO seal_app;
GRANT SELECT, INSERT ON consent_response, consent_response_item, mail_delivery_attempt TO seal_app;
REVOKE DELETE, TRUNCATE ON recipient_import_batch, recipient_import_row, public_consent_session,
    consent_response, consent_response_item, mail_outbox, mail_delivery_attempt FROM seal_app;
REVOKE UPDATE ON consent_response, consent_response_item, mail_delivery_attempt FROM seal_app;

INSERT INTO db_change_history (script_name, script_checksum, application_version, description)
VALUES ('03_patch/20260824_004_create_import_consent_delivery_tables.sql', :'script_checksum',
        :'app_version', 'Excel 임시 반입, 공개 동의 세션과 응답, 암호화 메일 Outbox 기반 생성');

COMMIT;
