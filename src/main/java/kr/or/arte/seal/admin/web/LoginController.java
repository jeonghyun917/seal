package kr.or.arte.seal.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String passwordChanged,
            @RequestParam(required = false) String expired,
            @RequestParam(required = false) String revoked,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호를 확인해 주세요.");
        }
        if (logout != null) {
            model.addAttribute("noticeMessage", "안전하게 로그아웃했습니다.");
        }
        if (expired != null) {
            model.addAttribute("errorMessage",
                    "로그인 페이지의 유효시간이 지났습니다. 새로 열린 화면에서 다시 로그인해 주세요.");
        }
        if (passwordChanged != null) {
            model.addAttribute("noticeMessage", "비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.");
        }
        if (revoked != null) {
            model.addAttribute("errorMessage",
                    "계정 또는 접근 권한이 변경되어 로그아웃되었습니다. 다시 로그인해 주세요.");
        }
        return "admin/login";
    }
}
