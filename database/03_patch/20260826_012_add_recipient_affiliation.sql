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
  ADD COLUMN affiliation_name VARCHAR(200),
  ADD CONSTRAINT ck_recipient_affiliation_name CHECK (
    affiliation_name IS NULL OR length(btrim(affiliation_name)) BETWEEN 1 AND 200
  );

ALTER TABLE recipient_import_row
  ADD COLUMN affiliation_name VARCHAR(200),
  ADD CONSTRAINT ck_recipient_import_affiliation_name CHECK (
    affiliation_name IS NULL OR length(btrim(affiliation_name)) BETWEEN 1 AND 200
  );

COMMENT ON COLUMN recipient.affiliation_name IS '동의 수신자가 실제 소속된 외부 회사·기관명(선택)';
COMMENT ON COLUMN recipient_import_row.affiliation_name IS 'Excel 반입 수신자의 외부 회사·기관명 임시값';

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260826_012_add_recipient_affiliation.sql',:'script_checksum',:'app_version',
 '내부 관리 부서와 분리된 동의 수신자 외부 소속기관 필드 추가');

COMMIT;