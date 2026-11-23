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

-- A queued or failed package may be destroyed without ever having been finalized.
-- Keep the original finalization timestamp for packages that were finalized.
ALTER TABLE evidence_package DROP CONSTRAINT ck_evidence_finalized;
ALTER TABLE evidence_package ADD CONSTRAINT ck_evidence_finalized CHECK (
    generation_status = 'DESTROYED'
    OR ((generation_status = 'FINALIZED') = (finalized_at IS NOT NULL))
);

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260929_016_allow_unfinalized_evidence_destruction.sql',:'script_checksum',:'app_version',
 '생성 대기·진행·실패 증적을 허위 완료시각 없이 파기 상태로 전환');
COMMIT;
