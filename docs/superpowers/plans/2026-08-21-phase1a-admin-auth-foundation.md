# Phase 1A Admin Authentication Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the temporary Spring in-memory administrator with a PostgreSQL-backed administrator authentication foundation, least-privilege DB ownership, temporary login throttling, audit events, and a JSP administrator shell.

**Architecture:** Keep the application as one WAR and isolate authentication rules in `admin.domain`, use ports in `admin.application`, and place MyBatis adapters in `admin.persistence`. Spring Security delegates username/password verification to a custom authentication provider; login success and failure update the administrator account and append an audit event. Phase 1A does not implement policy CRUD, but creates the database structures and interfaces it will consume.

**Tech Stack:** Java 17, eGovFrame Runtime 5.0.0, Spring Boot 3.5.6, Spring Security 6.5, JSP/JSTL, MyBatis 3.5, PostgreSQL 16.14, JUnit 5, AssertJ, MockMvc

**Spec:** `docs/superpowers/specs/2026-08-20-phase1-admin-policy-design.md`

## Global Constraints

- SMS, OTP, electronic signatures, identity verification, Flyway, Liquibase, jQuery, Redis, and Kafka remain excluded.
- `seal_owner` is `NOLOGIN` and owns database objects; `seal_app` is the runtime login role and receives only required DML privileges.
- Administrator password hashes use BCrypt strength 12; password plaintext and hashes never enter logs or audit metadata.
- The lock default is configurable: 5 consecutive failures cause a 15-minute temporary lock. It is a security default, not a statutory numeric requirement.
- Local SMTP remains disabled and local HTTP remains allowed.
- All production behavior is test-first; configuration and SQL are verified proportionally with contract checks and later with `seal_test` PostgreSQL.
- No H2 database is introduced.

---

### Task 1: Repository baseline and least-privilege PostgreSQL schema

**Files:**
- Modify: `.gitignore`
- Modify: `database/00_admin/001_create_dev_database.sql`
- Modify: `database/01_schema/001_create_schema.sql`
- Replace: `database/01_schema/002_create_core_tables.sql`
- Modify: `database/02_data/001_common_codes.sql`
- Create: `database/04_verify/001_phase1a_verify.sql`
- Modify: `database/README.md`
- Create: `src/test/java/kr/or/arte/seal/database/DatabaseScriptContractTest.java`

**Interfaces:**
- Consumes: approved Phase 1 data model and the fact that no existing development DB has applied these initial scripts.
- Produces: `seal_owner`, `seal_app`, `organization_unit`, `admin_user`, `role`, `permission`, `user_role`, `role_permission`, `user_scope`, `audit_event`, and manual change history tables for later authentication adapters.

- [x] **Step 1: Initialize Git, commit the approved baseline, and create the feature branch**

```powershell
git init -b main
git add .gitignore .env.example .mvn mvnw mvnw.cmd pom.xml README.md src database ops docs/architecture docs/superpowers
git commit -m "chore: establish approved development baseline"
git switch -c feature/phase1-admin-auth
```

Only source, configuration, approved documents, and tests are included. Existing generated design renders and user artifacts remain untracked.

- [x] **Step 2: Reject source-text-only SQL tests and define executable DB verification**

The test guidance review found that grepping SQL source text would only prove wording, not applied ownership or privileges. `database/04_verify/001_phase1a_verify.sql` is therefore the authoritative executable check. Its execution remains a mandatory gate when `seal_test` or `seal_dev` is available.

- [x] **Step 3: Rewrite initial SQL for separated ownership and phase 1A tables**

The admin script must create `seal_owner NOLOGIN`, create or alter `seal_app LOGIN`, and create `seal_dev OWNER seal_owner`. The schema script must use `SET ROLE seal_owner`. The core schema must define account states `ACTIVE`, `LOCKED`, `INACTIVE`, UUID primary keys supplied by Java, UTC-capable `TIMESTAMPTZ`, optimistic `version_no`, foreign keys, and audit append-only grants.

The verification SQL must fail with `ON_ERROR_STOP` when ownership, grants, required tables, or change-history rows differ from the approved model.

- [ ] **Step 4: Run executable verification on PostgreSQL 16.14**

Run: `psql -X -v ON_ERROR_STOP=1 -f database/04_verify/001_phase1a_verify.sql`

Expected: PASS only after the initial scripts are applied to `seal_test` or `seal_dev`. This step is intentionally pending while the external database is being prepared.

- [x] **Step 5: Commit the schema baseline**

```powershell
git add database docs/superpowers/plans/2026-08-21-phase1a-admin-auth-foundation.md
git commit -m "feat: establish least privilege database schema"
```

---

### Task 2: Administrator password and temporary-lock domain rules

**Files:**
- Create: `src/main/java/kr/or/arte/seal/admin/domain/AdminAccountStatus.java`
- Create: `src/main/java/kr/or/arte/seal/admin/domain/LoginLockPolicy.java`
- Create: `src/main/java/kr/or/arte/seal/admin/domain/LoginFailureResult.java`
- Create: `src/main/java/kr/or/arte/seal/admin/domain/PasswordPolicy.java`
- Create: `src/main/java/kr/or/arte/seal/admin/domain/PasswordValidationResult.java`
- Create: `src/main/java/kr/or/arte/seal/admin/domain/PasswordViolation.java`
- Create: `src/test/java/kr/or/arte/seal/admin/domain/LoginLockPolicyTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/domain/PasswordPolicyTest.java`

**Interfaces:**
- Consumes: `admin_user` columns from Task 1.
- Produces: `LoginLockPolicy.recordFailure(int, Instant): LoginFailureResult`, `LoginLockPolicy.isTemporarilyLocked(Instant, Instant): boolean`, and `PasswordPolicy.validate(String, String): PasswordValidationResult` for authentication and bootstrap services.

- [x] **Step 1: Write failing lock-policy tests**

```java
@Test
void fifthConsecutiveFailureCreatesFifteenMinuteLock() {
    LoginLockPolicy policy = new LoginLockPolicy(5, Duration.ofMinutes(15));
    Instant now = Instant.parse("2026-08-21T00:00:00Z");
    LoginFailureResult result = policy.recordFailure(4, now);
    assertThat(result.consecutiveFailures()).isEqualTo(5);
    assertThat(result.lockedUntil()).contains(now.plus(Duration.ofMinutes(15)));
}

@Test
void fourthFailureDoesNotLock() {
    LoginFailureResult result = new LoginLockPolicy(5, Duration.ofMinutes(15))
            .recordFailure(3, Instant.EPOCH);
    assertThat(result.lockedUntil()).isEmpty();
}
```

- [x] **Step 2: Run lock tests and verify RED**

Run: `./mvnw.cmd -Dtest=LoginLockPolicyTest test`

Expected: test compilation fails because the domain policy does not exist.

- [x] **Step 3: Implement the minimal immutable lock policy**

```java
public record LoginLockPolicy(int maxFailures, Duration lockDuration) {
    public LoginFailureResult recordFailure(int currentFailures, Instant now) {
        int next = currentFailures + 1;
        Optional<Instant> lockedUntil = next >= maxFailures
                ? Optional.of(now.plus(lockDuration))
                : Optional.empty();
        return new LoginFailureResult(next, lockedUntil);
    }
}
```

- [x] **Step 4: Run lock tests and verify GREEN**

Run: `./mvnw.cmd -Dtest=LoginLockPolicyTest test`

Expected: PASS.

- [x] **Step 5: Write failing password-policy tests**

Test that 11 characters fail, 12 characters pass, 129 characters fail, and a value equal to the login ID fails without trimming the candidate.

- [x] **Step 6: Implement password policy and verify GREEN**

Run: `./mvnw.cmd -Dtest=PasswordPolicyTest test`

Expected: PASS for all boundary and login-ID equality cases.

- [x] **Step 7: Commit the domain rules**

```powershell
git add src/main/java/kr/or/arte/seal/admin/domain src/test/java/kr/or/arte/seal/admin/domain
git commit -m "feat: add administrator credential policies"
```

---

### Task 3: Authentication ports, MyBatis adapter, and audit append port

**Files:**
- Create: `src/main/java/kr/or/arte/seal/admin/domain/AdminAccount.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AdminAccountRepository.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AdminAuthenticationService.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AuthenticationResult.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AuthenticationStatus.java`
- Create: `src/main/java/kr/or/arte/seal/admin/persistence/AdminAccountRow.java`
- Create: `src/main/java/kr/or/arte/seal/admin/persistence/AdminAccountMapper.java`
- Create: `src/main/java/kr/or/arte/seal/admin/persistence/MyBatisAdminAccountRepository.java`
- Create: `src/main/resources/mybatis/mappers/admin/AdminAccountMapper.xml`
- Create: `src/main/java/kr/or/arte/seal/audit/application/AuditEventWriter.java`
- Create: `src/main/java/kr/or/arte/seal/audit/application/AuditEventCommand.java`
- Create: `src/main/java/kr/or/arte/seal/audit/persistence/AuditEventMapper.java`
- Create: `src/main/java/kr/or/arte/seal/audit/persistence/AuditEventRow.java`
- Create: `src/main/java/kr/or/arte/seal/audit/persistence/MyBatisAuditEventWriter.java`
- Create: `src/main/resources/mybatis/mappers/audit/AuditEventMapper.xml`
- Create: `src/test/java/kr/or/arte/seal/admin/application/AdminAuthenticationServiceTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/persistence/MyBatisAdminAccountRepositoryTest.java`

**Interfaces:**
- Consumes: Task 2 domain rules and Spring `PasswordEncoder`.
- Produces: `AdminAuthenticationService.authenticate(String, String, Instant): AuthenticationResult`, repository update methods, and `AuditEventWriter.append(AuditEventCommand)`.

- [x] **Step 1: Write failing authentication-service tests using an in-test repository fake**

Cover these behaviors separately:

```java
@Test
void validPasswordReturnsAuthenticatedAndClearsFailures() {
    AuthenticationResult result = service.authenticate("admin", "correct-password", NOW);
    assertThat(result.status()).isEqualTo(AuthenticationStatus.AUTHENTICATED);
    assertThat(repository.required("admin").consecutiveFailures()).isZero();
}

@Test
void wrongPasswordRecordsFailureWithoutRevealingAccountState() {
    AuthenticationResult result = service.authenticate("admin", "wrong-password", NOW);
    assertThat(result.status()).isEqualTo(AuthenticationStatus.INVALID_CREDENTIALS);
    assertThat(repository.required("admin").consecutiveFailures()).isEqualTo(1);
}

@Test
void fifthWrongPasswordPersistsTemporaryLock() {
    repository.replace(repository.required("admin").withConsecutiveFailures(4));
    service.authenticate("admin", "wrong-password", NOW);
    assertThat(repository.required("admin").lockedUntil())
            .isEqualTo(NOW.plus(Duration.ofMinutes(15)));
}

@Test
void activeTemporaryLockRejectsBeforePasswordCheck() {
    repository.replace(repository.required("admin").temporarilyLockedUntil(NOW.plusSeconds(60)));
    AuthenticationResult result = service.authenticate("admin", "correct-password", NOW);
    assertThat(result.status()).isEqualTo(AuthenticationStatus.TEMPORARILY_LOCKED);
}

@Test
void unknownLoginUsesSamePublicFailureResult() {
    AuthenticationResult result = service.authenticate("unknown", "any-password", NOW);
    assertThat(result.status()).isEqualTo(AuthenticationStatus.INVALID_CREDENTIALS);
}
```

The production change that makes each test fail is the missing `AdminAuthenticationService` behavior, not a mock expectation.

- [x] **Step 2: Run service tests and verify RED**

Run: `./mvnw.cmd -Dtest=AdminAuthenticationServiceTest test`

Expected: test compilation fails because the service and ports do not exist.

- [x] **Step 3: Implement minimal service and repository port**

The service normalizes login IDs with `Locale.ROOT`, checks temporary lock before `PasswordEncoder.matches`, records success or failure through the port, returns one public invalid-credentials result for unknown and incorrect accounts, and writes audit events without password data.

- [x] **Step 4: Run service tests and verify GREEN**

Run: `./mvnw.cmd -Dtest=AdminAuthenticationServiceTest test`

Expected: PASS for success, invalid credentials, fifth-failure lock, active lock, and unknown account.

- [x] **Step 5: Implement MyBatis and audit adapters**

Use bound `#{}` parameters only. Account lookup joins active role and permission codes. Failure and success updates include `WHERE version_no = #{expectedVersion}` and increment `version_no`. Audit insert accepts only pre-sanitized metadata JSON.

- [x] **Step 6: Compile all production adapters**

Run: `./mvnw.cmd -DskipTests compile`

Expected: BUILD SUCCESS with mapper interfaces and XML resources packaged.

- [x] **Step 7: Commit ports and adapters**

```powershell
git add src/main/java/kr/or/arte/seal/admin src/main/java/kr/or/arte/seal/audit src/main/resources/mybatis src/test/java/kr/or/arte/seal/admin
git commit -m "feat: add database authentication service"
```

---

### Task 4: Spring Security authentication provider and bootstrap administrator

**Files:**
- Modify: `src/main/java/kr/or/arte/seal/common/config/SecurityConfig.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/AdminSecurityProperties.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/AdminSecurityConfiguration.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/BootstrapAdminProperties.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/DatabaseAuthenticationProvider.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/BootstrapAdminRunner.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/AdminPrincipal.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-local.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `.env.example`
- Create: `src/test/java/kr/or/arte/seal/admin/config/DatabaseAuthenticationProviderTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/config/BootstrapAdminRunnerTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/config/AdminSecurityConfigurationTest.java`

**Interfaces:**
- Consumes: Task 3 `AdminAuthenticationService` and repository port.
- Produces: a Spring `AuthenticationProvider`, BCrypt strength-12 bean, configurable lock settings, one-time bootstrap, and mandatory-password-change access enforcement.

- [x] **Step 1: Write failing provider tests**

Verify authenticated roles become `ROLE_*`, permission codes become authorities without the prefix, invalid credentials always throw `BadCredentialsException`, and temporary lock does not reveal whether the password was correct.

- [x] **Step 2: Run provider tests and verify RED**

Run: `./mvnw.cmd -Dtest=DatabaseAuthenticationProviderTest test`

Expected: compilation failure because the provider does not exist.

- [x] **Step 3: Implement provider and security properties**

Properties:

```yaml
seal:
  admin-security:
    max-login-failures: ${SEAL_ADMIN_MAX_LOGIN_FAILURES:5}
    temporary-lock-duration: ${SEAL_ADMIN_LOCK_DURATION:15m}
```

Remove all `spring.security.user.*` configuration. Define `BCryptPasswordEncoder(12)` and register the custom provider.

- [x] **Step 4: Run provider tests and verify GREEN**

Run: `./mvnw.cmd -Dtest=DatabaseAuthenticationProviderTest test`

Expected: PASS.

- [x] **Step 5: Write failing bootstrap tests**

Verify the runner creates exactly one account only when the repository is empty and all three environment-backed fields exist; verify it does nothing when any user exists; verify the stored account requires a password change.

- [x] **Step 6: Implement bootstrap and verify GREEN**

Run: `./mvnw.cmd -Dtest=BootstrapAdminRunnerTest test`

Expected: PASS without logging credentials.

- [x] **Step 7: Commit security integration**

```powershell
git add src/main/java/kr/or/arte/seal/admin/config src/main/java/kr/or/arte/seal/common/config src/main/resources .env.example src/test/java/kr/or/arte/seal/admin/config
git commit -m "feat: integrate database administrator security"
```

---

### Task 5: Custom login, password change, and administrator shell

**Files:**
- Create: `src/main/java/kr/or/arte/seal/admin/web/LoginController.java`
- Create: `src/main/java/kr/or/arte/seal/admin/web/AdminHomeController.java`
- Create: `src/main/java/kr/or/arte/seal/admin/web/PasswordChangeController.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AdminPasswordChangeService.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/AdminPasswordRepository.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/PasswordChangeResult.java`
- Create: `src/main/java/kr/or/arte/seal/admin/application/PasswordChangeStatus.java`
- Create: `src/main/java/kr/or/arte/seal/admin/config/MustChangePasswordFilter.java`
- Create: `src/main/webapp/WEB-INF/jsp/admin/login.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/home.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/admin/password-change.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/error/403.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/error/404.jsp`
- Modify: `src/main/webapp/resources/css/app.css`
- Modify: `src/test/java/kr/or/arte/seal/common/config/SecurityIntegrationTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/web/AdminSecurityMvcTest.java`
- Create: `src/test/java/kr/or/arte/seal/admin/application/AdminPasswordChangeServiceTest.java`

**Interfaces:**
- Consumes: authenticated `AdminPrincipal`, password policy, password encoder, and repository update port.
- Produces: `/login`, `/admin`, and `/account/password/change` JSP workflows.

- [x] **Step 1: Write failing MVC security tests**

```java
@Test
void anonymousAdministratorRequestRedirectsToCustomLogin() throws Exception {
    mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
}

@Test
void loginPageIsPublicAndUsesCustomView() throws Exception {
    mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/login"));
}

@Test
void csrfLessPasswordChangeIsRejected() throws Exception {
    mockMvc.perform(post("/account/password/change")
                    .with(user("admin").roles("SYSTEM_ADMIN"))
                    .param("newPassword", "safe-password-123"))
            .andExpect(status().isForbidden());
}

@Test
void principalRequiringPasswordChangeCannotOpenAdminHome() throws Exception {
    mockMvc.perform(get("/admin").with(authentication(mustChangeAuthentication)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account/password/change"));
}

@Test
void normalAdministratorCanOpenAdminHome() throws Exception {
    mockMvc.perform(get("/admin").with(user("admin").roles("SYSTEM_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/home"));
}
```

- [x] **Step 2: Run MVC tests and verify RED**

Run: `./mvnw.cmd -Dtest=AdminSecurityMvcTest test`

Expected: FAIL because custom routes, views, and password-change enforcement are missing.

- [x] **Step 3: Implement controllers, JSPs, filter, and security rules**

Use a generic login error message, retain CSRF, keep public home/status/consent routes, redirect successful login with `mustChangePassword=true` to `/account/password/change`, invalidate the session after password change, and apply existing CSP/no-referrer/frame-deny headers.

- [x] **Step 4: Run MVC tests and verify GREEN**

Run: `./mvnw.cmd -Dtest=AdminSecurityMvcTest test`

Expected: PASS for public login, protected admin, CSRF, forced change, and normal access.

- [x] **Step 5: Run existing security regression tests**

Run: `./mvnw.cmd -Dtest=SecurityIntegrationTest,HomeControllerTest test`

Expected: PASS; JSP forwards and public pages remain accessible.

- [x] **Step 6: Commit the administrator UI shell**

```powershell
git add src/main/java/kr/or/arte/seal/admin src/main/webapp src/test/java/kr/or/arte/seal
git commit -m "feat: add administrator login shell"
```

---

### Task 6: Phase 1A verification and documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/plans/2026-08-21-phase1a-admin-auth-foundation.md`

**Interfaces:**
- Consumes: all Phase 1A tasks.
- Produces: a buildable feature branch and explicit evidence for what is verified without `seal_test`.

- [ ] **Step 1: Run the complete unit and MVC suite**

Run: `./mvnw.cmd test`

Expected: all tests pass with zero failures and errors.

- [ ] **Step 2: Build the executable WAR**

Run: `./mvnw.cmd clean package -DskipTests`

Expected: `target/seal.war` is produced successfully.

- [ ] **Step 3: Inspect the WAR and dependency exclusions**

Verify the WAR contains administrator JSPs, MyBatis mapper XML, eGovFrame runtime artifacts, PostgreSQL JDBC, and no Flyway, Liquibase, `.env`, or secret file.

- [ ] **Step 4: Run an HTTP smoke test for public and custom login pages**

Start the WAR with a deliberately unreachable local DB, request `/`, `/system/status`, and `/login`, and verify HTTP 200. Request `/admin` and verify it redirects to `/login`. Do not claim database login works until `seal_test` or `seal_dev` is available.

- [ ] **Step 5: Document runtime configuration and remaining DB gate**

README must list bootstrap variables, configurable lock values, `seal_owner`/`seal_app` responsibilities, initial SQL commands, and the mandatory PostgreSQL integration-test gate.

- [ ] **Step 6: Run fresh final verification and commit**

```powershell
git add README.md docs/superpowers/plans/2026-08-21-phase1a-admin-auth-foundation.md
git commit -m "docs: document administrator authentication baseline"
```

The final report must distinguish verified unit/MVC/WAR behavior from pending real PostgreSQL authentication and SQL execution.
