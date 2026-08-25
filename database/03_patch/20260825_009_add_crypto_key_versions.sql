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

ALTER TABLE rights_request
  ADD COLUMN verification_note_key_id VARCHAR(30),
  ADD COLUMN result_note_key_id VARCHAR(30);

UPDATE rights_request
SET verification_note_key_id=encryption_key_id
WHERE verification_note_ciphertext IS NOT NULL;

UPDATE rights_request
SET result_note_key_id=encryption_key_id
WHERE result_note_ciphertext IS NOT NULL;

ALTER TABLE rights_request
  DROP CONSTRAINT ck_rights_personal_data_lifecycle,
  ADD CONSTRAINT ck_rights_verification_note_key CHECK (
    (verification_note_ciphertext IS NULL AND verification_note_key_id IS NULL)
    OR (verification_note_ciphertext IS NOT NULL AND verification_note_key_id ~ '^[A-Za-z0-9._-]{1,30}$')
  ),
  ADD CONSTRAINT ck_rights_result_note_key CHECK (
    (result_note_ciphertext IS NULL AND result_note_key_id IS NULL)
    OR (result_note_ciphertext IS NOT NULL AND result_note_key_id ~ '^[A-Za-z0-9._-]{1,30}$')
  ),
  ADD CONSTRAINT ck_rights_personal_data_lifecycle CHECK (
    (personal_data_destroyed_at IS NULL AND detail_ciphertext IS NOT NULL
      AND octet_length(detail_ciphertext)>=30 AND encryption_key_id IS NOT NULL)
    OR
    (personal_data_destroyed_at IS NOT NULL AND detail_ciphertext IS NULL
      AND verification_note_ciphertext IS NULL AND result_note_ciphertext IS NULL
      AND encryption_key_id IS NULL AND verification_note_key_id IS NULL
      AND result_note_key_id IS NULL)
  );

ALTER TABLE evidence_package ADD COLUMN hmac_key_id VARCHAR(30);

UPDATE evidence_package
SET hmac_key_id='v1'
WHERE generation_status='FINALIZED' AND manifest_hmac IS NOT NULL;

ALTER TABLE evidence_package
  ADD CONSTRAINT ck_evidence_hmac_key_lifecycle CHECK (
    (generation_status='FINALIZED' AND manifest_hmac IS NOT NULL
      AND hmac_key_id ~ '^[A-Za-z0-9._-]{1,30}$')
    OR
    (generation_status<>'FINALIZED' AND hmac_key_id IS NULL)
  );

INSERT INTO db_change_history(script_name,script_checksum,application_version,description)
VALUES('03_patch/20260825_009_add_crypto_key_versions.sql',:'script_checksum',:'app_version',
 '권리요청 메모별 AES 키 ID와 증적 패키지별 HMAC 키 ID 수명주기 추가');

COMMIT;
