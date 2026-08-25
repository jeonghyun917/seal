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

ALTER TABLE consent_request
  ADD CONSTRAINT uq_request_recipient_pair
    UNIQUE (consent_request_id, recipient_id),
  ADD CONSTRAINT uq_request_recipient_organization
    UNIQUE (consent_request_id, recipient_id, organization_unit_id);

ALTER TABLE rights_request
  ADD CONSTRAINT uq_rights_request_context
    UNIQUE (rights_request_id, consent_request_id, recipient_id),
  ADD CONSTRAINT fk_rights_recipient_organization
    FOREIGN KEY (recipient_id, organization_unit_id)
    REFERENCES recipient (recipient_id, organization_unit_id),
  ADD CONSTRAINT fk_rights_request_recipient_organization
    FOREIGN KEY (consent_request_id, recipient_id, organization_unit_id)
    REFERENCES consent_request (consent_request_id, recipient_id, organization_unit_id),
  ADD CONSTRAINT ck_rights_source_request_required CHECK (
    rights_type NOT IN ('ERASURE', 'RESTRICTION', 'WITHDRAWAL')
    OR consent_request_id IS NOT NULL
  );

ALTER TABLE retention_job
  ADD CONSTRAINT fk_retention_request_recipient
    FOREIGN KEY (consent_request_id, recipient_id)
    REFERENCES consent_request (consent_request_id, recipient_id),
  ADD CONSTRAINT fk_retention_source_rights_context
    FOREIGN KEY (source_rights_request_id, consent_request_id, recipient_id)
    REFERENCES rights_request (rights_request_id, consent_request_id, recipient_id);

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260825_008_harden_rights_scope_integrity.sql',:'script_checksum',:'app_version',
 '권리요청 대상자·조직·동의요청과 파기 원본 권리요청의 복합 참조 무결성 강화');

COMMIT;
