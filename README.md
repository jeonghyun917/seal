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
```

DB 생성과 초기 스키마 적용은 [database/README.md](database/README.md)를 따른다.

### DB 역할 분리

- `seal_owner`: 로그인할 수 없는 객체 소유자다. PostgreSQL 관리자가 스키마 변경 때만 `SET ROLE seal_owner`로 사용한다.
- `seal_app`: 애플리케이션 실행 계정이다. 필요한 조회·등록·수정 권한만 가지며 스키마 변경과 테이블 삭제는 할 수 없다.
- `audit_event`에는 `seal_app`의 조회·등록만 허용한다.

### 최초 관리자 생성

초기 SQL 적용 후 `admin_user`가 비어 있을 때만 아래 세 값을 모두 설정한다.

```powershell
$env:SEAL_BOOTSTRAP_ADMIN_ID = 'seal-admin'
$env:SEAL_BOOTSTRAP_ADMIN_NAME = '최초 관리자'
$env:SEAL_BOOTSTRAP_ADMIN_PASSWORD = '12자 이상의 임시 비밀번호'
```

한 명이라도 관리자가 있으면 추가 생성하지 않는다. 최초 로그인 후 비밀번호 변경 화면으로 강제 이동하며, 변경이 끝나면 세션을 종료하고 새 비밀번호로 다시 로그인한다. 초기 생성이 끝나면 세 환경변수를 실행 환경에서 제거한다.

로그인 실패 잠금값은 운영 설정이며 법령에 정해진 숫자가 아니다. 기본값은 5회·15분이고 환경변수로 조정할 수 있다.

```powershell
$env:SEAL_ADMIN_MAX_LOGIN_FAILURES = '5'
$env:SEAL_ADMIN_LOCK_DURATION = '15m'
```

## 테스트와 실행

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

브라우저 확인:

- `http://localhost:8080/`
- `http://localhost:8080/system/status`
- `http://localhost:8080/login`
- `http://localhost:8080/admin`
- `http://localhost:8080/actuator/health`

로컬 프로필에서는 실제 메일 발송이 비활성화된다. SMTP 테스트는 Rocky Linux 공인 IP 등록 후 승인된 수신 주소로만 수행한다.

## 패키징

```powershell
.\mvnw.cmd clean package
```

산출물은 `target/seal.war`이다. 운영 배포는 [ops/rocky/README.md](ops/rocky/README.md)를 따른다.

## 보안 주의

- `.env`, DB 비밀번호, 관리자 비밀번호, SMTP 관련 비밀정보를 커밋하지 않는다.
- Spring 기본 인메모리 사용자는 사용하지 않는다. 관리자 인증은 PostgreSQL `admin_user`와 BCrypt strength 12를 사용한다.
- 역할 권한과 조직 범위는 서버에서 다시 검사한다. 외부 관리자 접속은 방화벽 허용 IP나 기관 VPN 같은 네트워크 통제를 함께 적용한다.
- 기본 5회 실패·15분 잠금은 변경 가능한 보안 운영값이며 법정 숫자로 표현하지 않는다.
- SMTP 접수 성공은 수신자의 최종 수신이나 열람을 의미하지 않는다.
- 토큰 원문, 비밀번호, 동의서 개인정보를 애플리케이션 로그에 기록하지 않는다.

## Phase 1A 검증 범위

로컬에서 단위·MVC 테스트, Spring Security DB Provider 구성, WAR 패키징과 공개/로그인 페이지 HTTP 스모크를 검증한다. 다음 항목은 PostgreSQL 16.14의 `seal_test` 또는 `seal_dev`에 초기 SQL을 적용한 뒤 별도로 통과해야 한다.

- `database/04_verify/001_phase1a_verify.sql`
- `seal_app`으로 실제 관리자 조회·로그인·실패 잠금·비밀번호 변경
- `seal_app`의 스키마 변경·DELETE 거부와 감사 테이블 UPDATE·DELETE 거부

실제 SMTP 발송은 공인 IP 허용과 발신주소 확인이 완료될 때까지 로컬 검증 범위에서 제외한다.
