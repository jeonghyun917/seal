# Rocky Linux 단일 서버 배포 기준

운영 구성은 `Nginx → Tomcat 10.1 → seal.war → PostgreSQL 16.14`이다. PostgreSQL 서비스명은 `postgresql-16`이며 현재 운영 장비에서 `active` 상태임을 전제로 한다.

## 사전 확인

```bash
java -version
systemctl status postgresql-16
ss -lntp | grep 5432
openssl s_client -connect relay.govkorea.com:465 -servername relay.govkorea.com </dev/null
```

필수 조건:

- Java 17
- Tomcat 10.1
- Nginx
- PostgreSQL 16.14
- Rocky Linux 장비의 SMTP 발신 공인 IP 허용 등록
- `@arte.or.kr` 발신 주소
- `consent.arte.or.kr` DNS와 HTTPS 인증서
- 서버 외부의 DB·문서 백업 위치

## 배포 디렉터리

```text
/opt/tomcat/
├─ bin/setenv.sh
├─ webapps/seal.war
└─ logs/

/etc/nginx/conf.d/seal.conf
/var/log/seal/
/var/lib/seal/documents/
/var/backups/seal/  # 임시 스테이징만 사용하고 최종 백업은 서버 외부로 전송
```

Tomcat 실행 계정만 `/var/lib/seal/documents`와 `/var/log/seal`에 접근하게 한다. `setenv.sh`는 권한 `0600`으로 제한하고 실제 비밀번호가 들어간 파일을 저장소에 커밋하지 않는다.

## 배포 순서

1. DB 백업과 복원 가능 여부를 확인한다.
2. `database` 폴더의 미적용 SQL을 검토하고 수동 적용한다.
3. 기존 WAR를 서버 외부 또는 승인된 배포 보관소에 보존한다.
4. 새 `seal.war`를 Tomcat `webapps`에 반영한다.
5. Tomcat 재기동 후 `http://127.0.0.1:8080/actuator/health`를 확인한다.
6. Nginx 경유 HTTPS 화면과 CSP, 쿠키, 리다이렉트를 확인한다.
7. 운영 승인된 테스트 주소로 이메일 한 건을 발송한다.

## 네트워크

| 방향 | 포트 | 용도 |
|---|---:|---|
| 외부 → Nginx | 443/TCP | 관리자 및 정보주체 HTTPS |
| Nginx → Tomcat | 8080/TCP, localhost | 내부 리버스 프록시 |
| 애플리케이션 → PostgreSQL | 5432/TCP, localhost | DB 연결 |
| 애플리케이션 → SMTP | 465/TCP | SSL/TLS SMTP 릴레이 |
| 서버 → DNS/NTP | 기관 정책 | 이름 해석과 시간 동기화 |

Tomcat 8080과 PostgreSQL 5432는 외부에 공개하지 않는다.

## 롤백

- 애플리케이션 장애: 이전 WAR를 복원하고 Tomcat을 재기동한다.
- DB 패치 장애: 패치 작업서에 승인된 역방향 SQL을 실행한다. 데이터 손실 가능성이 있으면 실행하지 않고 백업 복원 절차로 전환한다.
- SMTP 장애: Outbox 발송 작업을 중지하고 적재된 요청을 보존한다. 메일을 임의로 중복 재발송하지 않는다.
