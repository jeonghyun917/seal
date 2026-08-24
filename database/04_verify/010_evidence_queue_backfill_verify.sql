\set ON_ERROR_STOP on
\encoding UTF8
\if :{?target_database}
\else
  \set target_database seal_dev
\endif
\connect :target_database
SET search_path = seal, public;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM consent_request request
      JOIN consent_response response USING (consent_request_id)
      WHERE request.request_status='COMPLETED' AND request.completed_at IS NULL) THEN
    RAISE EXCEPTION '완료 요청의 completed_at 백필이 누락됐습니다.';
  END IF;
  IF EXISTS (
      SELECT 1 FROM consent_response response
      WHERE NOT EXISTS (
        SELECT 1 FROM evidence_package package
        WHERE package.consent_response_id=response.consent_response_id
          AND package.generation_status<>'DESTROYED')) THEN
    RAISE EXCEPTION '완료 응답의 활성 증적 대기열이 누락됐습니다.';
  END IF;
END $$;

\echo 'Evidence queue backfill verification passed'
