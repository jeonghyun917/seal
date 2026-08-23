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

ALTER TABLE processing_activity_version
    ADD CONSTRAINT uq_processing_version_activity_pair
    UNIQUE (processing_activity_version_id, processing_activity_id);

ALTER TABLE processing_data_item
    ADD CONSTRAINT uq_processing_data_item_version_pair
    UNIQUE (processing_data_item_id, processing_activity_version_id);

CREATE TABLE consent_template (
    consent_template_id         UUID PRIMARY KEY,
    template_code               VARCHAR(30) NOT NULL UNIQUE,
    template_name               VARCHAR(100) NOT NULL,
    processing_activity_id      UUID NOT NULL REFERENCES processing_activity (processing_activity_id),
    template_status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_published_version_id UUID,
    created_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_template_activity_pair UNIQUE (consent_template_id, processing_activity_id),
    CONSTRAINT ck_template_code CHECK (template_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_template_name CHECK (length(btrim(template_name)) BETWEEN 1 AND 100),
    CONSTRAINT ck_template_status CHECK (template_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_template_version_no CHECK (version_no >= 0)
);

CREATE INDEX ix_consent_template_activity
    ON consent_template (processing_activity_id, template_status);

CREATE TABLE consent_template_version (
    consent_template_version_id UUID PRIMARY KEY,
    consent_template_id         UUID NOT NULL,
    processing_activity_id      UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    version_number              INTEGER NOT NULL,
    workflow_status             VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    title                       VARCHAR(200) NOT NULL,
    introduction_html           VARCHAR(8000) NOT NULL,
    review_requested_by         UUID REFERENCES admin_user (admin_user_id),
    review_requested_at         TIMESTAMPTZ,
    approved_by                 UUID REFERENCES admin_user (admin_user_id),
    approved_at                 TIMESTAMPTZ,
    published_by                UUID REFERENCES admin_user (admin_user_id),
    published_at                TIMESTAMPTZ,
    rejection_comment           VARCHAR(1000),
    content_hash                CHAR(64),
    created_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  UUID NOT NULL REFERENCES admin_user (admin_user_id),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_no                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_template_version_number UNIQUE (consent_template_id, version_number),
    CONSTRAINT uq_template_version_activity_pair UNIQUE
        (consent_template_version_id, processing_activity_version_id),
    CONSTRAINT uq_template_version_template_pair UNIQUE
        (consent_template_version_id, consent_template_id),
    CONSTRAINT fk_template_version_template_activity FOREIGN KEY
        (consent_template_id, processing_activity_id)
        REFERENCES consent_template (consent_template_id, processing_activity_id),
    CONSTRAINT fk_template_version_processing_activity FOREIGN KEY
        (processing_activity_version_id, processing_activity_id)
        REFERENCES processing_activity_version
            (processing_activity_version_id, processing_activity_id),
    CONSTRAINT ck_template_version_number CHECK (version_number >= 1),
    CONSTRAINT ck_template_workflow CHECK
        (workflow_status IN ('DRAFT', 'REVIEW_REQUESTED', 'APPROVED', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT ck_template_title CHECK (length(btrim(title)) BETWEEN 5 AND 200),
    CONSTRAINT ck_template_intro CHECK (length(introduction_html) BETWEEN 10 AND 8000),
    CONSTRAINT ck_template_review_pair CHECK
        ((review_requested_by IS NULL) = (review_requested_at IS NULL)),
    CONSTRAINT ck_template_approval_pair CHECK
        ((approved_by IS NULL) = (approved_at IS NULL)),
    CONSTRAINT ck_template_publish_pair CHECK
        ((published_by IS NULL) = (published_at IS NULL)),
    CONSTRAINT ck_template_author_approver CHECK
        (approved_by IS NULL OR approved_by <> created_by),
    CONSTRAINT ck_template_hash CHECK
        (content_hash IS NULL OR content_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_template_published_hash CHECK
        (workflow_status NOT IN ('PUBLISHED', 'RETIRED') OR content_hash IS NOT NULL),
    CONSTRAINT ck_template_version_lock CHECK (version_no >= 0)
);

CREATE INDEX ix_template_version_template
    ON consent_template_version (consent_template_id, version_number DESC);
CREATE INDEX ix_template_version_status
    ON consent_template_version (workflow_status, updated_at DESC);
CREATE INDEX ix_template_version_processing
    ON consent_template_version (processing_activity_version_id);

ALTER TABLE consent_template
    ADD CONSTRAINT fk_template_current_published_version
    FOREIGN KEY (current_published_version_id, consent_template_id)
    REFERENCES consent_template_version
        (consent_template_version_id, consent_template_id);

CREATE TABLE consent_item_definition (
    consent_item_definition_id UUID PRIMARY KEY,
    consent_template_version_id UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    item_order                  INTEGER NOT NULL,
    item_code                   VARCHAR(30) NOT NULL,
    item_type                   VARCHAR(30) NOT NULL,
    item_title                  VARCHAR(200) NOT NULL,
    required                    BOOLEAN NOT NULL,
    purpose_text                VARCHAR(1000) NOT NULL,
    retention_text              VARCHAR(1000) NOT NULL,
    refusal_notice              VARCHAR(1000) NOT NULL,
    third_party_recipient       VARCHAR(500),
    CONSTRAINT uq_template_item_order UNIQUE (consent_template_version_id, item_order),
    CONSTRAINT uq_template_item_code UNIQUE (consent_template_version_id, item_code),
    CONSTRAINT uq_template_item_version_pair UNIQUE
        (consent_item_definition_id, processing_activity_version_id),
    CONSTRAINT fk_template_item_version FOREIGN KEY
        (consent_template_version_id, processing_activity_version_id)
        REFERENCES consent_template_version
            (consent_template_version_id, processing_activity_version_id),
    CONSTRAINT ck_template_item_order CHECK (item_order >= 1),
    CONSTRAINT ck_template_item_code CHECK (item_code ~ '^[A-Z0-9-]{2,30}$'),
    CONSTRAINT ck_template_item_type CHECK
        (item_type IN ('COLLECTION_USE', 'THIRD_PARTY_PROVISION', 'OPTIONAL')),
    CONSTRAINT ck_template_item_title CHECK (length(btrim(item_title)) BETWEEN 5 AND 200),
    CONSTRAINT ck_template_item_purpose CHECK (length(btrim(purpose_text)) BETWEEN 5 AND 1000),
    CONSTRAINT ck_template_item_retention CHECK (length(btrim(retention_text)) BETWEEN 5 AND 1000),
    CONSTRAINT ck_template_item_refusal CHECK (length(btrim(refusal_notice)) BETWEEN 10 AND 1000),
    CONSTRAINT ck_template_optional_required CHECK (item_type <> 'OPTIONAL' OR required = FALSE),
    CONSTRAINT ck_template_third_party CHECK
        ((item_type = 'THIRD_PARTY_PROVISION') =
         (third_party_recipient IS NOT NULL AND length(btrim(third_party_recipient)) > 0))
);

CREATE INDEX ix_template_item_version
    ON consent_item_definition (consent_template_version_id, item_order);

CREATE TABLE consent_item_data_selection (
    consent_item_definition_id UUID NOT NULL,
    processing_activity_version_id UUID NOT NULL,
    processing_data_item_id    UUID NOT NULL,
    PRIMARY KEY (consent_item_definition_id, processing_data_item_id),
    CONSTRAINT fk_selection_item_version FOREIGN KEY
        (consent_item_definition_id, processing_activity_version_id)
        REFERENCES consent_item_definition
            (consent_item_definition_id, processing_activity_version_id),
    CONSTRAINT fk_selection_data_version FOREIGN KEY
        (processing_data_item_id, processing_activity_version_id)
        REFERENCES processing_data_item
            (processing_data_item_id, processing_activity_version_id)
);

CREATE INDEX ix_template_selection_data_item
    ON consent_item_data_selection (processing_data_item_id, consent_item_definition_id);

CREATE TABLE template_review (
    template_review_id          UUID PRIMARY KEY,
    consent_template_version_id UUID NOT NULL
        REFERENCES consent_template_version (consent_template_version_id),
    review_action               VARCHAR(20) NOT NULL,
    actor_id                    UUID NOT NULL REFERENCES admin_user (admin_user_id),
    comment                     VARCHAR(1000),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_template_review_action CHECK
        (review_action IN ('REQUESTED', 'APPROVED', 'REJECTED', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT ck_template_review_comment CHECK
        (comment IS NULL OR length(btrim(comment)) BETWEEN 1 AND 1000)
);

CREATE INDEX ix_template_review_version
    ON template_review (consent_template_version_id, created_at DESC);

INSERT INTO permission (permission_id, permission_code, permission_name) VALUES
    ('00000000-0000-0000-0002-000000000018', 'CONSENT_TEMPLATE_VIEW', '동의서 템플릿 조회'),
    ('00000000-0000-0000-0002-000000000019', 'CONSENT_TEMPLATE_EDIT', '동의서 템플릿 편집'),
    ('00000000-0000-0000-0002-000000000020', 'CONSENT_TEMPLATE_REVIEW', '동의서 템플릿 검토 요청'),
    ('00000000-0000-0000-0002-000000000021', 'CONSENT_TEMPLATE_APPROVE', '동의서 템플릿 승인'),
    ('00000000-0000-0000-0002-000000000022', 'CONSENT_TEMPLATE_PUBLISH', '동의서 템플릿 발행');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
  FROM admin_role r
 CROSS JOIN permission p
 WHERE r.role_code = 'SYSTEM_ADMIN'
   AND p.permission_code LIKE 'CONSENT_TEMPLATE_%';

REVOKE ALL ON
    consent_template,
    consent_template_version,
    consent_item_definition,
    consent_item_data_selection,
    template_review
FROM PUBLIC;

GRANT SELECT, INSERT, UPDATE ON
    consent_template,
    consent_template_version,
    consent_item_definition,
    consent_item_data_selection
TO seal_app;

GRANT SELECT, INSERT ON template_review TO seal_app;

REVOKE DELETE, TRUNCATE ON
    consent_template,
    consent_template_version,
    consent_item_definition,
    consent_item_data_selection,
    template_review
FROM seal_app;

REVOKE UPDATE ON template_review FROM seal_app;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '03_patch/20260824_001_create_consent_template_tables.sql',
    :'script_checksum',
    :'app_version',
    '구조화 동의서 템플릿, 버전, 결정 항목, 검토 이력과 템플릿 권한 생성'
);

COMMIT;
