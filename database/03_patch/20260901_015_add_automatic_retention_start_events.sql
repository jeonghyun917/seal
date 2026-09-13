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

ALTER TABLE retention_policy_version
    DROP CONSTRAINT ck_retention_start_event;

ALTER TABLE retention_policy_version
    ADD CONSTRAINT ck_retention_start_event CHECK (start_event IN (
        'MAIL_FIRST_SENT_AT',
        'REQUEST_EXPIRES_AT',
        'CONSENT_SUBMITTED_AT',
        'REQUEST_COMPLETED_AT',
        'PROGRAM_END_AT',
        'PURPOSE_ACHIEVED_AT'
    ));

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260901_015_add_automatic_retention_start_events.sql',:'script_checksum',:'app_version',
 '메일 최초 발송일과 동의 요청 만료일을 보유기간 자동 시작 기준으로 추가');

COMMIT;
