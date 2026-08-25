# Review Findings Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the five approved review findings without expanding product scope.

**Architecture:** Keep the existing modular monolith and MyBatis transaction boundaries. Add explicit restriction and public-session lifecycle query ports, keep pagination routing inside the JSP request contract, expose resumable retention state on the existing record, and distinguish evidence lease loss from generation failure.

**Tech Stack:** Java 17, Spring Boot 3.5.6, eGovFrame 5.0 runtime, JSP/JSTL, MyBatis, PostgreSQL 16/17, JUnit 5, AssertJ, Mockito.

**Spec:** `docs/superpowers/specs/2026-08-24-phase6-evidence-rights-lifecycle-design.md`

## Global Constraints

- SMS, OTP, electronic signature, mobile/PASS verification, SSO/LDAP, Redis, Kafka, and paid KMS remain excluded.
- No Flyway or Liquibase; database changes use the existing manual SQL/checksum process.
- Public consent uses HTML email plus a one-time web link; GET does not record consent access.
- Existing organization scope, author/approver separation, immutable evidence, and PostgreSQL locking rules remain intact.
- This plan changes only the five approved findings and their regression tests.

---

### Task 1: Enforce completed restriction requests

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/retention/application/RetentionRepository.java`
- Modify: `src/main/java/kr/or/arte/seal/retention/persistence/RetentionMapper.java`
- Modify: `src/main/resources/mybatis/mappers/retention/RetentionMapper.xml`
- Modify: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagementService.java`
- Modify: `src/main/java/kr/or/arte/seal/delivery/application/ConsentDeliveryService.java`
- Test: `src/test/java/kr/or/arte/seal/request/application/ConsentRequestManagementServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/delivery/application/ConsentDeliveryServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/retention/persistence/RetentionPostgresIntegrationTest.java`

**Interfaces:**
- Produces: `RetentionRepository.hasActiveRestriction(UUID recipientId, LocalDate today)`.
- Consumes: active `legal_hold` rows with `hold_reason_code='RIGHTS_RESTRICTION'` linked through `consent_request.recipient_id`.

- [ ] Write tests proving an active restriction rejects new request creation, initial delivery, and redelivery.
- [ ] Run the focused request and delivery tests and confirm they fail because no restriction query is made.
- [ ] Add the repository query and guard both service entry points with `RequestBusinessConflictException`.
- [ ] Run the focused tests and PostgreSQL restriction query test and confirm they pass.

### Task 2: Generate pagination links from the original controller path

**Files:**
- Modify: `src/main/webapp/WEB-INF/jsp/fragments/admin/pagination.jsp`
- Test: `src/test/java/kr/or/arte/seal/common/web/AdminPaginationJspTest.java`

**Interfaces:**
- Consumes: servlet forward attribute `jakarta.servlet.forward.servlet_path`.
- Produces: context-relative previous and next links that retain `page`, `status`, and `email` query parameters.

- [ ] Add a regression test that renders the pagination fragment through a servlet forward and asserts `/admin/...` links rather than `/WEB-INF/jsp/...` links.
- [ ] Run the test and confirm it fails with the internal JSP path.
- [ ] Resolve the original servlet path from the forward attribute with a direct-request fallback.
- [ ] Run the focused test and confirm it passes.

### Task 3: Expire and destroy abandoned public consent flows

**Files:**
- Create: `src/main/java/kr/or/arte/seal/request/application/PublicConsentSessionLifecycle.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/application/PublicConsentRepository.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/persistence/PublicConsentMapper.java`
- Modify: `src/main/java/kr/or/arte/seal/consent/persistence/MyBatisPublicConsentRepository.java`
- Modify: `src/main/resources/mybatis/mappers/consent/PublicConsentMapper.xml`
- Modify: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestExpiryService.java`
- Modify: `src/main/java/kr/or/arte/seal/request/application/ConsentRequestManagementService.java`
- Test: `src/test/java/kr/or/arte/seal/request/scheduling/ConsentRequestExpiryJobTest.java`
- Test: `src/test/java/kr/or/arte/seal/consent/persistence/PublicConsentPostgresIntegrationTest.java`

**Interfaces:**
- Produces: request-owned port `PublicConsentSessionLifecycle.expireSessions(UUID requestId, Instant now)`; `PublicConsentRepository` extends that port so the MyBatis adapter remains the single implementation.
- Behavior: transition every ACTIVE session for an expired or cancelled request to EXPIRED and clear `flow_hmac` with `flow_destroyed_at`; submission completion expires all other sessions.

- [ ] Add tests for request expiry, cancellation, and a second active session remaining after submission.
- [ ] Run the focused tests and confirm ACTIVE session state/HMAC assertions fail.
- [ ] Add atomic mapper updates and invoke them inside the existing request transactions.
- [ ] Run focused unit and PostgreSQL tests and confirm they pass.

### Task 4: Resume interrupted retention execution from the administrator UI

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/retention/application/RetentionJobRecord.java`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/retention/detail.jsp`
- Test: `src/test/java/kr/or/arte/seal/retention/application/RetentionJobRecordTest.java`

**Interfaces:**
- Produces: `RetentionJobRecord.executionAvailable()` and `RetentionJobRecord.executionKey()`.
- Behavior: APPROVED uses the job ID as its first execution key; EXECUTING exposes the stored key for resume; other states expose no execution action.

- [ ] Add record tests for APPROVED, EXECUTING, and EXECUTED states.
- [ ] Run the focused test and confirm the methods are missing.
- [ ] Add the two record methods and render the execution form for both resumable states.
- [ ] Run the focused test and JSP structure test and confirm they pass.

### Task 5: Treat evidence lease loss as a non-fatal worker outcome

**Files:**
- Create: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceClaimLostException.java`
- Modify: `src/main/java/kr/or/arte/seal/evidence/persistence/MyBatisEvidenceRepository.java`
- Modify: `src/main/java/kr/or/arte/seal/evidence/application/EvidenceWorker.java`
- Modify: `src/test/java/kr/or/arte/seal/evidence/application/EvidenceWorkerTest.java`

**Interfaces:**
- Produces: `EvidenceClaimLostException` when an optimistic worker update affects no row.
- Behavior: a worker that lost its lease does not overwrite the new owner and continues with the remaining batch items.

- [ ] Add a two-item worker test where finalizing the first item loses its claim and the second item still finalizes.
- [ ] Run the focused test and confirm the first exception aborts the batch.
- [ ] Throw the typed exception from optimistic updates and handle it separately from generation failure.
- [ ] Run the focused test and confirm both items are handled without an uncaught exception.

### Task 6: Final verification

**Files:**
- Review only: all files changed by Tasks 1-5.

**Interfaces:**
- Consumes: the exact branch diff and configured local PostgreSQL database.
- Produces: fresh test, integration, build, and clean-worktree evidence.

- [ ] Run all Maven tests with `SEAL_DB_PASSWORD` loaded from the existing `.env` without printing it.
- [ ] Run `mvn verify` and confirm zero failures and errors.
- [ ] Inspect `git diff --check`, `git diff --stat`, and the complete diff for scope creep.
- [ ] Confirm no secret, generated evidence, or unrelated user file is staged.
