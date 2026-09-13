# 통계·대시보드 UX 개선 구현 기록

## 구현 범위

- 통계 도넛에는 요청·응답·메일·증적·권리요청·파기의 상호배타적 현재 상태만 포함했다.
- 응답 철회, 증적 검증 실패 이력, 권리요청 기한 초과, 활성 보존 보류는 별도 `*_WARNING` 범주와 화면 영역으로 분리했다.
- 통계 화면에 지표별 기간 기준과 종료일 포함 여부, 경고 지표가 상태 총계와 중복될 수 있음을 명시했다.
- 관리자 홈의 고정 `정상 운영`/`구축 완료` 문구를 제거하고, 2000-01-01 이후 전체 운영 이력에서 현재 미처리 상태인 항목을 로그인 관리자의 기존 조직 범위에 한정해 표시했다. 따라서 생성 후 31일 이상 지난 실패·기한 초과 건도 누락되지 않는다.
- 관리자 홈 항목은 `MAIL_VIEW`, `EVIDENCE_VIEW`, `RIGHTS_VIEW`, `RETENTION_VIEW` 중 해당 목록 조회 권한이 있을 때만 생성하며 기존 목록 필터 URL을 사용한다.

## 변경 파일

- `src/main/java/kr/or/arte/seal/statistics/application/StatisticsSnapshot.java`
- `src/main/java/kr/or/arte/seal/statistics/persistence/MyBatisStatisticsRepository.java`
- `src/main/resources/mybatis/mappers/statistics/StatisticsMapper.xml`
- `src/main/webapp/WEB-INF/jsp/admin/statistics/index.jsp`
- `src/main/webapp/resources/css/statistics.css`
- `src/main/java/kr/or/arte/seal/admin/web/AdminHomeController.java`
- `src/main/java/kr/or/arte/seal/admin/web/AdminHomeWorkItem.java`
- `src/main/webapp/WEB-INF/jsp/admin/home.jsp`
- `src/test/java/kr/or/arte/seal/statistics/application/StatisticsQueryServiceTest.java`
- `src/test/java/kr/or/arte/seal/statistics/persistence/MyBatisStatisticsRepositoryTest.java`
- `src/test/java/kr/or/arte/seal/statistics/persistence/StatisticsMapperXmlTest.java`
- `src/test/java/kr/or/arte/seal/statistics/web/StatisticsJspTest.java`
- `src/test/java/kr/or/arte/seal/admin/web/AdminHomeControllerTest.java`
- `src/test/java/kr/or/arte/seal/admin/web/AdminHomeJspTest.java`
- `src/test/java/kr/or/arte/seal/admin/web/AdminSecurityMvcTest.java`

공통 `app.css`와 `admin-ui.js`는 변경하지 않았다.

## 테스트 증거

초기 RED 실행에서 새 경고 컬렉션, 관리자 홈 작업 항목 모델 및 생성자가 없어 test compilation이 실패하는 것을 확인했다. 초기 구현 후 다음 focused 명령을 `Local\\SealUxMaven` 뮤텍스로 직렬 실행했고 exit code 0을 확인했다.

```powershell
.\mvnw.cmd -q '-Dtest=MyBatisStatisticsRepositoryTest,StatisticsMapperXmlTest,StatisticsQueryServiceTest,StatisticsJspTest,AdminHomeControllerTest,AdminHomeJspTest,AdminSecurityMvcTest' test
```

- MyBatisStatisticsRepositoryTest: 1/1 성공
- StatisticsMapperXmlTest: 1/1 성공
- StatisticsQueryServiceTest: 2/2 성공
- StatisticsJspTest: 1/1 성공
- AdminHomeControllerTest: 2/2 성공
- AdminHomeJspTest: 1/1 성공
- AdminSecurityMvcTest: 11/11 성공
- 초기 합계: 19 tests, failures 0, errors 0, skipped 0

31일 이전 backlog 회귀 수정 후 아래 focused 명령도 exit code 0으로 완료했다.

```powershell
.\mvnw.cmd -q '-Dtest=StatisticsQueryServiceTest,AdminHomeControllerTest,AdminHomeJspTest' test
```

- StatisticsQueryServiceTest: 3/3 성공 (31일 이전 생성 backlog 포함)
- AdminHomeControllerTest: 2/2 성공
- AdminHomeJspTest: 1/1 성공
- 합계: 6 tests, failures 0, errors 0, skipped 0

이후 전체 focused 목록 재실행은 병렬 작업 중인 `RetentionJobControllerTest`의 미완성 생성자/서비스 호출 때문에 test compilation 단계에서 중단됐다. 해당 오류는 이 문서의 변경 파일 밖이며, 위 성공 실행 뒤 통계·대시보드 production 파일은 바뀌지 않았다.

## 자체 검토

- 기존 `visible_scope` CTE와 actor id 전달을 그대로 유지해 조직 범위를 확장하지 않았다.
- 대시보드 링크는 해당 목록의 기존 `@PreAuthorize` 권한과 일치하도록 생성 전에 권한을 검사한다.
- 권리요청 1건이 `RIGHTS` 현재 상태와 `RIGHTS_WARNING/OVERDUE`에 각각 나타나더라도 도넛 총계에는 현재 상태 1건만 합산된다.
- 증적 검증 경고는 패키지 수가 아니라 기간 내 실패 검증 이력 수임을 통계 화면 문구에 반영했다.
- 대시보드에는 과거 검증 실패 이력을 현재 처리 대상으로 단정하지 않고, 현재 생성 상태가 `FAILED`인 증적만 표시한다. 이후 검증 성공 여부를 판정하는 별도 조회가 없기 때문이다.
- 새로운 의존성, 스키마 변경, 외부 변경, 커밋은 만들지 않았다.

## 제한 및 후속 확인

- `StatisticsPostgresIntegrationTest`는 `@Transactional` 롤백을 확인한 뒤 로컬 비밀번호 파일을 출력하지 않고 실행했으나, `seal_app` 인증 실패로 연결을 열지 못해 1 error로 종료됐다. 데이터 변경은 시작되지 않았다. 유효한 로컬 DB 자격 증명으로 재실행이 필요하다.
- 전체 테스트 suite와 실제 브라우저 데스크톱/모바일 확인은 통합 담당의 최종 1회 검증 대상으로 남겼다.
- 처리업무·보유정책 승인 대기 건은 기존 통계 조회 계층에 해당 지표가 없고, 안전한 단순 read endpoint 재사용으로 끝나지 않아 이번 범위에서 추가하지 않았다.
