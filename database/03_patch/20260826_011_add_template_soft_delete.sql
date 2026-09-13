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

ALTER TABLE consent_template
  ADD COLUMN deleted_at TIMESTAMPTZ,
  ADD COLUMN deleted_by UUID,
  ADD COLUMN delete_reason VARCHAR(500),
  ADD CONSTRAINT fk_consent_template_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES admin_user(admin_user_id),
  ADD CONSTRAINT ck_consent_template_delete_metadata CHECK (
    (deleted_at IS NULL AND deleted_by IS NULL AND delete_reason IS NULL)
    OR
    (deleted_at IS NOT NULL AND deleted_by IS NOT NULL
      AND delete_reason IS NOT NULL AND btrim(delete_reason) <> '')
  );

CREATE INDEX ix_consent_template_deleted_at
  ON consent_template(deleted_at DESC)
  WHERE deleted_at IS NOT NULL;

COMMENT ON COLUMN consent_template.deleted_at IS '관리자 소프트 삭제 일시';
COMMENT ON COLUMN consent_template.deleted_by IS '소프트 삭제 수행 관리자';
COMMENT ON COLUMN consent_template.delete_reason IS '소프트 삭제 사유';

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260826_011_add_template_soft_delete.sql',:'script_checksum',:'app_version',
 '동의서 템플릿 소프트 삭제 메타데이터와 삭제 목록 조회 인덱스 추가');

COMMIT;