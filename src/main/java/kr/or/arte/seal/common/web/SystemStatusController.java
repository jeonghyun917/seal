package kr.or.arte.seal.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SystemStatusController {

    @GetMapping("/system/status")
    public String status(Model model) {
        model.addAttribute("runtimeVersion", "eGovFrame Runtime 5.0.0");
        model.addAttribute("javaVersion", "Java 17");
        model.addAttribute("databaseVersion", "PostgreSQL 16.14");
        model.addAttribute("viewTechnology", "JSP / JSTL");
        model.addAttribute("deliveryScope", "이메일 + 일회용 보안 링크");
        model.addAttribute("excludedScope", "전자서명·OTP 제외");
        return "system/status";
    }
}
