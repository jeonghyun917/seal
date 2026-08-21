package kr.or.arte.seal.admin.application.user;

import kr.or.arte.seal.admin.domain.PasswordPolicy;
import kr.or.arte.seal.admin.persistence.user.AdminUserManagementMapper;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AdminUserManagementService implements AdminUserManagement {
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-z0-9.-]{4,50}$");
    private static final Set<String> ACCOUNT_STATUSES = Set.of("ACTIVE", "LOCKED", "INACTIVE");
    private final AdminUserManagementMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditEventWriter auditEventWriter;
    private final Clock clock;

    public AdminUserManagementService(AdminUserManagementMapper mapper,
            PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
            AuditEventWriter auditEventWriter, Clock clock) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.auditEventWriter = auditEventWriter;
        this.clock = clock;
    }

    @Override @Transactional(readOnly = true)
    public List<AdminUserSummary> findVisibleUsers(UUID adminUserId) {
        return mapper.findVisibleUsers(adminUserId);
    }

    @Override @Transactional(readOnly = true)
    public AdminUserDetail findVisibleUser(UUID actorId, UUID adminUserId) {
        return find(actorId, adminUserId);
    }

    @Override @Transactional(readOnly = true)
    public List<UUID> findAssignedRoleIds(UUID actorId, UUID adminUserId) {
        find(actorId, adminUserId);
        return mapper.findAssignedRoleIds(adminUserId);
    }

    @Override @Transactional(readOnly = true)
    public List<UUID> findAssignedOrganizationScopeIds(UUID actorId, UUID adminUserId) {
        find(actorId, adminUserId);
        return mapper.findAssignedOrganizationScopeIds(adminUserId);
    }

    @Override @Transactional
    public UUID create(AdminUserCreateCommand command) {
        validateIdentity(command.loginId(), command.displayName());
        validatePassword(command.loginId(), command.temporaryPassword());
        validateAssignments(command.actorId(), command.organizationUnitId(),
                command.roleIds(), command.organizationScopeIds());
        UUID userId = UUID.randomUUID();
        if (mapper.insert(new AdminUserInsert(userId, command.loginId(), command.displayName().strip(),
                command.organizationUnitId(), passwordEncoder.encode(command.temporaryPassword()),
                command.actorId())) != 1) {
            throw conflict();
        }
        mapper.replaceRoles(userId, command.roleIds(), command.actorId());
        mapper.replaceOrganizationScopes(userId, command.organizationScopeIds(), command.actorId());
        audit("ADMIN_USER_CREATE", userId, command.actorId());
        return userId;
    }

    @Override @Transactional
    public void update(AdminUserUpdateCommand command) {
        AdminUserDetail current = find(command.actorId(), command.adminUserId());
        validateDisplayName(command.displayName());
        if (!ACCOUNT_STATUSES.contains(command.accountStatus())) {
            throw new IllegalArgumentException("지원하지 않는 계정 상태입니다.");
        }
        if (command.actorId().equals(command.adminUserId()) && "INACTIVE".equals(command.accountStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "자기 계정은 비활성화할 수 없습니다.");
        }
        validateAssignments(command.actorId(), command.organizationUnitId(),
                command.roleIds(), command.organizationScopeIds());
        if (mapper.hasSystemAdminRole(current.adminUserId())
                && (!mapper.includesSystemAdminRole(command.roleIds())
                    || "INACTIVE".equals(command.accountStatus()))
                && mapper.countActiveSystemAdministrators() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "마지막 시스템 관리자 역할은 회수하거나 비활성화할 수 없습니다.");
        }
        if (mapper.update(new AdminUserUpdate(current.adminUserId(), command.displayName().strip(),
                command.organizationUnitId(), command.accountStatus(), command.versionNo(),
                command.actorId())) != 1) {
            throw conflict();
        }
        mapper.replaceRoles(current.adminUserId(), command.roleIds(), command.actorId());
        mapper.replaceOrganizationScopes(current.adminUserId(), command.organizationScopeIds(), command.actorId());
        audit("ADMIN_USER_UPDATE", current.adminUserId(), command.actorId());
    }

    @Override @Transactional
    public void unlock(UUID adminUserId, UUID actorId) {
        AdminUserDetail current = find(actorId, adminUserId);
        if (mapper.unlock(adminUserId, current.versionNo(), actorId) != 1) {
            throw conflict();
        }
        audit("ADMIN_USER_UNLOCK", adminUserId, actorId);
    }

    @Override @Transactional
    public void resetPassword(UUID adminUserId, String temporaryPassword, UUID actorId) {
        AdminUserDetail current = find(actorId, adminUserId);
        validatePassword(current.loginId(), temporaryPassword);
        if (mapper.resetPassword(adminUserId, passwordEncoder.encode(temporaryPassword),
                current.versionNo(), actorId) != 1) {
            throw conflict();
        }
        audit("ADMIN_USER_RESET_PASSWORD", adminUserId, actorId);
    }

    private void validateAssignments(UUID actorId, UUID organizationId,
            Set<UUID> roleIds, Set<UUID> scopeIds) {
        if (organizationId == null || !mapper.canAccessOrganization(actorId, organizationId)) {
            throw notFound();
        }
        if (roleIds == null || roleIds.isEmpty() || mapper.countActiveRoles(roleIds) != roleIds.size()) {
            throw new IllegalArgumentException("활성 역할을 하나 이상 선택해야 합니다.");
        }
        if (scopeIds == null || scopeIds.isEmpty()
                || scopeIds.stream().anyMatch(scopeId -> !mapper.canAccessOrganization(actorId, scopeId))) {
            throw notFound();
        }
    }

    private AdminUserDetail find(UUID actorId, UUID userId) {
        List<AdminUserDetail> rows = mapper.findVisibleUser(actorId, userId);
        if (rows.size() != 1) throw notFound();
        return rows.get(0);
    }

    private void validatePassword(String loginId, String password) {
        if (password == null || !passwordPolicy.validate(loginId, password).valid()) {
            throw new IllegalArgumentException("임시 비밀번호가 보안 정책을 충족하지 않습니다.");
        }
    }

    private static void validateIdentity(String loginId, String displayName) {
        if (loginId == null || !LOGIN_PATTERN.matcher(loginId).matches()) {
            throw new IllegalArgumentException("로그인 ID 형식이 올바르지 않습니다.");
        }
        validateDisplayName(displayName);
    }

    private static void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > 100) {
            throw new IllegalArgumentException("관리자 이름은 1~100자로 입력해야 합니다.");
        }
    }

    private void audit(String action, UUID userId, UUID actorId) {
        auditEventWriter.append(new AuditEventCommand(clock.instant(), "ADMIN", actorId, action,
                "ADMIN_USER", userId.toString(), "SUCCESS", null, UUID.randomUUID(), Map.of()));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "다른 사용자가 먼저 변경했습니다.");
    }
}
