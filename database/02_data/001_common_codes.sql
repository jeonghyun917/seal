\set ON_ERROR_STOP on
\encoding UTF8

BEGIN;
SET LOCAL search_path = seal, public;

INSERT INTO common_code (code_group, code_value, code_name, sort_order, description) VALUES
    ('MAIL_STATUS', 'QUEUED', '발송 대기', 10, '아직 SMTP 릴레이에 전달하지 않은 상태'),
    ('MAIL_STATUS', 'PROCESSING', '발송 처리 중', 20, '발송 작업자가 처리권을 획득한 상태'),
    ('MAIL_STATUS', 'SMTP_ACCEPTED', '메일 서버 접수', 30, 'SMTP 릴레이가 메시지를 접수한 상태이며 최종 수신을 의미하지 않음'),
    ('MAIL_STATUS', 'SEND_FAILED', '발송 실패', 40, '재시도하지 않는 즉시 실패 또는 최대 재시도 초과'),
    ('MAIL_STATUS', 'RETRY_SCHEDULED', '재시도 예정', 50, '일시 장애로 다음 시도 시각을 예약한 상태'),
    ('LINK_STATUS', 'ISSUED', '발급', 10, '일회용 링크 발급 완료'),
    ('LINK_STATUS', 'ACCESSED', '접속', 20, '사용자가 링크 대상 웹페이지에 접속'),
    ('LINK_STATUS', 'EXPIRED', '만료', 30, '유효기간 종료'),
    ('LINK_STATUS', 'REVOKED', '폐기', 40, '취소 또는 재발급으로 무효화'),
    ('LINK_STATUS', 'COMPLETED', '사용 완료', 50, '최종 응답 제출 완료'),
    ('RESPONSE_STATUS', 'NOT_STARTED', '미응답', 10, '응답을 시작하지 않음'),
    ('RESPONSE_STATUS', 'IN_PROGRESS', '작성 중', 20, '항목별 결정을 작성 중'),
    ('RESPONSE_STATUS', 'SUBMITTED', '동의 결과 제출', 30, '하나 이상의 동의 결과를 최종 제출'),
    ('RESPONSE_STATUS', 'DECLINED_ALL', '전체 미동의 제출', 40, '모든 항목에 미동의로 최종 제출');

INSERT INTO db_change_history (script_name, description)
VALUES ('02_data/001_common_codes.sql', '메일, 링크, 응답 상태 초기 코드 등록');

COMMIT;
