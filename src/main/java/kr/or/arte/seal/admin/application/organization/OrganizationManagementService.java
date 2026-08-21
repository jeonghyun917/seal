package kr.or.arte.seal.admin.application.organization;

import kr.or.arte.seal.admin.domain.organization.OrganizationPolicy;
import kr.or.arte.seal.admin.persistence.organization.OrganizationMapper;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationManagementService implements OrganizationManagement {
    private final OrganizationMapper mapper;
    private final AuditEventWriter auditEventWriter;
    private final Clock clock;

    public OrganizationManagementService(OrganizationMapper mapper,
            AuditEventWriter auditEventWriter, Clock clock) {
        this.mapper = mapper;
        this.auditEventWriter = auditEventWriter;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSummary> findVisibleOrganizations(UUID adminUserId) {
        return mapper.findVisibleOrganizations(adminUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDetail findVisibleOrganization(UUID adminUserId, UUID organizationUnitId) {
        return find(adminUserId, organizationUnitId);
    }

    @Override
    @Transactional
    public UUID create(OrganizationCreateCommand command) {
        OrganizationPolicy.validateCode(command.organizationCode());
        OrganizationPolicy.validateName(command.organizationName());
        if (command.parentId() == null || !mapper.canAccessOrganization(command.actorId(), command.parentId())) {
            throw notFound();
        }
        UUID organizationId = UUID.randomUUID();
        if (mapper.insert(new OrganizationInsert(organizationId, command.organizationCode(),
                command.organizationName().strip(), command.parentId())) != 1) {
            throw conflict();
        }
        audit("ORGANIZATION_CREATE", organizationId, command.actorId());
        return organizationId;
    }

    @Override
    @Transactional
    public void update(OrganizationUpdateCommand command) {
        OrganizationDetail current = find(command.actorId(), command.organizationUnitId());
        OrganizationPolicy.validateName(command.organizationName());
        OrganizationPolicy.validateChange(current.organizationUnitId(), current.rootUnit(),
                command.parentId(), command.active());
        if (!current.rootUnit()) {
            if (command.parentId() == null
                    || !mapper.canAccessOrganization(command.actorId(), command.parentId())) {
                throw notFound();
            }
            if (mapper.wouldCreateCycle(current.organizationUnitId(), command.parentId())) {
                throw new IllegalArgumentException("하위 조직을 상위 조직으로 지정할 수 없습니다.");
            }
        }
        if (current.active() && !command.active()
                && mapper.countActiveDependencies(current.organizationUnitId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "활성 상태인 하위 조직 또는 업무 데이터가 있어 비활성화할 수 없습니다.");
        }
        if (mapper.update(new OrganizationUpdate(current.organizationUnitId(),
                command.organizationName().strip(), command.parentId(), command.active(),
                command.versionNo())) != 1) {
            throw conflict();
        }
        audit("ORGANIZATION_UPDATE", current.organizationUnitId(), command.actorId());
    }

    private OrganizationDetail find(UUID actorId, UUID organizationId) {
        List<OrganizationDetail> rows = mapper.findVisibleOrganization(actorId, organizationId);
        if (rows.size() != 1) {
            throw notFound();
        }
        return rows.get(0);
    }

    private void audit(String actionCode, UUID organizationId, UUID actorId) {
        auditEventWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId,
                actionCode, "ORGANIZATION", organizationId.toString(), "SUCCESS", null,
                UUID.randomUUID(), Map.of()));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "다른 사용자가 먼저 변경했습니다.");
    }
}
