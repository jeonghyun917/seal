# 개인정보 전자동의·증적 관리시스템

eGovFrame 5.0 기반 JSP 모듈형 모놀리스 프로젝트다. HTML 이메일은 개인정보 동의 내용을 안내하며, 동의·미동의 선택과 최종 제출은 기관 웹페이지의 일회용 보안 링크에서 처리한다.

## 확정 기술

| 구분 | 기준 |
|---|---|
| Java | 17 |
| eGovFrame | Runtime Maven artifacts 5.0.0 |
| Spring | Boot 3.5.6 / Framework 6.2.11 |
| View | JSP / JSTL |
| Persistence | MyBatis 3.5 계열 |
| DB | PostgreSQL 16.14 |
| Build | Maven Wrapper 3.3.4 / Maven 3.9.11 |
| Deploy | 단일 WAR, Tomcat 10.1 |
| Mail | `relay.govkorea.com:465`, SSL/TLS, 무인증 |

공식 eGovFrame 5.0 실행환경은 Java 17 이상을 지원한다. 프로젝트에는 공식 Maven 저장소에 실제 게시된 5.0.0 런타임 모듈을 사용한다.

## 로컬 준비

Java 17만 시스템에 설치하면 Maven은 Wrapper가 자동으로 준비한다.

```powershell
.\mvnw.cmd -version
```

PostgreSQL이 다른 Rocky Linux 장비에 있다면 `localhost` 대신 해당 장비의 내부 주소를 사용한다. DB를 외부 인터넷에 공개하지 않는다.

```powershell
$env:SEAL_DB_URL = 'jdbc:postgresql://DB내부주소:5432/seal_dev?currentSchema=seal'
$env:SEAL_DB_USERNAME = 'seal_app'
$env:SEAL_DB_PASSWORD = '로컬에서만 입력'
$env:SEAL_ADMIN_PASSWORD = '로컬에서만 입력'
```

DB 생성과 초기 스키마 적용은 [database/README.md](database/README.md)를 따른다.

## 테스트와 실행

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

브라우저 확인:

- `http://localhost:8080/`
- `http://localhost:8080/system/status`
- `http://localhost:8080/actuator/health`

로컬 프로필에서는 실제 메일 발송이 비활성화된다. SMTP 테스트는 Rocky Linux 공인 IP 등록 후 승인된 수신 주소로만 수행한다.

## 패키징

```powershell
.\mvnw.cmd clean package
```

산출물은 `target/seal.war`이다. 운영 배포는 [ops/rocky/README.md](ops/rocky/README.md)를 따른다.

## 보안 주의

- `.env`, DB 비밀번호, 관리자 비밀번호, SMTP 관련 비밀정보를 커밋하지 않는다.
- 현재 Spring 기본 계정은 개발환경 확인용이다. 운영 전 DB 기반 관리자 계정, 역할 권한, MFA를 구현해야 한다.
- SMTP 접수 성공은 수신자의 최종 수신이나 열람을 의미하지 않는다.
- 토큰 원문, 비밀번호, 동의서 개인정보를 애플리케이션 로그에 기록하지 않는다.
