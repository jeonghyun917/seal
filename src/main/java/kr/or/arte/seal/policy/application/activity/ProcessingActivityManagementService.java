package kr.or.arte.seal.policy.application.activity;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.domain.activity.ProcessingActivityVersion;
import kr.or.arte.seal.policy.persistence.activity.ProcessingActivityInsert;
import kr.or.arte.seal.policy.persistence.activity.ProcessingActivityMapper;
import kr.or.arte.seal.policy.persistence.activity.ProcessingActivityVersionInsert;
import kr.or.arte.seal.policy.persistence.activity.ProcessingActivityVersionRow;
import kr.or.arte.seal.policy.persistence.activity.ProcessingDataItemInsert;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProcessingActivityManagementService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,30}$");
    private final ProcessingActivityMapper mapper;
    private final AuditEventWriter auditWriter;
    private final Clock clock;

    public ProcessingActivityManagementService(ProcessingActivityMapper mapper,
            AuditEventWriter auditWriter, Clock clock) {
        this.mapper = mapper;
        this.auditWriter = auditWriter;
        this.clock = clock;
    }

    @Transactional
    public UUID create(ProcessingActivityCreateCommand command) {
        validateIdentity(command.activityCode(), command.activityName());
        if (!mapper.canAccessActiveOrganization(command.actorId(), command.ownerOrganizationUnitId())
                || !mapper.canUseApprovedRetentionVersion(command.actorId(), command.retentionPolicyVersionId())) {
            throw notFound();
        }
        UUID activityId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ProcessingActivityVersion draft = ProcessingActivityVersion.draft(versionId, activityId, 1,
                command.processingPurpose(), command.legalBasisType(), command.legalBasisDetail(),
                command.retentionPolicyVersionId(), command.dataItems(), command.actorId());
        requireOne(mapper.insertActivity(new ProcessingActivityInsert(activityId,
                command.activityCode(), command.activityName().strip(),
                command.ownerOrganizationUnitId(), command.actorId())));
        requireOne(mapper.insertVersion(new ProcessingActivityVersionInsert(versionId, activityId, 1,
                draft.processingPurpose(), draft.legalBasisType(), draft.legalBasisDetail(),
                draft.retentionPolicyVersionId(), command.actorId())));
        List<ProcessingDataItemInsert> inserts = new ArrayList<>();
        for (int index = 0; index < draft.dataItems().size(); index++) {
            var item = draft.dataItems().get(index);
            inserts.add(new ProcessingDataItemInsert(UUID.randomUUID(), versionId, index + 1,
                    item.itemName(), item.necessityReason(), item.required()));
        }
        if (mapper.insertDataItems(inserts) != inserts.size()) throw conflict();
        audit("PROCESSING_ACTIVITY_CREATE", activityId, command.actorId());
        return activityId;
    }

    @Transactional
    public void requestReview(UUID activityId, UUID versionId, UUID actorId) {
        ProcessingActivityVersion requested = find(actorId, activityId, versionId).requestReview(actorId);
        updateTransition(requested, actorId);
        audit("PROCESSING_ACTIVITY_REVIEW_REQUEST", activityId, actorId);
    }

    @Transactional
    public void approve(UUID activityId, UUID versionId, UUID actorId) {
        ProcessingActivityVersion approved = find(actorId, activityId, versionId).approve(actorId);
        updateTransition(approved, actorId);
        mapper.supersedePreviousApprovedVersions(activityId, versionId, actorId);
        requireOne(mapper.updateCurrentApprovedVersion(activityId, versionId, actorId));
        audit("PROCESSING_ACTIVITY_APPROVE", activityId, actorId);
    }

    @Transactional
    public void reject(UUID activityId, UUID versionId, UUID actorId, String comment) {
        ProcessingActivityVersion rejected = find(actorId, activityId, versionId).reject(actorId, comment);
        updateTransition(rejected, actorId);
        audit("PROCESSING_ACTIVITY_REJECT", activityId, actorId);
    }

    private ProcessingActivityVersion find(UUID actorId, UUID activityId, UUID versionId) {
        List<ProcessingActivityVersionRow> rows = mapper.findVisibleVersionForUpdate(actorId, activityId, versionId);
        if (rows.size() != 1) throw notFound();
        ProcessingActivityVersionRow row = rows.get(0);
        return new ProcessingActivityVersion(row.processingActivityVersionId(), row.processingActivityId(),
                row.versionNumber(), row.workflowStatus(), row.processingPurpose(), row.legalBasisType(),
                row.legalBasisDetail(), row.retentionPolicyVersionId(), mapper.findDataItems(versionId),
                row.createdBy(), row.reviewRequestedBy(), row.approvedBy(), row.rejectionComment(), row.versionNo());
    }

    private void updateTransition(ProcessingActivityVersion version, UUID actorId) {
        requireOne(mapper.updateVersionTransition(version, actorId, clock.instant()));
    }

    private void audit(String action, UUID activityId, UUID actorId) {
        auditWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId, action,
                "PROCESSING_ACTIVITY", activityId.toString(), "SUCCESS", null,
                UUID.randomUUID(), Map.of()));
    }

    private static void validateIdentity(String code, String name) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("업무코드는 영문 대문자, 숫자, 하이픈 2~30자로 입력해야 합니다.");
        }
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("업무명은 1~100자로 입력해야 합니다.");
        }
    }

    private static void requireOne(int count) { if (count != 1) throw conflict(); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "다른 변경과 충돌했습니다. 최신 내용을 다시 불러온 뒤 재시도해 주세요.");
    }
}
