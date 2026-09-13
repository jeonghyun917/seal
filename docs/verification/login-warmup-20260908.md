# Login GET startup warm-up verification

- Scope: anonymous `/login` GET only. No authentication POST or admin menu requests. Production security and database behavior unchanged.
- TDD: empty implementation failed both tests; implemented anonymous request, no redirect following, and expected-response checks; 16 focused tests passed with WAR packaging.
- Same diagnostic WAR, warm-up disabled/enabled, three fresh JVM starts each; order off/on/on/off/off/on. Diagnostic scheduled jobs disabled. Each measured visitor request used a fresh session; response cookies were not reused. Timings below are server-side, not browser full-page timings.

| Trial | Warm-up disabled: first visitor (ms) | Warm-up enabled: first visitor (ms) |
|---|---:|---:|
| 1 | 1639.215 | 7.479 |
| 2 | 1677.593 | 6.127 |
| 3 | 1721.369 | 7.914 |
| Median | 1677.593 | 7.479 |

- Enabled warm-up itself took 2094/2099/1919 ms, moved to startup. All measured requests returned the login form with CSRF and zero SQL calls. Login credential submission and authenticated menu performance were not tested by this comparison.
- Raw logs: `target/warmup-comparison-*.out.log`; reproduction script: `target/compare-warmup.ps1` (temporary diagnostic artifacts).
- Boot runner completes before ACCEPTING_TRAFFIC. Direct port access during startup is not gated; deployments requiring strict admission must use readiness-aware routing. External Tomcat/embedded HTTPS need deployment-managed warm-up, as documented in README.
- Previous production artifact backed up at `target/seal.before-login-warmup-20260908.war`. Diagnostic classes are excluded from production WAR.
