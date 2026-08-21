# 1B 관리자·정책 기능 구현 계획

> 현재 인증·잠금·비밀번호 변경 기반 위에 1단계의 나머지 관리자 업무를 수직으로 구현한다.

**설계:** `docs/superpowers/specs/2026-08-20-phase1-admin-policy-design.md`

## Task 1: DB 패치와 검증

**Files**
- Create: `database/03_patch/20260821_001_create_policy_tables.sql`
- Create: `database/04_verify/002_phase1b_verify.sql`
- Modify: `docs/superpowers/specs/2026-08-20-phase1-admin-policy-design.md`

1. 보유정책/버전, 처리업무/버전/개인정보 항목 테이블의 제약조건을 검증 SQL에 먼저 표현한다.
2. 검증 SQL이 테이블 부재로 실패하는 것을 확인한다.
3. 패치 SQL에 UUID/FK/CHECK/UNIQUE/낙관적 잠금, `seal_app` 최소 권한, 변경이력 INSERT를 구현한다.
4. `seal_dev`에 체크섬과 함께 적용하고 검증 SQL을 실행한다.

## Task 2: 권한과 데이터 범위 기반

**Files**
- Create: `src/main/java/kr/or/arte/seal/admin/application/AuthorizationService.java`
- Create: `src/main/java/kr/or/arte/seal/admin/persistence/AuthorizationMapper.java`
- Create: `src/main/resources/mybatis/mappers/admin/AuthorizationMapper.xml`
- Test: `src/test/java/kr/or/arte/seal/admin/application/AuthorizationServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/admin/web/AdminAuthorizationMvcTest.java`

1. 권한이 없으면 403, 객체 범위 밖이면 404가 되는 실패 테스트를 작성한다.
2. 역할 권한과 조직 하위 범위를 조회하는 서비스를 최소 구현한다.
3. 컨트롤러 메서드 권한 및 객체 범위 검사를 연결한다.

## Task 3: 조직 관리 수직 슬라이스

**Files**
- Create: `src/main/java/kr/or/arte/seal/admin/{domain,application,persistence,web}/organization/*`
- Create: `src/main/resources/mybatis/mappers/admin/OrganizationMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/organizations/{list,detail}.jsp`
- Test: `src/test/java/kr/or/arte/seal/admin/domain/organization/OrganizationPolicyTest.java`
- Test: `src/test/java/kr/or/arte/seal/admin/web/OrganizationControllerTest.java`

조직 순환, 루트 보호, 활성 종속 객체가 있는 조직 비활성화, 낙관적 잠금을 TDD로 구현한다. 목록/상세/등록/수정/비활성화는 POST-Redirect-GET과 CSRF를 사용한다.

## Task 4: 관리자 계정 관리 수직 슬라이스

**Files**
- Create: `src/main/java/kr/or/arte/seal/admin/{application,persistence,web}/user/*`
- Create: `src/main/resources/mybatis/mappers/admin/AdminUserManagementMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/users/{list,detail}.jsp`
- Test: `src/test/java/kr/or/arte/seal/admin/application/user/AdminUserManagementServiceTest.java`
- Test: `src/test/java/kr/or/arte/seal/admin/web/AdminUserControllerTest.java`

계정 생성, 조직/상태 변경, 역할·범위 배정, 임시 비밀번호 초기화, 잠금해제를 구현한다. 비밀번호 원문은 응답·로그·감사 메타데이터에 남기지 않는다.

## Task 5: 역할·권한 관리 수직 슬라이스

**Files**
- Create: `src/main/java/kr/or/arte/seal/admin/{application,persistence,web}/role/*`
- Create: `src/main/resources/mybatis/mappers/admin/RoleManagementMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/roles/{list,detail}.jsp`
- Test: `src/test/java/kr/or/arte/seal/admin/application/role/RoleManagementServiceTest.java`

시스템 역할 필수 권한 제거 금지, 비활성 역할 배정 금지, 변경 감사기록을 TDD로 구현한다.

## Task 6: 보유정책 수직 슬라이스

**Files**
- Create: `src/main/java/kr/or/arte/seal/policy/{domain,application,persistence,web}/retention/*`
- Create: `src/main/resources/mybatis/mappers/policy/RetentionPolicyMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/retention-policies/{list,detail,form}.jsp`
- Test: `src/test/java/kr/or/arte/seal/policy/domain/retention/RetentionPolicyVersionTest.java`
- Test: `src/test/java/kr/or/arte/seal/policy/web/RetentionPolicyControllerTest.java`

값 검증, DRAFT→REVIEW_REQUESTED→APPROVED, 반려, 작성자/승인자 분리, 승인 버전 불변, 새 버전 대체를 TDD로 구현한다.

## Task 7: 처리업무 수직 슬라이스

**Files**
- Create: `src/main/java/kr/or/arte/seal/policy/{domain,application,persistence,web}/activity/*`
- Create: `src/main/resources/mybatis/mappers/policy/ProcessingActivityMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/processing-activities/{list,detail,form}.jsp`
- Test: `src/test/java/kr/or/arte/seal/policy/domain/activity/ProcessingActivityVersionTest.java`
- Test: `src/test/java/kr/or/arte/seal/policy/web/ProcessingActivityControllerTest.java`

승인 보유정책 연결, 일반 개인정보 항목 1개 이상, 처리근거별 동의 요청 가능 여부, 승인 불변성과 범위 검사를 구현한다.

## Task 8: 감사 조회와 관리자 공통 UI

**Files**
- Create: `src/main/java/kr/or/arte/seal/audit/{application,persistence,web}/query/*`
- Create: `src/main/resources/mybatis/mappers/audit/AuditEventQueryMapper.xml`
- Create: `src/main/webapp/WEB-INF/jsp/admin/audit-events/list.jsp`
- Modify: `src/main/webapp/WEB-INF/jsp/admin/home.jsp`
- Modify: `src/main/webapp/resources/css/app.css` only after preserving existing user font-size changes
- Test: `src/test/java/kr/or/arte/seal/audit/web/AuditEventControllerTest.java`

최근 7일 기본/31일 최대 검색, 필터, 마스킹, 읽기 전용 UI와 전체 메뉴를 구현한다.

## Task 9: 단계 통합 검증

1. `.\mvnw.cmd test` 전체 통과.
2. `.\mvnw.cmd package -DskipTests` 실행형 WAR 생성.
3. PostgreSQL 패치 체크섬·권한 거부·검증 SQL 통과.
4. 9090에서 로그인 후 각 목록/상세/등록/권한거부 대표 흐름 확인.
5. 미완료 외부 항목(SMTP 인증서/IP, HTTPS)을 별도 인수 항목으로 기록한다.
