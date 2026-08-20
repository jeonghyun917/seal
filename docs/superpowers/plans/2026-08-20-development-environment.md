# 개인정보 전자동의 시스템 개발환경 구축 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** eGovFrame 5.0 기반의 JSP·MyBatis·PostgreSQL 모듈형 단일 WAR 프로젝트를 로컬에서 빌드하고 Rocky Linux에 배포할 수 있는 기반 환경을 구축한다.

**Architecture:** Spring Boot 기반의 배포 가능한 WAR 한 개 안에서 업무 모듈을 패키지별로 분리한다. 로컬은 내장 Tomcat으로 실행하고 운영은 Nginx와 외부 Tomcat 10.1을 사용한다. PostgreSQL Outbox를 사용해 이메일 발송 요청을 관리하며 SMS, OTP, 전자서명은 포함하지 않는다.

**Tech Stack:** Java 17, eGovFrame Runtime Maven artifacts 5.0.0, Spring Boot 3.5.6, Spring MVC, JSP/JSTL, Spring Security, MyBatis, PostgreSQL 16.14, Jakarta Mail, Maven Wrapper 3.3.4/Maven 3.9.11

**Spec:** `docs/개인정보_전자동의_관리시스템_기획서_v2.1.md`와 2026-08-20 대화에서 확정된 이메일 링크형 변경사항

## Global Constraints

- 프런트엔드는 JSP와 JSTL을 사용한다.
- 소스 폴더는 기술 계층이 아닌 업무 모듈별 패키지로 구성한다.
- 배포 단위는 단일 WAR이다.
- 운영 장비는 Rocky Linux 물리 서버 한 대이며 Nginx, Tomcat, PostgreSQL을 함께 운영한다.
- 데이터베이스는 기존 PostgreSQL 16.14를 사용한다.
- Flyway 및 다른 자동 DB 마이그레이션 도구를 사용하지 않는다.
- 이메일은 `relay.govkorea.com:465` SSL/TLS, 무인증, 공인 IP 허용 방식으로 발송한다.
- 발신 주소는 `@arte.or.kr` 도메인을 사용하며 실제 주소는 환경변수로 주입한다.
- 이메일 본문은 고지 내용을 표시하고 동의·미동의와 제출은 일회용 링크의 기관 웹페이지에서 처리한다.
- SMS, OTP, 전자서명, 실명 본인확인을 구현하지 않는다.
- 메일 상태, 링크 상태, 응답 상태를 분리한다.
- 비밀번호와 토큰 원문을 소스나 로그에 저장하지 않는다.

---

### Task 1: Maven WAR 프로젝트와 업무 모듈 골격

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`, `mvnw.cmd`
- Create: `src/main/java/kr/or/arte/seal/SealApplication.java`
- Create: `src/main/java/kr/or/arte/seal/*/package-info.java`

**Interfaces:**
- Produces: `SealApplication` 부트스트랩, `seal.war`, 업무 모듈 패키지 경계

- [ ] `pom.xml`에 WAR, Java 17, Spring Boot 3.5.6, 공식 저장소에 배포된 eGovFrame 5.0.0 런타임 JAR, JSP/JSTL, Security, MyBatis, PostgreSQL, Mail, Validation, Actuator 의존성을 선언한다.
- [ ] `SealApplication`을 `SpringBootServletInitializer`로 구성해 내장 및 외부 Tomcat 실행을 모두 지원한다.
- [ ] `common`, `admin`, `policy`, `template`, `recipient`, `request`, `delivery`, `consent`, `rights`, `retention`, `audit` 패키지 경계를 만든다.
- [ ] Maven Wrapper 3.3.4와 Maven 3.9.11 고정 설정을 추가한다.
- [ ] `mvnw.cmd -version`으로 Java 17과 Maven 3.9.11을 확인한다.

### Task 2: 웹·보안·DB 기본 설정과 JSP 확인 화면

**Files:**
- Create: `src/main/java/kr/or/arte/seal/common/config/WebMvcConfig.java`
- Create: `src/main/java/kr/or/arte/seal/common/config/SecurityConfig.java`
- Create: `src/main/java/kr/or/arte/seal/common/web/HomeController.java`
- Create: `src/main/java/kr/or/arte/seal/common/web/SystemStatusController.java`
- Create: `src/main/webapp/WEB-INF/jsp/home.jsp`
- Create: `src/main/webapp/WEB-INF/jsp/system/status.jsp`
- Create: `src/main/webapp/resources/css/app.css`
- Create: `src/test/java/kr/or/arte/seal/common/web/HomeControllerTest.java`

**Interfaces:**
- Produces: `GET /`, `GET /system/status`, JSP ViewResolver, CSRF가 적용된 기본 SecurityFilterChain

- [ ] MockMvc 테스트에 `/`의 `home` 뷰와 `/system/status`의 기술 기준 모델을 명시한다.
- [ ] 컨트롤러와 JSP ViewResolver를 구현한다.
- [ ] 정적 자원과 공개 링크 경로만 허용하고 관리 경로는 인증하도록 기본 보안 정책을 구성한다.
- [ ] JSP에 현재 확정 스택과 외부 연계 미확정 상태를 표시한다.
- [ ] `mvnw.cmd test`로 MVC 테스트를 검증한다.

### Task 3: PostgreSQL 수동 설치 스크립트와 상태 모델

**Files:**
- Create: `database/00_admin/001_create_dev_database.sql`
- Create: `database/01_schema/001_create_schema.sql`
- Create: `database/01_schema/002_create_core_tables.sql`
- Create: `database/02_data/001_common_codes.sql`
- Create: `database/03_patch/README.md`
- Create: `database/README.md`

**Interfaces:**
- Produces: `seal` 스키마, `system_setting`, `mail_outbox`, `secure_link`, `consent_response`, `audit_event`, `db_change_history`

- [ ] 전용 DB 역할과 `seal_dev` 생성 SQL을 작성한다.
- [ ] 메일·링크·응답 상태가 분리된 최소 기반 테이블과 제약조건을 작성한다.
- [ ] 적용된 SQL을 기록하는 수동 변경 이력 테이블과 초기 공통 코드를 작성한다.
- [ ] `psql` 실행 순서, 비밀번호 전달 방식, 재실행 금지 규칙을 문서화한다.
- [ ] SQL 파일의 인코딩, 트랜잭션 경계, 위험한 기본 비밀번호 부재를 정적 검증한다.

### Task 4: 환경별 설정과 SMTP 연결 경계

**Files:**
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-prod.yml`
- Create: `src/main/java/kr/or/arte/seal/delivery/config/MailRelayProperties.java`
- Create: `src/test/java/kr/or/arte/seal/delivery/config/MailRelayPropertiesTest.java`
- Create: `.env.example`

**Interfaces:**
- Produces: `seal.mail.*` 설정, 환경변수 기반 DB·SMTP·관리자 초기 계정 설정

- [ ] SMTP 호스트, 포트, SSL, 무인증과 `@arte.or.kr` 발신자 검증 테스트를 작성한다.
- [ ] `@ConfigurationProperties` 레코드와 Bean Validation을 구현한다.
- [ ] 로컬은 발송 비활성, 운영은 명시적 환경변수가 없으면 시작 실패하도록 구성한다.
- [ ] 비밀정보가 없는 `.env.example`을 작성한다.
- [ ] 설정 바인딩 테스트와 전체 테스트를 실행한다.

### Task 5: 운영 설정·문서 및 패키징 검증

**Files:**
- Create: `ops/rocky/nginx/seal.conf.example`
- Create: `ops/rocky/tomcat/setenv.sh.example`
- Create: `ops/rocky/README.md`
- Create: `README.md`
- Create: `.gitignore`

**Interfaces:**
- Produces: 로컬 실행, WAR 배포, 환경변수, 방화벽, DB 및 SMTP 점검 절차

- [ ] Nginx HTTPS 리버스 프록시와 보안 헤더 예시를 작성한다.
- [ ] Tomcat Java 17, 프로필, 비밀정보 외부화 예시를 작성한다.
- [ ] PostgreSQL 16.14와 SMTP 465 연결 사전 점검 및 롤백 절차를 문서화한다.
- [ ] `mvnw.cmd clean package`를 실행해 테스트와 WAR 생성을 확인한다.
- [ ] WAR에 JSP와 설정 파일이 포함되고 비밀번호·토큰·로컬 산출물이 포함되지 않았는지 확인한다.
