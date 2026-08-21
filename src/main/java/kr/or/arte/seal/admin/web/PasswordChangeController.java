package kr.or.arte.seal.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.or.arte.seal.admin.application.AdminPasswordChangeService;
import kr.or.arte.seal.admin.application.PasswordChangeResult;
import kr.or.arte.seal.admin.application.PasswordChangeStatus;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordChangeController {

    private final AdminPasswordChangeService passwordChangeService;

    public PasswordChangeController(AdminPasswordChangeService passwordChangeService) {
        this.passwordChangeService = passwordChangeService;
    }

    @GetMapping("/account/password/change")
    public String form(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        return "admin/password-change";
    }

    @PostMapping("/account/password/change")
    public String change(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String newPasswordConfirmation,
            HttpServletRequest request,
            Model model
    ) {
        PasswordChangeResult result = passwordChangeService.change(
                principal.adminUserId(),
                currentPassword,
                newPassword,
                newPasswordConfirmation
        );
        if (result.status() == PasswordChangeStatus.CHANGED) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return "redirect:/login?passwordChanged";
        }

        model.addAttribute("administrator", principal);
        model.addAttribute("errorMessage", messageFor(result.status()));
        return "admin/password-change";
    }

    private static String messageFor(PasswordChangeStatus status) {
        return switch (status) {
            case CURRENT_PASSWORD_INVALID -> "현재 비밀번호가 올바르지 않습니다.";
            case CONFIRMATION_MISMATCH -> "새 비밀번호와 확인 값이 일치하지 않습니다.";
            case POLICY_VIOLATION -> "새 비밀번호는 12~128자이며 로그인 아이디와 달라야 합니다.";
            case CONCURRENT_UPDATE -> "다른 요청에서 계정 정보가 변경되었습니다. 다시 시도해 주세요.";
            case ACCOUNT_NOT_FOUND -> "계정 정보를 확인할 수 없습니다. 다시 로그인해 주세요.";
            case CHANGED -> throw new IllegalArgumentException("changed status has no error message");
        };
    }
}
