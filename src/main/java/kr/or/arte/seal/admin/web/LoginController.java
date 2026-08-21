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
            Model model
    ) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호를 확인해 주세요.");
        }
        if (logout != null) {
            model.addAttribute("noticeMessage", "안전하게 로그아웃했습니다.");
        }
        if (passwordChanged != null) {
            model.addAttribute("noticeMessage", "비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.");
        }
        return "admin/login";
    }
}
