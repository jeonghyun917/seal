# JSP build-time compilation implementation plan

Goal: package all JSPs and tag files as compiled servlets without changing authorization, HTML, or database behavior.

Architecture: Maven process-classes runs the existing Tomcat Jasper compiler with the project's compile classpath, then packages generated classes and a generated servlet descriptor. A Spring Boot ServletContextInitializer registers the exact generated JSP mappings in both executable and external-container WAR deployments. Source-only IDE launches without the generated descriptor retain standard Jasper behavior.

Scope: 62 JSPs and one tag file; no database migrations, security relaxation, page warm-up requests, or background business jobs.

1. Add a build artifact test comparing every source JSP to the generated servlet mapping and loadable servlet classes. Verify failure before the build integration.
2. Add maven-antrun-plugin process-classes compilation, clearing only the dedicated generated JSP directories to avoid stale classes after deletions. Fail the build on JSP errors. Add the initializer and registration tests.
3. Verify JSP rebuilds after edits, normal package generation, all JSP mappings, and focused tests. Document build/deploy requirements.
4. Run an isolated local candidate with the existing diagnostic profile (background tasks disabled). Verify first login/public error pages and internal JSP servlet mappings without runtime recompilation; ask user for authenticated first/repeat navigation comparison.
5. Build and deploy the validated artifact to 9090; retain previous WAR for rollback. No live warm-up of business actions.

## Verification evidence (2026-09-08)

- Red/green: artifact test failed without generated descriptor; registration tests failed with an empty initializer; all three pass with implementation.
- Focused package run: 17 tests passed (JSP artifacts/registration and existing security tests). Updated the stale home test to expect the already-implemented `/login` redirect, without changing production routing. The security test context logged a background DB authentication warning because live credentials were deliberately not supplied; this is not a live DB integration result.
- Jasper compiled 62 JSPs and one tag class, zero errors, about 3.3 seconds. WAR contains 63 generated classes and descriptor, no generated Java or diagnostic classes.
- Isolated executable WAR registered 62 servlets. `/login` returned 200 with login form and CSRF token; unauthenticated `/admin` redirected to login; direct internal JSP access returned 404. Its fresh Tomcat work directory contains no runtime-generated JSP Java/classes.
- First isolated login GET still took 1578 ms server-side (1519 ms render), repeat 5 ms. JFR on a separate cold run showed runtime class/resource loading and nested-JAR resource verification. Do not claim that precompilation removes all first-request latency. Authenticated menu comparison awaits user interaction on 9092; diagnostic jobs are disabled.
- Deployed artifact SHA-256: `9D1131F1BD8B842E0944AB6685ADA90AEC23DE4498FDF7C690E5F260CAB8A469`. Previous WAR preserved at `target/seal.before-jsp-precompile-20260908.war`.
