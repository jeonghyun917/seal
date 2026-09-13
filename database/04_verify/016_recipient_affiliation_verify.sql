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
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema='seal' AND table_name='recipient'
       AND column_name='affiliation_name' AND character_maximum_length=200
  ) OR NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema='seal' AND table_name='recipient_import_row'
       AND column_name='affiliation_name' AND character_maximum_length=200
  ) THEN
    RAISE EXCEPTION 'recipient affiliation columns are missing or invalid';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid='seal.recipient'::regclass
       AND conname='ck_recipient_affiliation_name' AND contype='c'
  ) OR NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid='seal.recipient_import_row'::regclass
       AND conname='ck_recipient_import_affiliation_name' AND contype='c'
  ) THEN
    RAISE EXCEPTION 'recipient affiliation constraints are missing';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260826_012_add_recipient_affiliation.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Recipient affiliation checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Recipient affiliation verification passed'