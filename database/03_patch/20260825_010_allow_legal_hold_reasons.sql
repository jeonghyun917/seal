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

DROP INDEX uq_active_legal_hold;
CREATE UNIQUE INDEX uq_active_legal_hold_reason
  ON legal_hold(consent_request_id, hold_reason_code)
  WHERE active;

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260825_010_allow_legal_hold_reasons.sql',:'script_checksum',:'app_version',
 '동의 요청별 서로 다른 활성 보존 보류 사유의 공존 허용');

COMMIT;
