package kr.or.arte.seal.request.web;

import jakarta.validation.Valid;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.delivery.application.DeliveryIssueResult;
import kr.or.arte.seal.link.application.IssuedSecureLink;
import kr.or.arte.seal.link.application.SecureLinkManagementService;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.application.ConsentRequestManagement;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.request.application.CreateConsentRequestCommand;
import kr.or.arte.seal.request.application.UpdateConsentRequestCommand;
import kr.or.arte.seal.request.domain.ConsentRequestStatus;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.springframework.beans.factory.annotation.Value; import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Controller
public class ConsentRequestController {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final ConsentRequestManagement management;
    private final ConsentRequestQueryService query;
    private final SecureLinkManagementService links;
    private final ConsentDeliveryService delivery;
    private final RecipientQueryService recipients;
    private final PublishedConsentTemplateQuery templates; private final String publicBaseUrl;

    public ConsentRequestController(ConsentRequestManagement management, ConsentRequestQueryService query,
            SecureLinkManagementService links, ConsentDeliveryService delivery, RecipientQueryService recipients,
            PublishedConsentTemplateQuery templates, @Value("${seal.public-base-url}") String publicBaseUrl) {
        this.management=management; this.query=query; this.links=links; this.delivery=delivery;
        this.recipients=recipients; this.templates=templates; this.publicBaseUrl=normalizeBaseUrl(publicBaseUrl);
    }

    @GetMapping("/admin/requests") @PreAuthorize("hasAuthority('CONSENT_REQUEST_VIEW')")
    public String list(@RequestParam(required=false) ConsentRequestStatus status,
            @RequestParam(defaultValue="1") int page, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        base(principal, model); var pageResult=query.searchPage(principal.adminUserId(),status,page,20); model.addAttribute("requests",pageResult.items()); model.addAttribute("pageResult",pageResult);
        model.addAttribute("selectedStatus", status); return "admin/requests/list";
    }

    @GetMapping("/admin/requests/new") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("requestForm", new ConsentRequestForm()); populateOptions(principal, model);
        return "admin/requests/form";
    }

    @PostMapping("/admin/requests") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String create(@Valid @ModelAttribute("requestForm") ConsentRequestForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        java.time.Instant expires = null;
        try { expires = LocalDateTime.parse(form.getExpiresAt()).atZone(KOREA).toInstant(); }
        catch (RuntimeException exception) { result.rejectValue("expiresAt", "invalid", "올바른 만료일시를 입력해 주세요."); }
        if (result.hasErrors()) { populateOptions(principal, model); return "admin/requests/form"; }
        var recipient = recipients.find(principal.adminUserId(), form.getRecipientId());
        var created = management.create(principal.adminUserId(), new CreateConsentRequestCommand(
                recipient.organizationUnitId(), form.getRecipientId(), form.getTemplateVersionId(),
                expires, form.getIdempotencyKey()));
        return "redirect:/admin/requests/" + created.requestId() + (created.existing()?"?existing":"?created");
    }

    @GetMapping("/admin/requests/{requestId}/edit") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String editForm(@PathVariable UUID requestId, @AuthenticationPrincipal AdminPrincipal principal,
            Model model) {
        var request=query.require(principal.adminUserId(), requestId);
        if (request.status()!=ConsentRequestStatus.DRAFT) return redirect(requestId);
        ConsentRequestForm form=new ConsentRequestForm(); form.setRecipientId(request.recipientId());
        form.setTemplateVersionId(request.templateVersionId());
        form.setExpiresAt(LocalDateTime.ofInstant(request.expiresAt(),KOREA).toString());
        form.setVersionNo(request.versionNo()); model.addAttribute("requestForm",form);
        model.addAttribute("editing",true); model.addAttribute("requestId",requestId);
        populateOptions(principal,model); return "admin/requests/form";
    }

    @PostMapping("/admin/requests/{requestId}") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String update(@PathVariable UUID requestId,
            @Valid @ModelAttribute("requestForm") ConsentRequestForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        java.time.Instant expires=null;
        try { expires=LocalDateTime.parse(form.getExpiresAt()).atZone(KOREA).toInstant(); }
        catch(RuntimeException exception){result.rejectValue("expiresAt","invalid","올바른 만료일시를 입력해 주세요.");}
        if(result.hasErrors()){model.addAttribute("editing",true);model.addAttribute("requestId",requestId);populateOptions(principal,model);return "admin/requests/form";}
        management.updateDraft(principal.adminUserId(),requestId,new UpdateConsentRequestCommand(
                form.getRecipientId(),form.getTemplateVersionId(),expires,form.getVersionNo()));
        return redirect(requestId);
    }

    @GetMapping("/admin/requests/{requestId}") @PreAuthorize("hasAuthority('CONSENT_REQUEST_VIEW')")
    public String detail(@PathVariable UUID requestId, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        base(principal, model); var request=query.require(principal.adminUserId(), requestId);
        model.addAttribute("request", request);
        model.addAttribute("recipient", recipients.find(principal.adminUserId(), request.recipientId()));
        return "admin/requests/detail";
    }

    @PostMapping("/admin/requests/{requestId}/finalize") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String finalizeRequest(@PathVariable UUID requestId, @RequestParam long versionNo,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.finalizeRequest(principal.adminUserId(), requestId, versionNo); return redirect(requestId);
    }
    @GetMapping("/admin/requests/{requestId}/preview") @PreAuthorize("hasAuthority('CONSENT_REQUEST_VIEW')")
    public String preview(@PathVariable UUID requestId, @AuthenticationPrincipal AdminPrincipal principal,
            Model model) {
        base(principal, model); model.addAttribute("requestId", requestId);
        model.addAttribute("snapshotJson", query.requireSnapshotJson(principal.adminUserId(), requestId));
        return "admin/requests/preview";
    }
    @PostMapping("/admin/requests/{requestId}/cancel") @PreAuthorize("hasAuthority('CONSENT_REQUEST_CANCEL')")
    public String cancel(@PathVariable UUID requestId, @RequestParam long versionNo, @RequestParam String reason,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.cancel(principal.adminUserId(), requestId, versionNo, reason); return redirect(requestId);
    }
    @PostMapping("/admin/requests/{requestId}/activate") @PreAuthorize("hasAuthority('CONSENT_REQUEST_ACTIVATE')")
    public String activate(@PathVariable UUID requestId, @RequestParam long versionNo,
            @AuthenticationPrincipal AdminPrincipal principal, RedirectAttributes redirect) {
        flash(redirect, links.activate(principal.adminUserId(), requestId, versionNo));
        return "redirect:/admin/requests/"+requestId+"/issued-link";
    }
    @PostMapping("/admin/requests/{requestId}/reissue") @PreAuthorize("hasAuthority('CONSENT_REQUEST_LINK_REISSUE')")
    public String reissue(@PathVariable UUID requestId, @RequestParam long versionNo,
            @AuthenticationPrincipal AdminPrincipal principal, RedirectAttributes redirect) {
        flash(redirect, links.reissue(principal.adminUserId(), requestId, versionNo));
        return "redirect:/admin/requests/"+requestId+"/issued-link";
    }

    @PostMapping("/admin/requests/{requestId}/deliver")
    @PreAuthorize("hasAuthority('MAIL_SEND')")
    public String deliver(@PathVariable UUID requestId, @RequestParam long versionNo,
            @RequestParam(required = false) String scheduledAt,
            @AuthenticationPrincipal AdminPrincipal principal, RedirectAttributes redirect) {
        var result = delivery.activateAndQueue(principal.adminUserId(), requestId, versionNo,
                publicBaseUrl, parseSchedule(scheduledAt));
        flash(redirect, result);
        return "redirect:/admin/requests/" + requestId + "/issued-link";
    }

    @PostMapping("/admin/requests/{requestId}/redeliver")
    @PreAuthorize("hasAuthority('MAIL_SEND')")
    public String redeliver(@PathVariable UUID requestId, @RequestParam long versionNo,
            @RequestParam(required = false) String scheduledAt,
            @AuthenticationPrincipal AdminPrincipal principal, RedirectAttributes redirect) {
        var result = delivery.reissueAndQueue(principal.adminUserId(), requestId, versionNo,
                publicBaseUrl, parseSchedule(scheduledAt));
        flash(redirect, result);
        return "redirect:/admin/requests/" + requestId + "/issued-link";
    }
    @GetMapping("/admin/requests/{requestId}/issued-link") @PreAuthorize("hasAnyAuthority('CONSENT_REQUEST_ACTIVATE','CONSENT_REQUEST_LINK_REISSUE','MAIL_SEND')")
    public String issued(@PathVariable UUID requestId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        IssuedSecureLink issued = (IssuedSecureLink) model.asMap().get("issuedLink");
        if (issued == null || issued.token() == null) return redirect(requestId);
        base(principal, model); model.addAttribute("requestId", requestId); return "admin/requests/issued-link";
    }

    private void populateOptions(AdminPrincipal p, Model m) {
        base(p,m); m.addAttribute("recipients", recipients.search(p.adminUserId(), null, RecipientStatus.ACTIVE));
        m.addAttribute("templates", templates.findAllPublished(p.adminUserId()));
    }
    private static void base(AdminPrincipal p, Model m){m.addAttribute("administrator",p);}
    private static String redirect(UUID id){return "redirect:/admin/requests/"+id;}
    private static void flash(RedirectAttributes r, IssuedSecureLink link){r.addFlashAttribute("issuedLink",link);}
    private static void flash(RedirectAttributes r, DeliveryIssueResult result){
        r.addFlashAttribute("issuedLink", result.issuedLink());
        r.addFlashAttribute("outboxId", result.outboxId());
        r.addFlashAttribute("mailStatus", result.mailStatus());
    }
    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("공개 동의 기본 URL이 필요합니다.");
        String normalized = value.strip();
        java.net.URI uri = java.net.URI.create(normalized);
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null)
            throw new IllegalStateException("공개 동의 기본 URL 형식이 올바르지 않습니다.");
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
    private static java.time.Instant parseSchedule(String value){
        if(value==null||value.isBlank())return null;
        try{return LocalDateTime.parse(value).atZone(KOREA).toInstant();}
        catch(RuntimeException exception){throw new IllegalArgumentException("올바른 발송 예약일시를 입력해 주세요.");}
    }
}
