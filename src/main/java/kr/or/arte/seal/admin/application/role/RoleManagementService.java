package kr.or.arte.seal.admin.application.role;

import kr.or.arte.seal.admin.persistence.role.RoleManagementMapper;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RoleManagementService implements RoleManagement {
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,49}$");
    private final RoleManagementMapper mapper;
    private final AuditEventWriter auditWriter;
    private final Clock clock;
    public RoleManagementService(RoleManagementMapper mapper, AuditEventWriter auditWriter, Clock clock) {
        this.mapper = mapper; this.auditWriter = auditWriter; this.clock = clock;
    }
    @Override
    @Transactional(readOnly = true)
    public RoleDetail findRole(UUID roleId) {
        List<RoleDetail> rows = mapper.findRole(roleId);
        if (rows.size() != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return rows.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionSummary> findPermissions() {
        return mapper.findPermissions();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findAssignedPermissionIds(UUID roleId) {
        findRole(roleId);
        return mapper.findAssignedPermissionIds(roleId);
    }

    @Override
    @Transactional
    public UUID create(RoleCreateCommand command) {
        validate(command.roleCode(), command.roleName(), command.permissionIds());
        if (mapper.countPermissions(command.permissionIds()) != command.permissionIds().size()) {
            throw new IllegalArgumentException("존재하는 권한을 선택해야 합니다.");
        }
        UUID id = UUID.randomUUID();
        if (mapper.insert(new RoleInsert(id, command.roleCode(), command.roleName().strip())) != 1) throw conflict();
        mapper.replacePermissions(id, command.permissionIds(), command.actorId());
        audit("ROLE_CREATE", id, command.actorId());
        return id;
    }
    @Override
    @Transactional
    public void update(RoleUpdateCommand command) {
        RoleDetail role = findRole(command.roleId());
        validateName(command.roleName());
        if (command.permissionIds() == null || command.permissionIds().isEmpty()
                || mapper.countPermissions(command.permissionIds()) != command.permissionIds().size()) {
            throw new IllegalArgumentException("권한을 하나 이상 선택해야 합니다.");
        }
        if ("SYSTEM_ADMIN".equals(role.roleCode())
                && !command.permissionIds().containsAll(mapper.findRequiredSystemAdminPermissionIds())) {
            throw conflict();
        }
        if (role.active() && !command.active() && mapper.countActiveAssignedUsers(role.roleId()) > 0) {
            throw conflict();
        }
        if (mapper.update(new RoleUpdate(role.roleId(), command.roleName().strip(),
                command.active(), command.versionNo())) != 1) throw conflict();
        mapper.replacePermissions(role.roleId(), command.permissionIds(), command.actorId());
        audit("ROLE_UPDATE", role.roleId(), command.actorId());
    }
    private static void validate(String code, String name, Set<UUID> permissions) {
        if (code == null || !CODE.matcher(code).matches()) throw new IllegalArgumentException("역할코드 형식이 올바르지 않습니다.");
        validateName(name);
        if (permissions == null || permissions.isEmpty()) throw new IllegalArgumentException("권한을 하나 이상 선택해야 합니다.");
    }
    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 100) throw new IllegalArgumentException("역할명은 1~100자로 입력해야 합니다.");
    }
    private void audit(String action, UUID id, UUID actor) {
        auditWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actor, action,
                "ADMIN_ROLE", id.toString(), "SUCCESS", null, UUID.randomUUID(), Map.of()));
    }
    private static ResponseStatusException conflict() { return new ResponseStatusException(HttpStatus.CONFLICT); }
}
