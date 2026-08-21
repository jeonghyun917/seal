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

CREATE TABLE retention_policy (
    retention_policy_id        UUID PRIMARY KEY,
    policy_code                VARCHAR(30) NOT NULL UNIQUE,
    policy_name                VARCHAR(100) NOT NULL,
    owner_organization_unit_id UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    policy_status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_approved_version_id UUID,
    created_by                 UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_retention_policy_code CHECK (policy_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_retention_policy_name CHECK (length(btrim(policy_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_retention_policy_status CHECK (policy_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_retention_policy_version CHECK (version_no >= 0)
);

CREATE INDEX ix_retention_policy_owner
    ON retention_policy (owner_organization_unit_id, policy_status);

CREATE TABLE retention_policy_version (
    retention_policy_version_id UUID PRIMARY KEY,
    retention_policy_id         UUID NOT NULL REFERENCES retention_policy (retention_policy_id),
    version_number              INTEGER NOT NULL,
    workflow_status             VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    start_event                 VARCHAR(40) NOT NULL,
    period_value                INTEGER NOT NULL,
    period_unit                 VARCHAR(10) NOT NULL,
    grace_period_days           INTEGER NOT NULL DEFAULT 0,
    destruction_method          VARCHAR(30) NOT NULL,
    created_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_requested_by         UUID REFERENCES admin_user (admin_user_id),
    review_requested_at         TIMESTAMPTZ,
    approved_by                 UUID REFERENCES admin_user (admin_user_id),
    approved_at                 TIMESTAMPTZ,
    rejection_comment           VARCHAR(1000),
    updated_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_retention_policy_version UNIQUE (retention_policy_id, version_number),
    CONSTRAINT ck_retention_version_number CHECK (version_number >= 1),
    CONSTRAINT ck_retention_workflow CHECK (workflow_status IN ('DRAFT', 'REVIEW_REQUESTED', 'APPROVED', 'SUPERSEDED')),
    CONSTRAINT ck_retention_start_event CHECK (start_event IN ('CONSENT_SUBMITTED_AT', 'REQUEST_COMPLETED_AT', 'PROGRAM_END_AT', 'PURPOSE_ACHIEVED_AT')),
    CONSTRAINT ck_retention_period CHECK (period_value BETWEEN 1 AND 100),
    CONSTRAINT ck_retention_period_unit CHECK (period_unit IN ('DAY', 'MONTH', 'YEAR')),
    CONSTRAINT ck_retention_grace CHECK (grace_period_days BETWEEN 0 AND 365),
    CONSTRAINT ck_retention_destruction CHECK (destruction_method IN ('DELETE', 'ANONYMIZE', 'FILE_SECURE_DELETE')),
    CONSTRAINT ck_retention_review_fields CHECK ((review_requested_by IS NULL) = (review_requested_at IS NULL)),
    CONSTRAINT ck_retention_approval_fields CHECK ((approved_by IS NULL) = (approved_at IS NULL)),
    CONSTRAINT ck_retention_separation CHECK (approved_by IS NULL OR approved_by <> created_by),
    CONSTRAINT ck_retention_version_lock CHECK (version_no >= 0)
);

CREATE INDEX ix_retention_version_policy
    ON retention_policy_version (retention_policy_id, version_number DESC);
CREATE INDEX ix_retention_version_status
    ON retention_policy_version (workflow_status, updated_at DESC);

ALTER TABLE retention_policy
    ADD CONSTRAINT fk_retention_current_version
    FOREIGN KEY (current_approved_version_id)
    REFERENCES retention_policy_version (retention_policy_version_id);

CREATE TABLE processing_activity (
    processing_activity_id      UUID PRIMARY KEY,
    activity_code               VARCHAR(30) NOT NULL UNIQUE,
    activity_name               VARCHAR(100) NOT NULL,
    owner_organization_unit_id  UUID NOT NULL REFERENCES organization_unit (organization_unit_id),
    activity_status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_approved_version_id UUID,
    created_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_processing_activity_code CHECK (activity_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_processing_activity_name CHECK (length(btrim(activity_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_processing_activity_status CHECK (activity_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_processing_activity_version CHECK (version_no >= 0)
);

CREATE INDEX ix_processing_activity_owner
    ON processing_activity (owner_organization_unit_id, activity_status);

CREATE TABLE processing_activity_version (
    processing_activity_version_id UUID PRIMARY KEY,
    processing_activity_id         UUID NOT NULL REFERENCES processing_activity (processing_activity_id),
    version_number                 INTEGER NOT NULL,
    workflow_status                VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    processing_purpose             VARCHAR(1000) NOT NULL,
    legal_basis_type               VARCHAR(20) NOT NULL,
    legal_basis_detail             VARCHAR(1000) NOT NULL,
    retention_policy_version_id    UUID NOT NULL REFERENCES retention_policy_version (retention_policy_version_id),
    created_by                     UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_requested_by            UUID REFERENCES admin_user (admin_user_id),
    review_requested_at            TIMESTAMPTZ,
    approved_by                    UUID REFERENCES admin_user (admin_user_id),
    approved_at                    TIMESTAMPTZ,
    rejection_comment              VARCHAR(1000),
    updated_by                     UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_processing_activity_version UNIQUE (processing_activity_id, version_number),
    CONSTRAINT ck_processing_version_number CHECK (version_number >= 1),
    CONSTRAINT ck_processing_workflow CHECK (workflow_status IN ('DRAFT', 'REVIEW_REQUESTED', 'APPROVED', 'SUPERSEDED')),
    CONSTRAINT ck_processing_purpose CHECK (length(btrim(processing_purpose)) BETWEEN 10 AND 1000),
    CONSTRAINT ck_processing_basis_type CHECK (legal_basis_type IN ('CONSENT', 'LAW', 'PUBLIC_TASK', 'CONTRACT', 'OTHER')),
    CONSTRAINT ck_processing_basis_detail CHECK (length(btrim(legal_basis_detail)) BETWEEN 5 AND 1000),
    CONSTRAINT ck_processing_review_fields CHECK ((review_requested_by IS NULL) = (review_requested_at IS NULL)),
    CONSTRAINT ck_processing_approval_fields CHECK ((approved_by IS NULL) = (approved_at IS NULL)),
    CONSTRAINT ck_processing_separation CHECK (approved_by IS NULL OR approved_by <> created_by),
    CONSTRAINT ck_processing_version_lock CHECK (version_no >= 0)
);

CREATE INDEX ix_processing_version_activity
    ON processing_activity_version (processing_activity_id, version_number DESC);
CREATE INDEX ix_processing_version_status
    ON processing_activity_version (workflow_status, updated_at DESC);
CREATE INDEX ix_processing_version_retention
    ON processing_activity_version (retention_policy_version_id);

ALTER TABLE processing_activity
    ADD CONSTRAINT fk_processing_current_version
    FOREIGN KEY (current_approved_version_id)
    REFERENCES processing_activity_version (processing_activity_version_id);

CREATE TABLE processing_data_item (
    processing_data_item_id        UUID PRIMARY KEY,
    processing_activity_version_id UUID NOT NULL REFERENCES processing_activity_version (processing_activity_version_id),
    item_order                     INTEGER NOT NULL,
    item_name                      VARCHAR(100) NOT NULL,
    necessity_reason               VARCHAR(500) NOT NULL,
    required                       BOOLEAN NOT NULL DEFAULT TRUE,
    data_category                  VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    CONSTRAINT uq_processing_data_item_order UNIQUE (processing_activity_version_id, item_order),
    CONSTRAINT ck_processing_data_item_order CHECK (item_order >= 1),
    CONSTRAINT ck_processing_data_item_name CHECK (length(btrim(item_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_processing_data_item_reason CHECK (length(btrim(necessity_reason)) BETWEEN 1 AND 500),
    CONSTRAINT ck_processing_data_category CHECK (data_category = 'GENERAL')
);

CREATE INDEX ix_processing_data_item_version
    ON processing_data_item (processing_activity_version_id, item_order);

CREATE TABLE user_processing_activity_scope (
    admin_user_id          UUID NOT NULL REFERENCES admin_user (admin_user_id),
    processing_activity_id UUID NOT NULL REFERENCES processing_activity (processing_activity_id),
    assigned_by            UUID REFERENCES admin_user (admin_user_id),
    assigned_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_user_id, processing_activity_id)
);

CREATE INDEX ix_user_processing_scope_activity
    ON user_processing_activity_scope (processing_activity_id, admin_user_id);

REVOKE ALL ON
    retention_policy,
    retention_policy_version,
    processing_activity,
    processing_activity_version,
    processing_data_item,
    user_processing_activity_scope
FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE ON
    retention_policy,
    retention_policy_version,
    processing_activity,
    processing_activity_version,
    processing_data_item,
    user_processing_activity_scope
TO seal_app;

REVOKE DELETE, TRUNCATE ON
    retention_policy,
    retention_policy_version,
    processing_activity,
    processing_activity_version,
    processing_data_item,
    user_processing_activity_scope
FROM seal_app;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260821_001_create_policy_tables.sql',
    :'script_checksum',
    :'app_version',
    '보유정책, 처리업무, 일반 개인정보 항목과 처리업무 범위 테이블 생성'
);

COMMIT;
