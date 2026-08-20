package kr.or.arte.seal.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("serviceName", "개인정보 전자동의·증적 관리시스템");
        return "home";
    }
}
