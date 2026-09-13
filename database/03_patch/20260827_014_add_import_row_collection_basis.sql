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

ALTER TABLE recipient_import_row
  ADD COLUMN collection_basis VARCHAR(500),
  ADD CONSTRAINT ck_recipient_import_collection_basis CHECK (
    collection_basis IS NULL OR length(btrim(collection_basis)) BETWEEN 5 AND 500
  );

COMMENT ON COLUMN recipient_import_row.collection_basis IS 'Excel 각 수신자 행의 정보 등록 근거 임시값';

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260827_014_add_import_row_collection_basis.sql',:'script_checksum',:'app_version',
 'Excel 수신자 반입 행별 정보 등록 근거 필드 추가');

COMMIT;