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

ALTER TABLE recipient
  ADD COLUMN job_title VARCHAR(100),
  ADD CONSTRAINT ck_recipient_job_title CHECK (
    job_title IS NULL OR length(btrim(job_title)) BETWEEN 1 AND 100
  );

ALTER TABLE recipient_import_row
  ADD COLUMN job_title VARCHAR(100),
  ADD CONSTRAINT ck_recipient_import_job_title CHECK (
    job_title IS NULL OR length(btrim(job_title)) BETWEEN 1 AND 100
  );

COMMENT ON COLUMN recipient.job_title IS '동의 수신자의 소속기관 직책 또는 직위(선택)';
COMMENT ON COLUMN recipient_import_row.job_title IS 'Excel 반입 수신자의 직책 임시값';

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260827_013_add_recipient_job_title.sql',:'script_checksum',:'app_version',
 '동의 수신자 직책과 Excel 반입 임시 직책 필드 추가');

COMMIT;