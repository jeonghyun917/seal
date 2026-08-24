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

ALTER TABLE public_consent_session
  DROP CONSTRAINT ck_public_session_hmac,
  ALTER COLUMN flow_hmac DROP NOT NULL,
  ADD COLUMN flow_destroyed_at TIMESTAMPTZ,
  ADD CONSTRAINT ck_public_session_hmac CHECK (
    (flow_hmac IS NOT NULL AND flow_hmac ~ '^[0-9a-f]{64}$' AND flow_destroyed_at IS NULL)
    OR
    (flow_hmac IS NULL AND session_status IN ('COMPLETED','EXPIRED') AND flow_destroyed_at IS NOT NULL)
  );

ALTER TABLE rights_request
  DROP CONSTRAINT ck_rights_detail,
  ALTER COLUMN detail_ciphertext DROP NOT NULL,
  ALTER COLUMN encryption_key_id DROP NOT NULL,
  ADD COLUMN personal_data_destroyed_at TIMESTAMPTZ,
  ADD CONSTRAINT ck_rights_personal_data_lifecycle CHECK (
    (personal_data_destroyed_at IS NULL AND detail_ciphertext IS NOT NULL
      AND octet_length(detail_ciphertext)>=30 AND encryption_key_id IS NOT NULL)
    OR
    (personal_data_destroyed_at IS NOT NULL AND detail_ciphertext IS NULL
      AND verification_note_ciphertext IS NULL AND result_note_ciphertext IS NULL
      AND encryption_key_id IS NULL)
  );

ALTER TABLE retention_job
  DROP CONSTRAINT ck_retention_job_counts,
  ADD COLUMN destroyed_session_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN destroyed_rights_count INTEGER NOT NULL DEFAULT 0,
  ADD CONSTRAINT ck_retention_job_counts CHECK (
    destroyed_mail_count>=0 AND destroyed_file_count>=0
    AND destroyed_session_count>=0 AND destroyed_rights_count>=0
  );

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260824_007_add_residual_destruction.sql',:'script_checksum',:'app_version',
 '완료 공개 세션 흐름값과 권리요청 암호화 메모의 파기 수명주기 및 결과 건수 추가');

COMMIT;
