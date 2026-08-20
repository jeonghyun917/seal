\set ON_ERROR_STOP on
\encoding UTF8

BEGIN;

CREATE SCHEMA seal AUTHORIZATION seal_app;

ALTER ROLE seal_app SET search_path = seal, public;
ALTER ROLE seal_app SET timezone = 'Asia/Seoul';

SET search_path = seal, public;

CREATE TABLE db_change_history (
    change_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    script_name     VARCHAR(200) NOT NULL UNIQUE,
    script_checksum VARCHAR(64),
    applied_by      VARCHAR(100) NOT NULL DEFAULT CURRENT_USER,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description     VARCHAR(500) NOT NULL
);

INSERT INTO db_change_history (script_name, description)
VALUES ('01_schema/001_create_schema.sql', 'seal 스키마 및 수동 DB 변경 이력 테이블 생성');

COMMIT;
