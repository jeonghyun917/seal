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
     WHERE table_schema='seal' AND table_name='recipient_import_row'
       AND column_name='collection_basis' AND character_maximum_length=500
  ) THEN
    RAISE EXCEPTION 'recipient import row collection basis column is missing or invalid';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid='seal.recipient_import_row'::regclass
       AND conname='ck_recipient_import_collection_basis' AND contype='c'
  ) THEN
    RAISE EXCEPTION 'recipient import row collection basis constraint is missing';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260827_014_add_import_row_collection_basis.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Import row collection basis checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Import row collection basis verification passed'