package kr.or.arte.seal.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Principal principal) {
        return principal == null ? "redirect:/login" : "redirect:/admin";
    }
}
