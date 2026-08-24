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

ALTER TABLE processing_activity_version
    ADD CONSTRAINT uq_processing_version_retention_pair
    UNIQUE (processing_activity_version_id, retention_policy_version_id);

CREATE TABLE recipient (
    recipient_id               UUID PRIMARY KEY,
    organization_unit_id       UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    recipient_name_ciphertext  BYTEA NOT NULL,
    recipient_email_ciphertext BYTEA NOT NULL,
    encryption_key_id          VARCHAR(30) NOT NULL,
    email_lookup_hmac          CHAR(64) NOT NULL,
    collection_source          VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    collection_basis           VARCHAR(500) NOT NULL,
    recipient_status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by                 UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_recipient_organization_pair UNIQUE (recipient_id, organization_unit_id),
    CONSTRAINT ck_recipient_name_cipher CHECK (octet_length(recipient_name_ciphertext) >= 30),
    CONSTRAINT ck_recipient_email_cipher CHECK (octet_length(recipient_email_ciphertext) >= 30),
    CONSTRAINT ck_recipient_key_id CHECK (encryption_key_id ~ '^[A-Za-z0-9._-]{1,30}$'),
    CONSTRAINT ck_recipient_email_hmac CHECK (email_lookup_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_recipient_source CHECK (collection_source = 'MANUAL'),
    CONSTRAINT ck_recipient_basis CHECK (length(btrim(collection_basis)) BETWEEN 5 AND 500),
    CONSTRAINT ck_recipient_status CHECK (recipient_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_recipient_version CHECK (version_no >= 0)
);

CREATE UNIQUE INDEX ux_recipient_active_email
    ON recipient (organization_unit_id, email_lookup_hmac)
    WHERE recipient_status = 'ACTIVE';
CREATE INDEX ix_recipient_organization_status
    ON recipient (organization_unit_id, recipient_status, created_at DESC);

CREATE TABLE consent_request (
    consent_request_id             UUID PRIMARY KEY,
    request_code                   VARCHAR(20) NOT NULL UNIQUE,
    organization_unit_id           UUID NOT NULL,
    recipient_id                   UUID NOT NULL,
    processing_activity_id         UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    consent_template_id            UUID NOT NULL,
    consent_template_version_id    UUID NOT NULL,
    retention_policy_version_id    UUID NOT NULL,
    request_status                 VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    expires_at                     TIMESTAMPTZ NOT NULL,
    idempotency_key                UUID NOT NULL,
    current_secure_link_id         UUID,
    snapshot_id                    UUID,
    cancelled_by                   UUID REFERENCES admin_user (admin_user_id),
    cancelled_at                   TIMESTAMPTZ,
    cancellation_reason            VARCHAR(500),
    created_by                     UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                     UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_request_org_idempotency UNIQUE (organization_unit_id, idempotency_key),
    CONSTRAINT uq_request_organization_pair UNIQUE (consent_request_id, organization_unit_id),
    CONSTRAINT fk_request_recipient_organization FOREIGN KEY (recipient_id, organization_unit_id)
        REFERENCES recipient (recipient_id, organization_unit_id),
    CONSTRAINT fk_request_processing_activity FOREIGN KEY
        (processing_activity_version_id, processing_activity_id)
        REFERENCES processing_activity_version
            (processing_activity_version_id, processing_activity_id),
    CONSTRAINT fk_request_processing_retention FOREIGN KEY
        (processing_activity_version_id, retention_policy_version_id)
        REFERENCES processing_activity_version
            (processing_activity_version_id, retention_policy_version_id),
    CONSTRAINT fk_request_template_activity FOREIGN KEY
        (consent_template_id, processing_activity_id)
        REFERENCES consent_template (consent_template_id, processing_activity_id),
    CONSTRAINT fk_request_template_version FOREIGN KEY
        (consent_template_version_id, consent_template_id)
        REFERENCES consent_template_version
            (consent_template_version_id, consent_template_id),
    CONSTRAINT fk_request_template_processing_version FOREIGN KEY
        (consent_template_version_id, processing_activity_version_id)
        REFERENCES consent_template_version
            (consent_template_version_id, processing_activity_version_id),
    CONSTRAINT ck_request_code CHECK (request_code ~ '^CR-[0-9]{8}-[A-Z0-9]{8}$'),
    CONSTRAINT ck_request_status CHECK
        (request_status IN ('DRAFT', 'READY', 'ACTIVE', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_request_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_request_draft_pointers CHECK
        (request_status <> 'DRAFT' OR (snapshot_id IS NULL AND current_secure_link_id IS NULL)),
    CONSTRAINT ck_request_snapshot_required CHECK
        (request_status NOT IN ('READY', 'ACTIVE', 'COMPLETED') OR snapshot_id IS NOT NULL),
    CONSTRAINT ck_request_link_required CHECK
        (request_status NOT IN ('ACTIVE', 'COMPLETED') OR current_secure_link_id IS NOT NULL),
    CONSTRAINT ck_request_cancellation CHECK (
        (request_status = 'CANCELLED' AND cancelled_by IS NOT NULL
            AND cancelled_at IS NOT NULL
            AND length(btrim(cancellation_reason)) BETWEEN 5 AND 500)
        OR
        (request_status <> 'CANCELLED' AND cancelled_by IS NULL
            AND cancelled_at IS NULL AND cancellation_reason IS NULL)
    ),
    CONSTRAINT ck_request_version CHECK (version_no >= 0)
);

CREATE INDEX ix_request_organization_status
    ON consent_request (organization_unit_id, request_status, created_at DESC);
CREATE INDEX ix_request_processing_status
    ON consent_request (processing_activity_id, request_status, created_at DESC);
CREATE INDEX ix_request_recipient
    ON consent_request (recipient_id, created_at DESC);
CREATE INDEX ix_request_due
    ON consent_request (expires_at, consent_request_id)
    WHERE request_status IN ('READY', 'ACTIVE');

CREATE TABLE consent_request_snapshot (
    consent_request_snapshot_id UUID PRIMARY KEY,
    consent_request_id          UUID NOT NULL UNIQUE
        REFERENCES consent_request (consent_request_id),
    snapshot_schema_version     INTEGER NOT NULL DEFAULT 1,
    snapshot_json               JSONB NOT NULL,
    snapshot_hash               CHAR(64) NOT NULL,
    template_content_hash       CHAR(64) NOT NULL,
    created_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_snapshot_request_pair UNIQUE
        (consent_request_snapshot_id, consent_request_id),
    CONSTRAINT ck_snapshot_schema_version CHECK (snapshot_schema_version = 1),
    CONSTRAINT ck_snapshot_json_object CHECK (jsonb_typeof(snapshot_json) = 'object'),
    CONSTRAINT ck_snapshot_hash CHECK (snapshot_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_snapshot_template_hash CHECK (template_content_hash ~ '^[0-9a-f]{64}$')
);

ALTER TABLE consent_request
    ADD CONSTRAINT fk_request_snapshot FOREIGN KEY (snapshot_id, consent_request_id)
    REFERENCES consent_request_snapshot (consent_request_snapshot_id, consent_request_id);

CREATE TABLE consent_request_item (
    consent_request_item_id     UUID PRIMARY KEY,
    consent_request_snapshot_id UUID NOT NULL
        REFERENCES consent_request_snapshot (consent_request_snapshot_id),
    item_order                  INTEGER NOT NULL,
    item_code                   VARCHAR(30) NOT NULL,
    item_type                   VARCHAR(30) NOT NULL,
    item_title                  VARCHAR(200) NOT NULL,
    required                    BOOLEAN NOT NULL,
    purpose_text                VARCHAR(1000) NOT NULL,
    retention_text              VARCHAR(1000) NOT NULL,
    refusal_notice              VARCHAR(1000) NOT NULL,
    third_party_recipient       VARCHAR(500),
    CONSTRAINT uq_request_item_order UNIQUE (consent_request_snapshot_id, item_order),
    CONSTRAINT uq_request_item_code UNIQUE (consent_request_snapshot_id, item_code),
    CONSTRAINT uq_request_item_snapshot_pair UNIQUE
        (consent_request_item_id, consent_request_snapshot_id),
    CONSTRAINT ck_request_item_order CHECK (item_order >= 1),
    CONSTRAINT ck_request_item_code CHECK (item_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_request_item_type CHECK
        (item_type IN ('COLLECTION_USE', 'THIRD_PARTY_PROVISION', 'OPTIONAL')),
    CONSTRAINT ck_request_item_title CHECK (length(btrim(item_title)) BETWEEN 5 AND 200),
    CONSTRAINT ck_request_item_purpose CHECK (length(btrim(purpose_text)) BETWEEN 5 AND 1000),
    CONSTRAINT ck_request_item_retention CHECK (length(btrim(retention_text)) BETWEEN 5 AND 1000),
    CONSTRAINT ck_request_item_refusal CHECK (length(btrim(refusal_notice)) BETWEEN 10 AND 1000),
    CONSTRAINT ck_request_item_optional CHECK (item_type <> 'OPTIONAL' OR required = FALSE),
    CONSTRAINT ck_request_item_third_party CHECK (
        (item_type = 'THIRD_PARTY_PROVISION') =
        (third_party_recipient IS NOT NULL AND length(btrim(third_party_recipient)) > 0)
    )
);

CREATE TABLE consent_request_item_data (
    consent_request_item_id     UUID NOT NULL,
    consent_request_snapshot_id UUID NOT NULL,
    processing_data_item_id     UUID NOT NULL REFERENCES processing_data_item (processing_data_item_id),
    item_order                  INTEGER NOT NULL,
    item_name                   VARCHAR(100) NOT NULL,
    PRIMARY KEY (consent_request_item_id, processing_data_item_id),
    CONSTRAINT uq_request_item_data_order UNIQUE (consent_request_item_id, item_order),
    CONSTRAINT fk_request_item_data_parent FOREIGN KEY
        (consent_request_item_id, consent_request_snapshot_id)
        REFERENCES consent_request_item
            (consent_request_item_id, consent_request_snapshot_id),
    CONSTRAINT ck_request_item_data_order CHECK (item_order >= 1),
    CONSTRAINT ck_request_item_data_name CHECK (length(btrim(item_name)) BETWEEN 1 AND 100)
);

CREATE TABLE consent_request_event (
    consent_request_event_id UUID PRIMARY KEY,
    consent_request_id       UUID NOT NULL REFERENCES consent_request (consent_request_id),
    event_type               VARCHAR(30) NOT NULL,
    actor_type               VARCHAR(20) NOT NULL,
    actor_id                 UUID REFERENCES admin_user (admin_user_id),
    reason_code              VARCHAR(100),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_request_event_type CHECK
        (event_type IN ('CREATED', 'FINALIZED', 'ACTIVATED', 'LINK_REISSUED',
                        'CANCELLED', 'EXPIRED', 'COMPLETED')),
    CONSTRAINT ck_request_event_actor_type CHECK (actor_type IN ('ADMIN', 'SYSTEM')),
    CONSTRAINT ck_request_event_actor CHECK
        ((actor_type = 'ADMIN' AND actor_id IS NOT NULL)
         OR (actor_type = 'SYSTEM' AND actor_id IS NULL)),
    CONSTRAINT ck_request_event_reason CHECK
        (reason_code IS NULL OR length(btrim(reason_code)) BETWEEN 1 AND 100)
);

CREATE INDEX ix_request_event_request
    ON consent_request_event (consent_request_id, created_at DESC);

CREATE TABLE secure_link (
    secure_link_id     UUID PRIMARY KEY,
    consent_request_id UUID NOT NULL REFERENCES consent_request (consent_request_id),
    token_hmac         CHAR(64) NOT NULL UNIQUE,
    link_status        VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    expires_at         TIMESTAMPTZ NOT NULL,
    issued_by          UUID NOT NULL REFERENCES admin_user (admin_user_id),
    issued_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accessed_at        TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    expired_at         TIMESTAMPTZ,
    revoked_by         UUID REFERENCES admin_user (admin_user_id),
    revoked_at         TIMESTAMPTZ,
    revoke_reason      VARCHAR(100),
    version_no         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_secure_link_request_pair UNIQUE (secure_link_id, consent_request_id),
    CONSTRAINT ck_secure_link_hmac CHECK (token_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_secure_link_status CHECK
        (link_status IN ('ISSUED', 'ACCESSED', 'COMPLETED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT ck_secure_link_expiry CHECK (expires_at > issued_at),
    CONSTRAINT ck_secure_link_completion CHECK
        ((link_status = 'COMPLETED') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_secure_link_expired CHECK
        ((link_status = 'EXPIRED') = (expired_at IS NOT NULL)),
    CONSTRAINT ck_secure_link_revocation CHECK (
        (link_status = 'REVOKED' AND revoked_by IS NOT NULL AND revoked_at IS NOT NULL
            AND revoke_reason IS NOT NULL)
        OR
        (link_status <> 'REVOKED' AND revoked_by IS NULL AND revoked_at IS NULL
            AND revoke_reason IS NULL)
    ),
    CONSTRAINT ck_secure_link_version CHECK (version_no >= 0)
);

CREATE UNIQUE INDEX ux_secure_link_current_per_request
    ON secure_link (consent_request_id)
    WHERE link_status IN ('ISSUED', 'ACCESSED');
CREATE INDEX ix_secure_link_request
    ON secure_link (consent_request_id, issued_at DESC);
CREATE INDEX ix_secure_link_due
    ON secure_link (expires_at, secure_link_id)
    WHERE link_status IN ('ISSUED', 'ACCESSED');

ALTER TABLE consent_request
    ADD CONSTRAINT fk_request_current_link
    FOREIGN KEY (current_secure_link_id, consent_request_id)
    REFERENCES secure_link (secure_link_id, consent_request_id);

INSERT INTO permission (permission_id, permission_code, permission_name) VALUES
    ('00000000-0000-0000-0002-000000000023', 'RECIPIENT_VIEW', '대상자 조회'),
    ('00000000-0000-0000-0002-000000000024', 'RECIPIENT_EDIT', '대상자 편집'),
    ('00000000-0000-0000-0002-000000000025', 'CONSENT_REQUEST_VIEW', '동의 요청 조회'),
    ('00000000-0000-0000-0002-000000000026', 'CONSENT_REQUEST_EDIT', '동의 요청 편집'),
    ('00000000-0000-0000-0002-000000000027', 'CONSENT_REQUEST_ACTIVATE', '동의 요청 활성화'),
    ('00000000-0000-0000-0002-000000000028', 'CONSENT_REQUEST_CANCEL', '동의 요청 취소'),
    ('00000000-0000-0000-0002-000000000029', 'CONSENT_REQUEST_LINK_REISSUE', '동의 요청 링크 재발급');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
  FROM admin_role r
 CROSS JOIN permission p
 WHERE r.role_code = 'SYSTEM_ADMIN'
   AND p.permission_code IN (
       'RECIPIENT_VIEW', 'RECIPIENT_EDIT', 'CONSENT_REQUEST_VIEW',
       'CONSENT_REQUEST_EDIT', 'CONSENT_REQUEST_ACTIVATE',
       'CONSENT_REQUEST_CANCEL', 'CONSENT_REQUEST_LINK_REISSUE'
   );

REVOKE ALL ON
    recipient,
    consent_request,
    consent_request_snapshot,
    consent_request_item,
    consent_request_item_data,
    consent_request_event,
    secure_link
FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE ON
    recipient,
    consent_request,
    secure_link
TO seal_app;

GRANT SELECT, INSERT ON
    consent_request_snapshot,
    consent_request_item,
    consent_request_item_data,
    consent_request_event
TO seal_app;

REVOKE DELETE, TRUNCATE ON
    recipient,
    consent_request,
    consent_request_snapshot,
    consent_request_item,
    consent_request_item_data,
    consent_request_event,
    secure_link
FROM seal_app;

REVOKE UPDATE ON
    consent_request_snapshot,
    consent_request_item,
    consent_request_item_data,
    consent_request_event
FROM seal_app;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260824_003_create_recipient_request_tables.sql',
    :'script_checksum',
    :'app_version',
    '암호화 대상자, 불변 요청 스냅샷, 요청 상태 이력과 일회용 링크 기반 생성'
);

COMMIT;
