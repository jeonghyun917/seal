package kr.or.arte.seal.consent.web;

import jakarta.servlet.http.*;import kr.or.arte.seal.consent.application.*;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller public class PublicConsentController{
 private static final String FLOW="SEAL_PUBLIC_FLOW";private static final String DECISIONS="SEAL_PUBLIC_DECISIONS";private static final String IDEM="SEAL_PUBLIC_IDEMPOTENCY";
 private final PublicConsentEntryService entry;private final PublicConsentQueryService query;private final ConsentSubmissionService submissions;
 public PublicConsentController(PublicConsentEntryService e,PublicConsentQueryService q,ConsentSubmissionService s){entry=e;query=q;submissions=s;}
 @GetMapping("/consent/{token}") public String entry(@PathVariable String token,HttpServletResponse response,Model model){headers(response);model.addAttribute("token",token);model.addAttribute("entry",entry.inspect(token));return "consent/entry";}
 @PostMapping("/consent/{token}/start") public String start(@PathVariable String token,HttpSession session){var flow=entry.start(token);session.setAttribute(FLOW,flow.flowId());return "redirect:/consent/respond/"+flow.flowId();}
 @GetMapping("/consent/respond/{flow}") public String respond(@PathVariable String flow,HttpSession session,HttpServletResponse response,Model model){requireFlow(flow,session);headers(response);model.addAttribute("document",query.load(flow));model.addAttribute("flow",flow);return "consent/respond";}
 @PostMapping("/consent/respond/{flow}") public String decisions(@PathVariable String flow,@RequestParam Map<String,String> parameters,HttpSession session){requireFlow(flow,session);var page=query.load(flow);Map<UUID,ConsentDecision> values=new LinkedHashMap<>();for(var item:page.items()){String raw=parameters.get("decision_"+item.itemId());if(raw!=null)try{values.put(item.itemId(),ConsentDecision.valueOf(raw));}catch(IllegalArgumentException ignored){}}
  ConsentDecisionValidator.validate(page.items().stream().map(PublicConsentItem::itemId).toList(),values);session.setAttribute(DECISIONS,values);session.setAttribute(IDEM,UUID.randomUUID());return "redirect:/consent/respond/"+flow+"/confirm";}
 @GetMapping("/consent/respond/{flow}/confirm") public String confirm(@PathVariable String flow,HttpSession session,HttpServletResponse response,Model model){requireFlow(flow,session);headers(response);model.addAttribute("document",query.load(flow));model.addAttribute("decisions",requiredDecisions(session));model.addAttribute("flow",flow);return "consent/confirm";}
 @PostMapping("/consent/respond/{flow}/submit") public String submit(@PathVariable String flow,HttpSession session){requireFlow(flow,session);var result=submissions.submit(flow,requiredDecisions(session),(UUID)session.getAttribute(IDEM));session.setAttribute("SEAL_PUBLIC_RESULT",result);return "redirect:/consent/respond/"+flow+"/complete";}
 @GetMapping("/consent/respond/{flow}/complete") public String complete(@PathVariable String flow,HttpSession session,HttpServletResponse response,Model model){requireFlow(flow,session);headers(response);Object result=session.getAttribute("SEAL_PUBLIC_RESULT");if(result==null)throw new IllegalArgumentException("제출 완료 결과를 찾을 수 없습니다.");model.addAttribute("result",result);return "consent/complete";}
 @SuppressWarnings("unchecked") private static Map<UUID,ConsentDecision> requiredDecisions(HttpSession s){Object v=s.getAttribute(DECISIONS);if(!(v instanceof Map))throw new IllegalArgumentException("동의 항목을 다시 선택해 주세요.");return (Map<UUID,ConsentDecision>)v;}
 private static void requireFlow(String flow,HttpSession session){if(!Objects.equals(flow,session.getAttribute(FLOW)))throw new IllegalArgumentException("동의 세션을 찾을 수 없습니다.");}
 private static void headers(HttpServletResponse r){r.setHeader("Cache-Control","no-store");r.setHeader("Referrer-Policy","no-referrer");r.setHeader("X-Content-Type-Options","nosniff");}
}
