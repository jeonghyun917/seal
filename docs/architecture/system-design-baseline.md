# 시스템 설계 기준선

## 확정 범위

- eGovFrame 5.0 실행환경
- Java 17, Spring Boot 3.5.6, Spring MVC
- JSP/JSTL 프런트엔드
- MyBatis와 PostgreSQL 16.14
- 모듈형 모놀리스, 단일 WAR
- Rocky Linux 물리 서버 한 대
- Nginx, Tomcat 10.1, PostgreSQL 동시 운영
- HTML 이메일 안내와 기관 웹페이지의 일회용 보안 링크
- SMTP `relay.govkorea.com:465`, SSL/TLS, 무인증, 발신 IP 허용
- 발신 주소 `@arte.or.kr`
- Flyway 제외, SQL 파일 수동 적용 및 이력 관리

## 제외 범위

- SMS
- OTP
- 전자서명과 전자계약
- 실명 본인확인
- 메일 열람 추적 픽셀
- Kafka, Redis, 별도 메시지 브로커
- jQuery 기본 의존성

## 배포 구조

```text
Internet
   │ HTTPS 443
Nginx
   │ localhost:8080
Tomcat 10.1 / seal.war
   ├─ JSP 관리자 화면
   ├─ 모바일 동의 화면
   ├─ PostgreSQL Outbox 발송 작업
   └─ 감사·보유·파기 모듈
        │ localhost:5432
PostgreSQL 16.14
```

## 모듈 경계

| 패키지 | 책임 |
|---|---|
| `admin` | 관리자, 조직, 역할, 권한 |
| `policy` | 처리업무, 처리 근거, 보유정책 |
| `template` | 동의서 템플릿과 발행 버전 |
| `recipient` | 대상자 개별·파일 반입 |
| `request` | 요청과 고지 스냅샷 |
| `delivery` | HTML 이메일, Outbox, SMTP 접수 결과 |
| `consent` | 일회용 링크와 항목별 최종 제출 |
| `rights` | 철회와 권리요청 |
| `retention` | 보유기간과 파기 |
| `audit` | 감사 이벤트와 조회 |
| `common` | 보안, 웹, 오류 등 공통 기반 |

## 상태 분리

```text
메일: QUEUED → PROCESSING → SMTP_ACCEPTED
                    └────→ RETRY_SCHEDULED → SEND_FAILED

링크: ISSUED → ACCESSED → COMPLETED
          ├───────→ EXPIRED
          └───────→ REVOKED

응답: NOT_STARTED → IN_PROGRESS → SUBMITTED 또는 DECLINED_ALL
```

`SMTP_ACCEPTED`는 메일 릴레이 서버 접수 상태이며 최종 수신이나 열람을 의미하지 않는다.
