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

ALTER TABLE consent_request ADD COLUMN completed_at TIMESTAMPTZ;

DROP INDEX ux_recipient_active_email;
ALTER TABLE recipient ALTER COLUMN recipient_name_ciphertext DROP NOT NULL;
ALTER TABLE recipient ALTER COLUMN recipient_email_ciphertext DROP NOT NULL;
ALTER TABLE recipient ALTER COLUMN encryption_key_id DROP NOT NULL;
ALTER TABLE recipient ALTER COLUMN email_lookup_hmac DROP NOT NULL;
ALTER TABLE recipient DROP CONSTRAINT ck_recipient_name_cipher;
ALTER TABLE recipient DROP CONSTRAINT ck_recipient_email_cipher;
ALTER TABLE recipient DROP CONSTRAINT ck_recipient_key_id;
ALTER TABLE recipient DROP CONSTRAINT ck_recipient_email_hmac;
ALTER TABLE recipient DROP CONSTRAINT ck_recipient_status;
ALTER TABLE recipient ADD CONSTRAINT ck_recipient_status
  CHECK (recipient_status IN ('ACTIVE','INACTIVE','DESTROYED'));
ALTER TABLE recipient ADD CONSTRAINT ck_recipient_personal_data
  CHECK ((recipient_status='DESTROYED' AND recipient_name_ciphertext IS NULL
          AND recipient_email_ciphertext IS NULL AND encryption_key_id IS NULL
          AND email_lookup_hmac IS NULL)
      OR (recipient_status<>'DESTROYED' AND octet_length(recipient_name_ciphertext)>=30
          AND octet_length(recipient_email_ciphertext)>=30
          AND encryption_key_id ~ '^[A-Za-z0-9._-]{1,30}$'
          AND email_lookup_hmac ~ '^[0-9a-f]{64}$'));
CREATE UNIQUE INDEX ux_recipient_active_email ON recipient(organization_unit_id,email_lookup_hmac)
  WHERE recipient_status='ACTIVE' AND email_lookup_hmac IS NOT NULL;

ALTER TABLE mail_outbox ALTER COLUMN recipient_email_ciphertext DROP NOT NULL;
ALTER TABLE mail_outbox ALTER COLUMN html_ciphertext DROP NOT NULL;
ALTER TABLE mail_outbox ALTER COLUMN encryption_key_id DROP NOT NULL;
ALTER TABLE mail_outbox DROP CONSTRAINT ck_mail_outbox_email;
ALTER TABLE mail_outbox DROP CONSTRAINT ck_mail_outbox_html;
ALTER TABLE mail_outbox DROP CONSTRAINT ck_mail_outbox_status;
ALTER TABLE mail_outbox ADD CONSTRAINT ck_mail_outbox_status CHECK (mail_status IN
  ('QUEUED','PROCESSING','SMTP_ACCEPTED','RETRY_SCHEDULED','SEND_FAILED','CANCELLED','DESTROYED'));
ALTER TABLE mail_outbox ADD CONSTRAINT ck_mail_outbox_content CHECK (
  (mail_status='DESTROYED' AND recipient_email_ciphertext IS NULL AND html_ciphertext IS NULL AND encryption_key_id IS NULL)
  OR (mail_status<>'DESTROYED' AND octet_length(recipient_email_ciphertext)>=30
      AND octet_length(html_ciphertext)>=30 AND encryption_key_id IS NOT NULL));

CREATE TABLE evidence_package (
  evidence_package_id UUID PRIMARY KEY,
  consent_response_id UUID NOT NULL REFERENCES consent_response(consent_response_id),
  consent_request_id UUID NOT NULL REFERENCES consent_request(consent_request_id),
  organization_unit_id UUID NOT NULL REFERENCES organization_unit(organization_unit_id),
  generation_status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  relative_path VARCHAR(500), manifest_sha256 CHAR(64), manifest_hmac VARCHAR(100),
  generator_version VARCHAR(50), font_version VARCHAR(100),
  queued_at TIMESTAMPTZ NOT NULL, locked_at TIMESTAMPTZ, finalized_at TIMESTAMPTZ,
  failed_at TIMESTAMPTZ, destroyed_at TIMESTAMPTZ, last_error_code VARCHAR(100),
  attempt_count INTEGER NOT NULL DEFAULT 0, row_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_evidence_status CHECK (generation_status IN ('QUEUED','PROCESSING','FINALIZED','FAILED','DESTROYED')),
  CONSTRAINT ck_evidence_attempt CHECK (attempt_count BETWEEN 0 AND 10),
  CONSTRAINT ck_evidence_manifest CHECK (manifest_sha256 IS NULL OR manifest_sha256 ~ '^[0-9a-f]{64}$'),
  CONSTRAINT ck_evidence_finalized CHECK ((generation_status IN ('FINALIZED','DESTROYED'))=(finalized_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_active_evidence_response ON evidence_package(consent_response_id)
  WHERE generation_status<>'DESTROYED';
CREATE INDEX ix_evidence_due ON evidence_package(generation_status,queued_at);

CREATE TABLE evidence_file (
  evidence_file_id UUID PRIMARY KEY,
  evidence_package_id UUID NOT NULL REFERENCES evidence_package(evidence_package_id),
  file_type VARCHAR(30) NOT NULL, file_name VARCHAR(100) NOT NULL,
  relative_path VARCHAR(600) NOT NULL, file_size BIGINT NOT NULL, sha256 CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_evidence_file_type UNIQUE(evidence_package_id,file_type),
  CONSTRAINT ck_evidence_file_type CHECK (file_type IN ('PDF','REQUEST_JSON','DECISIONS_JSON','EVENTS_JSON','MANIFEST_JSON','MANIFEST_HMAC')),
  CONSTRAINT ck_evidence_file_size CHECK (file_size>=0),
  CONSTRAINT ck_evidence_file_hash CHECK (sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TABLE evidence_verification (
  evidence_verification_id UUID PRIMARY KEY,
  evidence_package_id UUID NOT NULL REFERENCES evidence_package(evidence_package_id),
  verification_result VARCHAR(30) NOT NULL, reason_code VARCHAR(100),
  verified_by UUID NOT NULL REFERENCES admin_user(admin_user_id), verified_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_evidence_verify_result CHECK (verification_result IN ('VALID','INVALID_HMAC','INVALID_HASH','MISSING_FILE'))
);

CREATE TABLE rights_request (
  rights_request_id UUID PRIMARY KEY,
  request_code VARCHAR(25) NOT NULL UNIQUE,
  organization_unit_id UUID NOT NULL REFERENCES organization_unit(organization_unit_id),
  recipient_id UUID NOT NULL REFERENCES recipient(recipient_id),
  consent_request_id UUID REFERENCES consent_request(consent_request_id),
  rights_type VARCHAR(20) NOT NULL, request_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
  received_date DATE NOT NULL, due_date DATE NOT NULL,
  detail_ciphertext BYTEA NOT NULL, verification_note_ciphertext BYTEA,
  result_note_ciphertext BYTEA, encryption_key_id VARCHAR(30) NOT NULL,
  verification_channel VARCHAR(30), identity_verified_by UUID REFERENCES admin_user(admin_user_id),
  identity_verified_at TIMESTAMPTZ, decision_code VARCHAR(100),
  retention_job_id UUID, result_target_id UUID,
  created_by UUID NOT NULL REFERENCES admin_user(admin_user_id), created_at TIMESTAMPTZ NOT NULL,
  decided_by UUID REFERENCES admin_user(admin_user_id), decided_at TIMESTAMPTZ,
  completed_by UUID REFERENCES admin_user(admin_user_id), completed_at TIMESTAMPTZ,
  cancelled_by UUID REFERENCES admin_user(admin_user_id), cancelled_at TIMESTAMPTZ,
  row_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_rights_type CHECK (rights_type IN ('ACCESS','CORRECTION','ERASURE','RESTRICTION','WITHDRAWAL')),
  CONSTRAINT ck_rights_status CHECK (request_status IN ('RECEIVED','IDENTITY_VERIFIED','IN_REVIEW','APPROVED','REJECTED','COMPLETED','CANCELLED')),
  CONSTRAINT ck_rights_due CHECK (due_date>=received_date),
  CONSTRAINT ck_rights_detail CHECK (octet_length(detail_ciphertext)>=30),
  CONSTRAINT ck_rights_identity CHECK ((identity_verified_by IS NULL)=(identity_verified_at IS NULL)),
  CONSTRAINT ck_rights_decision_separation CHECK (decided_by IS NULL OR decided_by<>created_by)
);
CREATE INDEX ix_rights_scope ON rights_request(organization_unit_id,request_status,due_date);

CREATE TABLE rights_request_event (
  rights_request_event_id UUID PRIMARY KEY,
  rights_request_id UUID NOT NULL REFERENCES rights_request(rights_request_id),
  event_type VARCHAR(40) NOT NULL, from_status VARCHAR(30), to_status VARCHAR(30) NOT NULL,
  actor_id UUID NOT NULL REFERENCES admin_user(admin_user_id), reason_code VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE consent_withdrawal (
  consent_withdrawal_id UUID PRIMARY KEY,
  consent_response_id UUID NOT NULL REFERENCES consent_response(consent_response_id),
  rights_request_id UUID NOT NULL UNIQUE REFERENCES rights_request(rights_request_id),
  withdrawal_scope VARCHAR(20) NOT NULL DEFAULT 'ALL', reason_code VARCHAR(100),
  withdrawn_by UUID NOT NULL REFERENCES admin_user(admin_user_id), withdrawn_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_withdrawal_scope CHECK (withdrawal_scope='ALL')
);

CREATE TABLE retention_anchor_event (
  retention_anchor_event_id UUID PRIMARY KEY,
  consent_request_id UUID NOT NULL REFERENCES consent_request(consent_request_id),
  anchor_type VARCHAR(40) NOT NULL, anchor_date DATE NOT NULL,
  supersedes_event_id UUID REFERENCES retention_anchor_event(retention_anchor_event_id),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  recorded_by UUID NOT NULL REFERENCES admin_user(admin_user_id), recorded_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_anchor_type CHECK (anchor_type IN ('PROGRAM_END_AT','PURPOSE_ACHIEVED_AT'))
);
CREATE UNIQUE INDEX uq_active_retention_anchor ON retention_anchor_event(consent_request_id,anchor_type) WHERE active;

CREATE TABLE legal_hold (
  legal_hold_id UUID PRIMARY KEY,
  consent_request_id UUID NOT NULL REFERENCES consent_request(consent_request_id),
  hold_reason_code VARCHAR(100) NOT NULL, reason_text VARCHAR(1000) NOT NULL,
  expires_on DATE, active BOOLEAN NOT NULL DEFAULT TRUE,
  held_by UUID NOT NULL REFERENCES admin_user(admin_user_id), held_at TIMESTAMPTZ NOT NULL,
  released_by UUID REFERENCES admin_user(admin_user_id), released_at TIMESTAMPTZ,
  CONSTRAINT ck_hold_release CHECK ((released_by IS NULL)=(released_at IS NULL))
);
CREATE UNIQUE INDEX uq_active_legal_hold ON legal_hold(consent_request_id) WHERE active;

CREATE TABLE retention_job (
  retention_job_id UUID PRIMARY KEY,
  consent_request_id UUID NOT NULL REFERENCES consent_request(consent_request_id),
  recipient_id UUID NOT NULL REFERENCES recipient(recipient_id),
  retention_policy_version_id UUID NOT NULL REFERENCES retention_policy_version(retention_policy_version_id),
  source_rights_request_id UUID REFERENCES rights_request(rights_request_id),
  anchor_date DATE, due_date DATE, job_status VARCHAR(30) NOT NULL,
  destruction_method VARCHAR(30) NOT NULL, idempotency_key UUID,
  requested_by UUID REFERENCES admin_user(admin_user_id), requested_at TIMESTAMPTZ NOT NULL,
  approved_by UUID REFERENCES admin_user(admin_user_id), approved_at TIMESTAMPTZ,
  executed_by UUID REFERENCES admin_user(admin_user_id), executed_at TIMESTAMPTZ,
  confirmed_by UUID REFERENCES admin_user(admin_user_id), confirmed_at TIMESTAMPTZ,
  held_at TIMESTAMPTZ, last_error_code VARCHAR(100),
  destroyed_mail_count INTEGER NOT NULL DEFAULT 0, destroyed_file_count INTEGER NOT NULL DEFAULT 0,
  recipient_pii_destroyed BOOLEAN NOT NULL DEFAULT FALSE, row_version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_retention_job_status CHECK (job_status IN ('DUE','PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','CONFIRMED','FAILED','HELD','CANCELLED')),
  CONSTRAINT ck_retention_job_method CHECK (destruction_method IN ('DELETE','ANONYMIZE','FILE_SECURE_DELETE')),
  CONSTRAINT ck_retention_job_counts CHECK (destroyed_mail_count>=0 AND destroyed_file_count>=0),
  CONSTRAINT ck_retention_approval_separation CHECK (approved_by IS NULL OR requested_by IS NULL OR approved_by<>requested_by),
  CONSTRAINT ck_retention_confirmation_separation CHECK (confirmed_by IS NULL OR executed_by IS NULL OR confirmed_by<>executed_by)
);
CREATE UNIQUE INDEX uq_active_retention_job ON retention_job(consent_request_id)
  WHERE job_status NOT IN ('CONFIRMED','CANCELLED');
CREATE UNIQUE INDEX uq_retention_execution_key ON retention_job(idempotency_key) WHERE idempotency_key IS NOT NULL;
ALTER TABLE rights_request ADD CONSTRAINT fk_rights_retention_job FOREIGN KEY(retention_job_id) REFERENCES retention_job(retention_job_id);

CREATE TABLE retention_job_event (
  retention_job_event_id UUID PRIMARY KEY,
  retention_job_id UUID NOT NULL REFERENCES retention_job(retention_job_id),
  event_type VARCHAR(40) NOT NULL, from_status VARCHAR(30), to_status VARCHAR(30) NOT NULL,
  actor_type VARCHAR(20) NOT NULL, actor_id UUID REFERENCES admin_user(admin_user_id),
  reason_code VARCHAR(100), created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_retention_event_actor CHECK ((actor_type='ADMIN' AND actor_id IS NOT NULL) OR (actor_type='SYSTEM' AND actor_id IS NULL))
);

INSERT INTO permission(permission_id,permission_code,permission_name) VALUES
 ('00000000-0000-0000-0002-000000000034','EVIDENCE_VIEW','증적 조회'),
 ('00000000-0000-0000-0002-000000000035','EVIDENCE_GENERATE','증적 생성 재시도'),
 ('00000000-0000-0000-0002-000000000036','EVIDENCE_DOWNLOAD','증적 다운로드'),
 ('00000000-0000-0000-0002-000000000037','EVIDENCE_VERIFY','증적 무결성 검증'),
 ('00000000-0000-0000-0002-000000000038','RIGHTS_VIEW','권리요청 조회'),
 ('00000000-0000-0000-0002-000000000039','RIGHTS_EDIT','권리요청 처리'),
 ('00000000-0000-0000-0002-000000000040','RIGHTS_APPROVE','권리요청 승인'),
 ('00000000-0000-0000-0002-000000000041','RETENTION_VIEW','파기 작업 조회'),
 ('00000000-0000-0000-0002-000000000042','RETENTION_MANAGE','보존·파기 작업 관리'),
 ('00000000-0000-0000-0002-000000000043','RETENTION_APPROVE','파기 승인'),
 ('00000000-0000-0000-0002-000000000044','RETENTION_EXECUTE','파기 실행'),
 ('00000000-0000-0000-0002-000000000045','RETENTION_CONFIRM','파기 확인'),
 ('00000000-0000-0000-0002-000000000046','STATISTICS_VIEW','운영 통계 조회');

REVOKE ALL ON evidence_package,evidence_file,evidence_verification,rights_request,rights_request_event,
 consent_withdrawal,retention_anchor_event,legal_hold,retention_job,retention_job_event FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON evidence_package,rights_request,retention_anchor_event,legal_hold,retention_job TO seal_app;
GRANT SELECT,INSERT ON evidence_file,evidence_verification,rights_request_event,consent_withdrawal,retention_job_event TO seal_app;
REVOKE DELETE,TRUNCATE ON evidence_package,evidence_file,evidence_verification,rights_request,rights_request_event,
 consent_withdrawal,retention_anchor_event,legal_hold,retention_job,retention_job_event FROM seal_app;
REVOKE UPDATE ON evidence_file,evidence_verification,rights_request_event,consent_withdrawal,retention_job_event FROM seal_app;

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260824_005_create_evidence_rights_retention_tables.sql',:'script_checksum',:'app_version',
 '증적, 권리요청, 철회, 보존 보류와 파기 수명주기 기반 생성');

COMMIT;
