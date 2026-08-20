\set ON_ERROR_STOP on
\encoding UTF8

-- Run once as the PostgreSQL administrative account.
-- The password is prompted by psql and is not written to this file.
\prompt 'Password for the new seal_app role: ' seal_app_password

SELECT format(
    'CREATE ROLE seal_app LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',
    :'seal_app_password'
) \gexec

CREATE DATABASE seal_dev
    WITH OWNER = seal_app
         ENCODING = 'UTF8'
         TEMPLATE = template0;

\unset seal_app_password
