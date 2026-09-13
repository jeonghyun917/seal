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
     WHERE table_schema='seal' AND table_name='consent_template'
       AND column_name='deleted_at' AND data_type='timestamp with time zone'
  ) OR NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema='seal' AND table_name='consent_template'
       AND column_name='deleted_by' AND data_type='uuid'
  ) OR NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema='seal' AND table_name='consent_template'
       AND column_name='delete_reason' AND character_maximum_length=500
  ) THEN
    RAISE EXCEPTION 'template soft-delete columns are missing or invalid';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid='seal.consent_template'::regclass
       AND conname='fk_consent_template_deleted_by' AND contype='f'
  ) OR NOT EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conrelid='seal.consent_template'::regclass
       AND conname='ck_consent_template_delete_metadata' AND contype='c'
  ) THEN
    RAISE EXCEPTION 'template soft-delete constraints are missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_indexes
     WHERE schemaname='seal' AND tablename='consent_template'
       AND indexname='ix_consent_template_deleted_at'
       AND indexdef LIKE '%WHERE (deleted_at IS NOT NULL)%'
  ) THEN
    RAISE EXCEPTION 'template deleted-at partial index is missing';
  END IF;

  IF NOT has_table_privilege('seal_app','seal.consent_template','UPDATE')
     OR has_table_privilege('seal_app','seal.consent_template','DELETE') THEN
    RAISE EXCEPTION 'seal_app template privileges are invalid';
  END IF;
END $$;

SELECT COALESCE((SELECT script_checksum=:'expected_checksum' FROM db_change_history
 WHERE script_name='03_patch/20260826_011_add_template_soft_delete.sql'),FALSE) checksum_matches \gset
\if :checksum_matches
\else
  \echo 'Template soft delete checksum does not match the applied SQL file'
  \quit 4
\endif

\echo 'Template soft delete verification passed'