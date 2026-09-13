package kr.or.arte.seal.policy.application.retention;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.policy.domain.retention.RetentionPolicyVersion;
import kr.or.arte.seal.policy.persistence.RetentionPolicyInsert;
import kr.or.arte.seal.policy.persistence.RetentionPolicyMapper;
import kr.or.arte.seal.policy.persistence.RetentionPolicyVersionInsert;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RetentionPolicyManagementService {
    private final RetentionPolicyMapper mapper;
    private final AuditEventWriter auditEventWriter;
    private final Clock clock;

    public RetentionPolicyManagementService(RetentionPolicyMapper mapper,
            AuditEventWriter auditEventWriter, Clock clock) {
        this.mapper = mapper;
        this.auditEventWriter = auditEventWriter;
        this.clock = clock;
    }

    @Transactional
    public UUID create(RetentionPolicyCreateCommand command) {
        validateName(command.policyName());
        if (!mapper.canAccessOrganization(command.actorId(), command.ownerOrganizationUnitId())) {
            throw notFound();
        }
        UUID policyId = UUID.randomUUID();
        String policyCode = generatePolicyCode(policyId);
        UUID versionId = UUID.randomUUID();
        RetentionPolicyVersion draft = RetentionPolicyVersion.draft(versionId, policyId, 1,
                command.startEvent(), command.periodValue(), command.periodUnit(),
                command.gracePeriodDays(), command.destructionMethod(), command.actorId());
        mapper.insertPolicy(new RetentionPolicyInsert(policyId, policyCode,
                command.policyName().strip(), command.ownerOrganizationUnitId(), command.actorId()));
        mapper.insertVersion(new RetentionPolicyVersionInsert(versionId, policyId, 1,
                draft.startEvent(), draft.periodValue(), draft.periodUnit(), draft.gracePeriodDays(),
                draft.destructionMethod(), command.actorId()));
        audit("RETENTION_POLICY_CREATE", policyId, command.actorId(), "SUCCESS");
        return policyId;
    }

    @Transactional
    public void requestReview(UUID policyId, UUID versionId, UUID actorId) {
        RetentionPolicyVersion requested = find(actorId, policyId, versionId).requestReview(actorId);
        updateTransition(requested, actorId);
        audit("RETENTION_POLICY_REVIEW_REQUEST", policyId, actorId, "SUCCESS");
    }

    @Transactional
    public void approve(UUID policyId, UUID versionId, UUID actorId) {
        RetentionPolicyVersion approved = find(actorId, policyId, versionId).approve(actorId);
        updateTransition(approved, actorId);
        mapper.supersedePreviousApprovedVersions(policyId, versionId, actorId);
        if (mapper.updateCurrentApprovedVersion(policyId, versionId, actorId) != 1) {
            throw conflict();
        }
        audit("RETENTION_POLICY_APPROVE", policyId, actorId, "SUCCESS");
    }

    @Transactional
    public void reject(UUID policyId, UUID versionId, UUID actorId, String comment) {
        RetentionPolicyVersion rejected = find(actorId, policyId, versionId).reject(actorId, comment);
        updateTransition(rejected, actorId);
        audit("RETENTION_POLICY_REJECT", policyId, actorId, "SUCCESS");
    }

    private RetentionPolicyVersion find(UUID actorId, UUID policyId, UUID versionId) {
        List<RetentionPolicyVersion> rows = mapper.findVisibleVersionForUpdate(actorId, policyId, versionId);
        if (rows.size() != 1) {
            throw notFound();
        }
        return rows.get(0);
    }

    private void updateTransition(RetentionPolicyVersion version, UUID actorId) {
        if (mapper.updateVersionTransition(version, actorId, clock.instant()) != 1) {
            throw conflict();
        }
    }

    private void audit(String action, UUID policyId, UUID actorId, String result) {
        Instant now = clock.instant();
        auditEventWriter.append(new AuditEventCommand(now, "ADMIN", actorId, action,
                "RETENTION_POLICY", policyId.toString(), result, null,
                UUID.randomUUID(), Map.of()));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) {
            throw new IllegalArgumentException("정책명은 1~100자로 입력해야 합니다.");
        }
    }

    private static String generatePolicyCode(UUID policyId) {
        return "RP-" + policyId.toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "다른 사용자가 먼저 변경했습니다.");
    }
}
