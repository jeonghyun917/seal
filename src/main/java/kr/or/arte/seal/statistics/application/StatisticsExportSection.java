package kr.or.arte.seal.statistics.application;

import java.util.List;

/** Fixed export definitions; SQL identifiers never come from request parameters. */
public enum StatisticsExportSection {
    REQUEST("동의 요청", "요청 내역", "request", "동의 요청 생성 일시", "r.created_at",
        "FROM seal.consent_request r", "",
        "r.consent_request_id", "r.request_status",
        "r.expires_at,r.cancelled_at,r.cancellation_reason,r.updated_at,pa.activity_name,rp.policy_name,tv.version_number",
        "expires_at:응답 만료일시|cancelled_at:취소일시|cancellation_reason:취소 사유|updated_at:최종 수정일시|activity_name:개인정보 이용 업무|policy_name:보유정책|version_number:동의서 버전"),
    RESPONSE("동의 응답·철회", "응답 내역", "response", "응답 제출 일시", "x.submitted_at",
        "FROM seal.consent_response x JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.consent_response_id", "x.response_status",
        "x.snapshot_hash,(SELECT string_agg(i.item_title || ': ' || CASE ri.decision WHEN 'AGREE' THEN '동의' ELSE '미동의' END, E'\\n' ORDER BY ri.item_order) FROM seal.consent_response_item ri JOIN seal.consent_request_item i USING(consent_request_item_id) WHERE ri.consent_response_id=x.consent_response_id) decisions,(SELECT count(*) FROM seal.consent_response_item ri WHERE ri.consent_response_id=x.consent_response_id AND ri.decision='AGREE') agreed_count,(SELECT count(*) FROM seal.consent_response_item ri WHERE ri.consent_response_id=x.consent_response_id) decision_count",
        "agreed_count:동의 항목 수|decision_count:전체 항목 수|decisions:항목별 응답|snapshot_hash:발행 동의서 해시"),
    WITHDRAWAL("동의 응답·철회", "철회 내역 (상태 총계 미합산)", "response", "철회 발생 일시", "x.withdrawn_at",
        "FROM seal.consent_withdrawal x JOIN seal.consent_response res USING(consent_response_id) JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.consent_withdrawal_id", "'WITHDRAWN'",
        "x.withdrawal_scope,x.reason_code,x.rights_request_id", "withdrawal_scope:철회 범위|reason_code:사유 코드|rights_request_id:관련 권리요청 ID"),
    MAIL("메일", "메일 내역", "mail", "메일 발송함 등록 일시", "x.created_at",
        "FROM seal.mail_outbox x JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.mail_outbox_id", "x.mail_status",
        "x.mail_subject,x.scheduled_at,x.next_attempt_at,x.attempt_count,x.smtp_accepted_at,x.last_error_code,x.updated_at",
        "mail_subject:메일 제목|scheduled_at:발송 예약일시|next_attempt_at:다음 시도일시|attempt_count:시도 횟수|smtp_accepted_at:SMTP 접수일시|last_error_code:최근 오류 코드|updated_at:최종 수정일시"),
    EVIDENCE("증적", "증적 내역", "evidence", "증적 생성 대기 등록 일시", "x.queued_at",
        "FROM seal.evidence_package x JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.evidence_package_id", "x.generation_status",
        "x.finalized_at,x.failed_at,x.destroyed_at,x.attempt_count,x.last_error_code,x.manifest_sha256,x.generator_version,(SELECT count(*) FROM seal.evidence_file f WHERE f.evidence_package_id=x.evidence_package_id) file_count,(SELECT sum(f.file_size) FROM seal.evidence_file f WHERE f.evidence_package_id=x.evidence_package_id) file_bytes,(SELECT string_agg(f.file_name, ', ' ORDER BY f.file_name) FROM seal.evidence_file f WHERE f.evidence_package_id=x.evidence_package_id) file_names",
        "finalized_at:생성 완료일시|failed_at:생성 실패일시|destroyed_at:파기일시|attempt_count:시도 횟수|last_error_code:최근 오류 코드|file_count:파일 수|file_bytes:총 파일 크기 (bytes)|file_names:생성 파일명|manifest_sha256:매니페스트 SHA-256|generator_version:생성기 버전"),
    VERIFICATION("증적", "검증 실패 내역 (상태 총계 미합산)", "evidence", "검증 실패 사건 발생 일시", "x.verified_at",
        "FROM seal.evidence_verification x JOIN seal.evidence_package e USING(evidence_package_id) JOIN seal.consent_request r USING(consent_request_id)",
        "AND x.verification_result<>'VALID'", "x.evidence_verification_id", "'VERIFY_' || x.verification_result",
        "x.evidence_package_id,x.reason_code", "evidence_package_id:증적 ID|reason_code:사유 코드"),
    RIGHTS("권리요청", "권리요청 내역", "rights", "권리요청 접수 등록 일시", "x.created_at",
        "FROM seal.rights_request x LEFT JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.rights_request_id", "x.request_status",
        "x.request_code rights_code,x.rights_type,x.received_date,x.due_date,x.verification_channel,x.identity_verified_at,x.decision_code,x.decided_at,x.completed_at,x.cancelled_at,(x.due_date<CURRENT_DATE AND x.request_status NOT IN ('COMPLETED','CANCELLED')) overdue",
        "rights_code:권리요청번호|rights_type:권리 유형|received_date:접수일|due_date:처리기한|overdue:현재 기한 초과|verification_channel:본인확인 경로|identity_verified_at:본인확인일시|decision_code:결정 코드|decided_at:결정일시|completed_at:완료일시|cancelled_at:취소일시"),
    RETENTION("파기·보존 보류", "파기 작업 내역", "retention", "파기 작업 요청 일시", "x.requested_at",
        "FROM seal.retention_job x JOIN seal.consent_request r USING(consent_request_id)", "",
        "x.retention_job_id", "x.job_status",
        "rp.policy_name,x.anchor_date,x.due_date,x.destruction_method,x.approved_at,x.executed_at,x.confirmed_at,x.held_at,x.last_error_code,x.destroyed_mail_count,x.destroyed_file_count,x.recipient_pii_destroyed",
        "policy_name:보유정책|anchor_date:보유기간 시작일|due_date:파기 예정일|destruction_method:파기 방법|approved_at:승인일시|executed_at:실행일시|confirmed_at:결과 확인일시|held_at:보류일시|last_error_code:최근 오류 코드|destroyed_mail_count:파기 메일 수|destroyed_file_count:파기 파일 수|recipient_pii_destroyed:수신자 개인정보 파기"),
    HOLD("파기·보존 보류", "현재 활성 보존 보류 (상태 총계 미합산)", "retention", "기간 내 등록 중 현재 활성·미만료 보류", "x.held_at",
        "FROM seal.legal_hold x JOIN seal.consent_request r USING(consent_request_id)",
        "AND x.active AND (x.expires_on IS NULL OR x.expires_on>=CURRENT_DATE)", "x.legal_hold_id", "'LEGAL_HOLD_ACTIVE'",
        "x.hold_reason_code,x.reason_text,x.expires_on,x.active,x.released_at",
        "hold_reason_code:보류 사유|reason_text:보류 상세 사유|expires_on:보류 만료일|active:활성 여부|released_at:해제일시");

    public final String sheet, title, domain, basis;
    private final String time, source, condition, id, status, fields;
    public final List<Column> columns;
    StatisticsExportSection(String sheet, String title, String domain, String basis, String time,
            String source, String condition, String id, String status, String fields, String columns) {
        this.sheet=sheet; this.title=title; this.domain=domain; this.basis=basis; this.time=time;
        this.source=source; this.condition=condition; this.id=id; this.status=status; this.fields=fields;
        this.columns=java.util.Arrays.stream(columns.split("\\|")).map(v -> {
            String[] p=v.split(":",2); return new Column(p[0],p[1]);
        }).toList();
    }
    public record Column(String key, String label) {}
    public String sql() {
        String org=this==RIGHTS ? "x.organization_unit_id" : "r.organization_unit_id";
        String recipient=this==RIGHTS ? "x.recipient_id" : "r.recipient_id";
        return """
            WITH RECURSIVE visible_scope(organization_unit_id,include_descendants) AS (
              SELECT scope_target_id,include_descendants FROM seal.user_scope
              WHERE admin_user_id=:actor AND scope_type='ORGANIZATION' AND active=TRUE
              UNION SELECT c.organization_unit_id,TRUE FROM seal.organization_unit c
              JOIN visible_scope p ON c.parent_id=p.organization_unit_id WHERE p.include_descendants=TRUE)
            SELECT %s id,%s occurred_at,%s status,r.request_code,
              o.organization_name,t.template_name,rc.affiliation_name,rc.job_title,
              rc.recipient_id,rc.organization_unit_id,rc.recipient_status,rc.encryption_key_id,
              rc.recipient_name_ciphertext,rc.recipient_email_ciphertext,%s
            %s
            JOIN (SELECT DISTINCT organization_unit_id FROM visible_scope) allowed ON allowed.organization_unit_id=%s
            JOIN seal.organization_unit o ON o.organization_unit_id=%s
            JOIN seal.recipient rc ON rc.recipient_id=%s
            LEFT JOIN seal.consent_template t ON t.consent_template_id=r.consent_template_id
            LEFT JOIN seal.consent_template_version tv ON tv.consent_template_version_id=r.consent_template_version_id
            LEFT JOIN seal.processing_activity pa ON pa.processing_activity_id=r.processing_activity_id
            LEFT JOIN seal.retention_policy_version rv ON rv.retention_policy_version_id=%s
            LEFT JOIN seal.retention_policy rp ON rp.retention_policy_id=rv.retention_policy_id
            WHERE %s>=:from AND %s<(CAST(:to AS date)+1) %s
            ORDER BY %s,%s
            """.formatted(id,time,status,fields,source,org,org,recipient,
                this==RETENTION ? "x.retention_policy_version_id" : "r.retention_policy_version_id",
                time,time,condition,time,id);
    }
}
