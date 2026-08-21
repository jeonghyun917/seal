\set ON_ERROR_STOP on
\encoding UTF8

-- PostgreSQL administrative account로 빈 개발 환경에 한 번 실행한다.
-- 비밀번호는 psql 프롬프트로만 입력하고 파일이나 명령행에 남기지 않는다.
\prompt 'Password for the seal_app runtime role: ' seal_app_password

SELECT 'CREATE ROLE seal_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT'
WHERE NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'seal_owner')
\gexec

SELECT format(
    'CREATE ROLE seal_app LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'seal_app_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'seal_app')
\gexec

SELECT format('ALTER ROLE seal_app PASSWORD %L', :'seal_app_password')
WHERE EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'seal_app')
\gexec

SELECT 'CREATE DATABASE seal_dev WITH OWNER = seal_owner ENCODING = ''UTF8'' TEMPLATE = template0'
WHERE NOT EXISTS (SELECT 1 FROM pg_catalog.pg_database WHERE datname = 'seal_dev')
\gexec

ALTER DATABASE seal_dev OWNER TO seal_owner;
REVOKE ALL ON DATABASE seal_dev FROM PUBLIC;
GRANT CONNECT ON DATABASE seal_dev TO seal_app;

ALTER ROLE seal_app SET search_path = seal, public;
ALTER ROLE seal_app SET timezone = 'Asia/Seoul';

\unset seal_app_password
