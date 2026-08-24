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

UPDATE consent_request request
   SET completed_at = response.submitted_at
  FROM consent_response response
 WHERE response.consent_request_id = request.consent_request_id
   AND request.request_status = 'COMPLETED'
   AND request.completed_at IS NULL;

INSERT INTO evidence_package (evidence_package_id, consent_response_id, consent_request_id,
        organization_unit_id, generation_status, queued_at)
SELECT gen_random_uuid(), response.consent_response_id, response.consent_request_id,
       request.organization_unit_id, 'QUEUED', response.submitted_at
  FROM consent_response response
  JOIN consent_request request USING (consent_request_id)
 WHERE NOT EXISTS (
       SELECT 1 FROM evidence_package package
        WHERE package.consent_response_id = response.consent_response_id
          AND package.generation_status <> 'DESTROYED');

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260824_006_backfill_evidence_queue.sql',:'script_checksum',:'app_version',
 '기존 완료 동의의 완료시각과 증적 생성 대기열 백필');

COMMIT;
