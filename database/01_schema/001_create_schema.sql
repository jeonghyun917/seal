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

\connect seal_dev

BEGIN;
SET LOCAL ROLE seal_owner;

CREATE SCHEMA seal AUTHORIZATION seal_owner;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA seal FROM PUBLIC;
GRANT USAGE ON SCHEMA seal TO seal_app;

ALTER DATABASE seal_dev SET search_path = seal, public;
ALTER DATABASE seal_dev SET timezone = 'Asia/Seoul';

SET LOCAL search_path = seal, public;

CREATE TABLE db_change_history (
    change_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    script_name         VARCHAR(200) NOT NULL UNIQUE,
    script_checksum     CHAR(64) NOT NULL,
    application_version VARCHAR(50) NOT NULL,
    applied_by          VARCHAR(100) NOT NULL DEFAULT SESSION_USER,
    applied_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description         VARCHAR(500) NOT NULL,
    CONSTRAINT ck_db_change_checksum CHECK (script_checksum ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_db_change_app_version CHECK (length(btrim(application_version)) > 0)
);

REVOKE ALL ON db_change_history FROM PUBLIC;
GRANT SELECT ON db_change_history TO seal_app;

INSERT INTO db_change_history (
    script_name, script_checksum, application_version, description
) VALUES (
    '01_schema/001_create_schema.sql',
    :'script_checksum',
    :'app_version',
    'seal 스키마와 수동 DB 변경 이력 테이블 생성'
);

COMMIT;
