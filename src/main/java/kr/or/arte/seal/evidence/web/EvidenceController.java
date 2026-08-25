package kr.or.arte.seal.evidence.web;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.evidence.application.*;
import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Controller
@RequestMapping({"/admin/evidence","/admin/evidence-packages"})
public class EvidenceController {
    private final EvidenceQueryService service;public EvidenceController(EvidenceQueryService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('EVIDENCE_VIEW')") public String list(@RequestParam(required=false)EvidenceGenerationStatus status,@RequestParam(defaultValue="1")int page,@AuthenticationPrincipal AdminPrincipal p,Model m){m.addAttribute("administrator",p);var result=service.searchPage(p.adminUserId(),status,page,20);m.addAttribute("items",result.items());m.addAttribute("pageResult",result);m.addAttribute("selectedStatus",status);return "admin/evidence/list";}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('EVIDENCE_VIEW')") public String detail(@PathVariable UUID id,@AuthenticationPrincipal AdminPrincipal p,Model m){m.addAttribute("administrator",p);m.addAttribute("item",service.require(p.adminUserId(),id));return "admin/evidence/detail";}
    @PostMapping("/{id}/retry") @PreAuthorize("hasAuthority('EVIDENCE_GENERATE')") public String retry(@PathVariable UUID id,@AuthenticationPrincipal AdminPrincipal p){service.requeue(p.adminUserId(),id);return "redirect:/admin/evidence/"+id;}
    @PostMapping("/{id}/verify") @PreAuthorize("hasAuthority('EVIDENCE_VERIFY')") public String verify(@PathVariable UUID id,@AuthenticationPrincipal AdminPrincipal p,@RequestParam(defaultValue="false")boolean fromDownload,Model m){m.addAttribute("verificationResult",service.verify(p.adminUserId(),id));m.addAttribute("administrator",p);m.addAttribute("item",service.require(p.adminUserId(),id));return "admin/evidence/detail";}
    @GetMapping("/{id}/download") @PreAuthorize("hasAuthority('EVIDENCE_DOWNLOAD')") public ResponseEntity<byte[]> download(@PathVariable UUID id,@AuthenticationPrincipal AdminPrincipal p){try{return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=consent-evidence-"+id+".zip").contentType(MediaType.APPLICATION_OCTET_STREAM).body(service.download(p.adminUserId(),id));}catch(IllegalStateException e){throw new ResponseStatusException(HttpStatus.CONFLICT,e.getMessage());}}
}
