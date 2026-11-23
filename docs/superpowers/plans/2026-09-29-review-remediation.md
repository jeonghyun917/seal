# Project review remediation implementation plan

> For agentic workers: execute task-by-task with executing-plans and TDD. User requested verification and correction after each stage. No commit, push, deployment or live mail/destruction.

**Goal:** Resolve the approved review findings without weakening encryption, authorization or retention safeguards.
**Architecture:** Preserve the existing Java 17 / Spring / MyBatis / JSP modules. Serialize evidence publication, hold registration and destruction on the consent request; validate current administrator security state; retain encrypted recipient data.
**Spec:** `docs/reviews/project-review-2026-09-29.md`, plus user-approved rule: holds stop execution before it starts; new holds are rejected once destruction starts.

## Constraints and review focus

- Reuse runtime-main; base HEAD 3df30c9. Git branch creation failed due filesystem permissions; leave refs untouched and no commits.
- Use synthetic test data, temporary evidence directories and isolated DB only. Do not load live .env or change SMTP throughput in a running environment.
- Check hold/execute and evidence/execute interleaving, stale administrator sessions, organization+activity restrictions, deadline equality, selection over 1,000, and download failure.
- Prior suite evidence: 423 non-integration Java and 27 JS tests passed at unchanged base SHA.

## Task 1: Authorization and destruction safety

Files: retention service/repository/mapper, evidence worker/repository/mapper, admin authentication/filter/mapper and corresponding tests.

- [x] Write and run failing regression tests for hold after approval, hold after execution, evidence publication after destruction and stale authentication.
- [x] Serialize hold/execute/evidence publication on the same request row; validate hold at execution; invalidate pending evidence; collect deterministic orphan paths for destruction.
- [x] Apply activity scope to evidence operations and related hold mutations.
- [x] Reject sessions after account/permission/password-security changes, without retaining plaintext passwords.
- [x] Run focused tests, inspect stage diff, fix related important findings, record evidence.

## Task 2: Deadline correctness

Files: public consent entry/submission services, session query record/mapper and tests.

- [x] Failing tests: flow opened immediately before request deadline, submit at/after deadline while scheduler has not run.
- [x] Cap flow expiry and check request deadline in submission under row lock.
- [x] Run consent tests and review stage diff.

## Task 3: Scale safeguards and query cost

Files: recipient query/picker, request list/controller/form, mail scheduling configuration, tests.

- [x] Bound all-selection result collection and show selection limit/truncation; enforce batch size server-side.
- [x] Avoid collecting every decrypted candidate for paginated search; retain substring behavior and encryption.
- [x] Replace per-row request list fetches with scoped aggregate/batch reads.
- [x] Make worker batch controls explicit; preserve safe default sending rate pending relay capacity verification.
- [x] Verify implemented safeguards with synthetic data/fake transport. Sparse substring matches may still scan all encrypted candidates; no searchable index or 150,000-recipient campaign engine implemented. No throughput/load-test claim.

## Task 4: User-facing consistency

Files: rights list/view queries, evidence search, common form JS/JSPs, statistics download JS, browser tests.

- [x] Localize rights types and align identity-verification instructions.
- [x] Add recipient/form identity to rights list.
- [x] Add evidence search filters with preserved pagination state.
- [x] Apply unsaved-form and download progress/failure guards without locking normal navigation.
- [x] Focused desktop/mobile browser tests and stage review for implemented changes; full remaining UI enhancements not complete.

## Progress ledger

- Setup: approved review and destruction boundary read. Working-tree changes only; no deployment or live-data operations.
- Lock ordering: execution locks job then request then package; evidence publication locks request then package; holds lock request and only read job state. Independent review found no cycle in these changed paths. Tasks 3/4 share picker response shape; keep it backward compatible.
- Ruling: preserve branch and refs after branch creation failure; only working-tree files change. Cost: changes stay uncommitted until Git write access is available.
- Tasks 1/2 focused verification passed: `target/review-stages1-2-green.log` (2026-09-29). Includes actual PostgreSQL 17 on loopback port 15439, full-FK synthetic fixtures rolled back after tests, plus unit/MVC regressions. Production PostgreSQL 16 and live application not tested.
- Task 1 review: pending destruction required migration `20260929_016`; late submission could enqueue after destruction. Both fixed and independently re-reviewed with no remaining Important findings in that scope. Migration applied only to disposable test DB; MUST be applied before deploying this code.
- Task 1 coverage: evidence list/detail/requeue activity scope; hold create/restriction/release scope; pending claim invalidation; stale session and fail-closed filter. Concurrent two-connection interleavings remain a verification limitation (locking order reviewed, no stress test yet).
- Task 2: effective expiry uses the earlier request/link/session deadline; new flow capped to deadline; submission clock refreshed after request lock. RED repro logs: `review-deadline-red.log`, `review-deadline-db-red.log`, `review-lock-deadline-red.log`.
- Final verification of this checkpoint: `mvnw.cmd -Dtest=*,!*IntegrationTest test -q` exited 0; 445 tests / 129 reports / 0 failures / 0 errors / 0 skipped, counted from report timestamps belonging to this run. `target/review-final-java.log`.
- Browser verification: all 30 tests passed (`node --test --test-concurrency=2 src/test/js/*.js`), `target/review-final-js.log`. Desktop bulk-selection and mobile download failure/retry states included.
- Independent focused re-review: no remaining Critical/Important issues in the implemented changes. Non-blocking rollout edge: old sessions do not separately cap to secure-link expiry if it differs from request expiry; newly created sessions do. Track separately, not claimed fixed.
- Ponytail complexity review: existing download handler and form guard reused; no dependencies or generic framework added. No material removable abstraction identified in this checkpoint.
- Not deployed, committed or pushed. The new migration is tested only on disposable PostgreSQL 17. Production schema migration, deployment smoke checks, PostgreSQL 16 compatibility check and concurrency stress tests remain prerequisites/limitations, not completed work.
- Follow-up implementation (2026-09-29): request/evidence/rights list recipient enrichment now uses one scoped batch read; request template summaries use one scoped batch read without per-row request detail queries. Rights rows show masked recipient context and a snapshot title only when its request organization/activity is visible.
- Evidence follow-up: literal case-insensitive title/request-code partial search; queued-at date range with Asia/Seoul inclusive days; invalid ranges show an inline error; pagination retains keyword/date/status; reset and no-results recovery added.
- Focused verification: failing regression tests reproduced before implementation; focused unit/controller tests and actual PostgreSQL 17 batch/scope/date-boundary/rights-title queries passed. SQL fixture updates clear MyBatis local cache before re-reading. All fixtures roll back.
- Fresh scoped independent review: no newly introduced Critical/Important findings. Inherited overlapping-scope template duplicates were also addressed with DISTINCT on the version summary query; inherited evidence overlap behavior remains a separate follow-up.
- Browser follow-up: 32 JavaScript/browser tests passed, including evidence filters at 1440px and 390px and pagination query preservation. Synthetic DOM/browser checks, not a deployed live-site audit.
- Final follow-up verification: full non-integration Java suite and JSP precompilation exited 0 (`target/review-followup-final-java.log`), 452 tests / 133 fresh reports / 0 failures / 0 errors / 0 skipped. Surefire's negative IntegrationTest selector takes precedence; the extended PostgreSQL integration test was verified separately with exit 0 (`target/review-followup-db.log`), not counted among those 452 tests.
- Complexity review: existing repository, masking and pagination mechanisms retained, no new dependency/framework; no material complexity removal needed. Deployment remains separate and still requires migration 20260929_016 plus environment smoke checks. No live DB/mail/server changes, commit or push in this follow-up.

## Local deployment follow-up

- 2026-09-29: user approved 9090 deployment with automatic email disabled. Recovered the previously user-supplied administrator credential-file reference from this thread; used only for PostgreSQL authentication, without printing its contents or changing authentication rules.
- Preserved previous WAR (`target/seal.before-review-deploy-20260929.war`) and schema-only backup (`target/schema-before-review-20260929.sql`). Applied patch 20260929_016 to local PostgreSQL 17.10 seal_dev transactionally with lock/statement timeouts. Constraint validated and recorded checksum matched `ad202a3810458da05d550bb3e0be3909b465d1ca5534efd6806706f9fb291970`.
- Set root local .env SEAL_MAIL_ENABLED=false and started the packaged WAR on 9090 with an explicit --seal.mail.enabled=false override. PID 26168 at deployment. Health HTTP 200 / UP, login HTTP 200 with login form; 62 precompiled JSP servlets registered and anonymous login warm-up completed.
- This is local 9090 deployment, not Rocky production deployment. Authenticated list-page smoke tests were not performed. No commit or push.
