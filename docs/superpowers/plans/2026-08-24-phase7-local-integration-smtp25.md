# Phase 7 Local Integration and SMTP 25 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the verified unauthenticated SMTP port 25 relay to the application and complete the remaining local integration acceptance without Rocky Linux, backup/restore, HTTPS, domain, or Nginx work.

**Architecture:** Keep Spring Boot JavaMail and the existing PostgreSQL Outbox. Drive the SMTP transport mode from one environment setting used by both Spring Mail and `seal.mail`, allow only the two institution-approved relay combinations (`25/NONE/no-auth` and `465/SSL/no-auth`), then verify one real message through the application worker. Finish with the existing automated, PostgreSQL, WAR, and representative browser checks.

**Tech Stack:** Java 17, eGovFrame 5.0 runtime, Spring Boot 3.5.6, Spring Mail/Jakarta Mail, PostgreSQL Outbox, MyBatis, JSP, JUnit 5, AssertJ, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-08-21-consent-system-full-build-design.md`

## Global Constraints

- Port 25 uses no SSL, no STARTTLS, and no SMTP authentication.
- Port 465 uses implicit SSL and no SMTP authentication.
- Enabled mail must use an `@arte.or.kr` sender; the local accepted sender is `seal@arte.or.kr`.
- A real email test is limited to one message addressed to `vheraclesv@arte.or.kr`.
- SMTP acceptance is not represented as mailbox read/open tracking.
- Do not add SMS, OTP, electronic signatures, tracking pixels, or new paid infrastructure.
- Do not include Rocky Linux, backup/restore, HTTPS, domain, or Nginx work.

---

### Task 1: SMTP transport-mode configuration

**Files:**
- Modify: `src/test/java/kr/or/arte/seal/delivery/config/MailRelayPropertiesTest.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/config/MailRelayProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `.env.example`

**Interfaces:**
- Consumes: existing `seal.mail.*` configuration and Spring Boot `spring.mail.*` auto-configuration.
- Produces: `SEAL_SMTP_SSL_ENABLED` as the single switch for implicit SSL in both configuration namespaces.

- [x] **Step 1: Write failing property-validation tests**

Add tests proving that enabled `25/ssl=false/auth=false` and `465/ssl=true/auth=false` are accepted, while port/mode mismatches and any authenticated relay are rejected.

- [x] **Step 2: Run the focused test and verify RED**

Run: `.\mvnw.cmd -Dtest=MailRelayPropertiesTest test`

Expected: the port 25 no-SSL test fails because the current validator requires port 465 and SSL.

- [x] **Step 3: Implement the minimal validation and environment binding**

Change the class-level assertion to:

```java
return !enabled || (!authEnabled && ((port == 25 && !sslEnabled) || (port == 465 && sslEnabled)));
```

Bind both `spring.mail.properties.mail.smtp.ssl.enable` and `seal.mail.ssl-enabled` to `${SEAL_SMTP_SSL_ENABLED:false}`, explicitly keep STARTTLS disabled, and set the example sender to `seal@arte.or.kr` with port `25`.

- [x] **Step 4: Run the focused test and verify GREEN**

Run: `.\mvnw.cmd -Dtest=MailRelayPropertiesTest test`

Expected: all `MailRelayPropertiesTest` tests pass.

### Task 2: Application Outbox real-mail acceptance

**Files:**
- Modify: `README.md`
- Runtime only: untracked `.env` / process environment; never commit secrets.

**Interfaces:**
- Consumes: existing request/link creation, encrypted `mail_outbox`, scheduler/worker, and `JavaMailTransport`.
- Produces: one Outbox record ending in `SMTP_ACCEPTED`; mailbox receipt remains a user-owned confirmation outside application tracking.

- [x] **Step 1: Verify mail configuration without sending**

Start the application on port 9090 with `SEAL_MAIL_ENABLED=true`, `SEAL_SMTP_PORT=25`, `SEAL_SMTP_SSL_ENABLED=false`, `SEAL_MAIL_FROM_ADDRESS=seal@arte.or.kr`, and existing local DB/key settings. Confirm startup validation succeeds.

- [x] **Step 2: Queue exactly one approved message through the application flow**

Use the existing administrator request/link/mail flow and recipient `vheraclesv@arte.or.kr`. Do not use a direct SMTP client for this acceptance step.

- [x] **Step 3: Verify Outbox state and relay result**

The queued row transitioned once to `SMTP_ACCEPTED` at `2026-08-24 18:21:22 KST` with attempt count 1 (no retry). Request `CR-20260824-F2UKJTBD`, link state, response state, and mail state remained separate. No secure token was recorded. Mailbox delivery or reading is not inferred from relay acceptance.

### Task 3: Local integration acceptance

**Files:**
- Modify only files needed for reproducible defects found by the scoped checks.
- Modify: `README.md`

**Interfaces:**
- Consumes: Phases 1-6 implementation and the SMTP configuration from Task 1.
- Produces: fresh automated, PostgreSQL, WAR, and representative browser evidence.

- [x] **Step 1: Run the full automated suite and package the WAR**

Run: `.\mvnw.cmd clean verify`

Expected: zero failures/errors and `target/seal.war` created.

- [x] **Step 2: Run PostgreSQL verification scripts**

No schema changes were applied. All verification scripts `001` through `011` passed against the local `seal_dev`; integration tests also exercised the local PostgreSQL database. The Phase 1A check was corrected to require the three initial history rows without rejecting later patch history.

- [x] **Step 3: Run representative browser and HTTP smoke checks**

Browser checks covered the public home and administrator login with no console errors. Authenticated HTTP checks covered request detail, `SMTP_ACCEPTED` mail detail and authenticated 404; the issued consent link GET returned 200 without starting or submitting consent. Mobile layout and 403 behavior remain covered by the automated MVC/CSS contracts without exposing credentials or the one-time token to browser automation.

- [x] **Step 4: Record only scoped fixes**

Fix only Critical or Important defects that block the approved SMTP/local-integration acceptance. Record unrelated or operational findings as deferred.

### Task 4: Documentation and branch delivery

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture/system-design-baseline.md`
- Modify: `docs/superpowers/plans/2026-08-21-full-build-roadmap.md`
- Modify: this plan file to mark completed steps.

**Interfaces:**
- Consumes: verification evidence from Tasks 1-3.
- Produces: one coherent local-development baseline and a reviewable Git branch.

- [x] **Step 1: Update documentation**

Document `relay.govkorea.com:25`, no SSL/STARTTLS/authentication, `seal@arte.or.kr`, the environment variables, SMTP acceptance semantics, and the completed local SMTP acceptance. Preserve 465/SSL as an alternate approved mode.

- [x] **Step 2: Review configuration and documentation consistency**

Search for stale statements that claim SMTP 465 is mandatory or real SMTP remains unverified. Historical documents may remain unchanged only when clearly marked historical.

- [x] **Step 3: Verify and commit**

Focused tests and the final full `clean verify` passed (228 tests, zero failures/errors/skips), `target/seal.war` was created, and the branch was inspected with `git diff --check` and `git status --short`. Do not merge to `main` without presenting the verified branch result to the user.
