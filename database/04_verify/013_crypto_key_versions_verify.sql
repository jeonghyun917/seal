\set ON_ERROR_STOP on
\encoding UTF8
\if :{?expected_checksum}
\else
  \echo 'expected_checksum psql variable is required'
  \quit 3
\endif
\if :{?target_database}
\else
  \set target_database seal_dev
\endif
\connect :target_database
SET search_path = seal, public;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='rights_request' AND column_name='verification_note_key_id')
      OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='rights_request' AND column_name='result_note_key_id') THEN
    RAISE EXCEPTION 'rights field key columns are missing';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='seal'
      AND table_name='evidence_package' AND column_name='hmac_key_id') THEN
    RAISE EXCEPTION 'evidence HMAC key column is missing';
  END IF;
  IF EXISTS (SELECT 1 FROM rights_request WHERE
      (verification_note_ciphertext IS NULL)<>(verification_note_key_id IS NULL)
      OR (result_note_ciphertext IS NULL)<>(result_note_key_id IS NULL)) THEN
    RAISE EXCEPTION 'rights ciphertext and key IDs are inconsistent';
  END IF;
  IF EXISTS (SELECT 1 FROM evidence_package WHERE generation_status='FINALIZED'
      AND hmac_key_id IS NULL) THEN
    RAISE EXCEPTION 'finalized evidence exists without an HMAC key ID';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260825_009_add_crypto_key_versions.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Crypto key versions checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Crypto key versions verification passed'
