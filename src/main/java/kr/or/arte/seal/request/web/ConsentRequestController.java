package kr.or.arte.seal.request.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.delivery.application.ConsentDeliveryService;
import kr.or.arte.seal.delivery.application.DeliveryIssueResult;
import kr.or.arte.seal.link.application.IssuedSecureLink;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import kr.or.arte.seal.request.application.ConsentRequestManagement;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.request.application.CreateConsentRequestCommand;
import kr.or.arte.seal.request.application.CreateConsentRequestBatchCommand;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;

@Controller
public class ConsentRequestController {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final ConsentRequestManagement management;
    private final ConsentRequestQueryService query;
    private final ConsentDeliveryService delivery;
    private final RecipientQueryService recipients;
    private final PublishedConsentTemplateQuery templates; private final String publicBaseUrl;
    private final ConsentRequestSnapshotPresenter snapshotPresenter;

    public ConsentRequestController(ConsentRequestManagement management, ConsentRequestQueryService query,
            ConsentDeliveryService delivery, RecipientQueryService recipients,
            PublishedConsentTemplateQuery templates, ObjectMapper objectMapper,
            @Value("${seal.public-base-url}") String publicBaseUrl) {
        this.management=management; this.query=query; this.delivery=delivery;
        this.recipients=recipients; this.templates=templates; this.publicBaseUrl=normalizeBaseUrl(publicBaseUrl);
        this.snapshotPresenter = new ConsentRequestSnapshotPresenter(objectMapper);
    }

    @GetMapping("/admin/requests") @PreAuthorize("hasAuthority('CONSENT_REQUEST_VIEW')")
    public String list(@RequestParam(required=false) ConsentRequestStatus status,
            @RequestParam(required=false) String q, @RequestParam(defaultValue="1") int page,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        base(principal, model); var pageResult=query.searchPage(principal.adminUserId(),status,q,page,20);
        var recipientDetails=recipients.findMany(principal.adminUserId(),pageResult.items().stream().map(item->item.recipientId()).toList());
        var templateOptions=templates.findVersionOptions(principal.adminUserId(),pageResult.items().stream().map(item->item.templateVersionId()).toList());
        var items=pageResult.items().stream().map(item->new RequestListItem(item,
                recipientDetails.get(item.recipientId()),templateOptions.get(item.templateVersionId()))).toList();
        model.addAttribute("requests",items); model.addAttribute("pageResult",pageResult);
        model.addAttribute("selectedStatus", status); model.addAttribute("q", q);
        model.addAttribute("statusOptions", ConsentRequestStatus.values()); return "admin/requests/list";
    }

    @GetMapping("/admin/requests/new") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String createForm(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("requestForm", new ConsentRequestForm()); populateOptions(principal, model, false);
        return "admin/requests/form";
    }

    @GetMapping("/admin/requests/recipient-options")
    @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    @ResponseBody
    public RecipientOptionsResponse recipientOptions(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="false") boolean all,
            @AuthenticationPrincipal AdminPrincipal principal) {
        if (all) {
            var matches = recipients.searchPicker(principal.adminUserId(), keyword);
            List<RecipientOption> items = matches.stream().limit(1000).map(RecipientOption::from).toList();
            return new RecipientOptionsResponse(items, 1, matches.size() > 1000);
        }
        var result = recipients.searchPickerPage(principal.adminUserId(), keyword, page, 20);
        return new RecipientOptionsResponse(result.items().stream().map(RecipientOption::from).toList(),
                result.page(), result.hasNext());
    }

    @PostMapping("/admin/requests") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String create(@Valid @ModelAttribute("requestForm") ConsentRequestForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model, RedirectAttributes redirect) {
        java.time.Instant expires = parseExpiry(form, result);
        if (form.getRecipientIds() == null || form.getRecipientIds().isEmpty()) {
            result.rejectValue("recipientIds", "required", "동의 수신자를 한 명 이상 선택해 주세요.");
        }
        if (result.hasErrors()) { populateOptions(principal, model, false); populateSelected(form, principal, model); return "admin/requests/form"; }
        var created = management.createBatch(principal.adminUserId(), new CreateConsentRequestBatchCommand(
                form.getRecipientIds(), form.getTemplateVersionId(), expires, form.getIdempotencyKey()));
        if (created.size() == 1) {
            var request = created.get(0);
            return "redirect:/admin/requests/" + request.requestId() + (request.existing()?"?existing":"?created");
        }
        redirect.addFlashAttribute("notice", created.size() + "명의 수신자에 대한 동의 요청 초안을 생성했습니다.");
        return "redirect:/admin/requests";
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
        populateOptions(principal,model,true); return "admin/requests/form";
    }

    @PostMapping("/admin/requests/{requestId}") @PreAuthorize("hasAuthority('CONSENT_REQUEST_EDIT')")
    public String update(@PathVariable UUID requestId,
            @Valid @ModelAttribute("requestForm") ConsentRequestForm form, BindingResult result,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        java.time.Instant expires = parseExpiry(form, result);
        if (form.getRecipientId() == null) result.rejectValue("recipientId", "required", "동의 수신자를 선택해 주세요.");
        if(result.hasErrors()){model.addAttribute("editing",true);model.addAttribute("requestId",requestId);populateOptions(principal,model,true);return "admin/requests/form";}
        management.updateDraft(principal.adminUserId(),requestId,new UpdateConsentRequestCommand(
                form.getRecipientId(),form.getTemplateVersionId(),expires,form.getVersionNo()));
        return redirect(requestId);
    }

    @GetMapping("/admin/requests/{requestId}") @PreAuthorize("hasAuthority('CONSENT_REQUEST_VIEW')")
    public String detail(@PathVariable UUID requestId, @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        base(principal, model); var request=query.require(principal.adminUserId(), requestId);
        model.addAttribute("request", request);
        model.addAttribute("recipient", recipients.find(principal.adminUserId(), request.recipientId()));
        templates.findVersionOption(principal.adminUserId(), request.templateVersionId())
                .ifPresent(option -> model.addAttribute("requestTemplate", option));
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
        String snapshotJson = query.requireSnapshotJson(principal.adminUserId(), requestId);
        model.addAttribute("snapshotJson", snapshotJson);
        model.addAttribute("snapshotView", snapshotPresenter.present(snapshotJson));
        return "admin/requests/preview";
    }
    @PostMapping("/admin/requests/{requestId}/cancel") @PreAuthorize("hasAuthority('CONSENT_REQUEST_CANCEL')")
    public String cancel(@PathVariable UUID requestId, @RequestParam long versionNo, @RequestParam String reason,
            @AuthenticationPrincipal AdminPrincipal principal) {
        management.cancel(principal.adminUserId(), requestId, versionNo, reason); return redirect(requestId);
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

    private void populateOptions(AdminPrincipal p, Model m, boolean includeRecipients) {
        base(p,m);
        if (includeRecipients) m.addAttribute("recipients", recipients.search(p.adminUserId(), null, RecipientStatus.ACTIVE));
        m.addAttribute("templates", templates.findAllPublished(p.adminUserId()));
    }
    private void populateSelected(ConsentRequestForm form, AdminPrincipal principal, Model model) {
        if (form.getRecipientIds() == null) return;
        model.addAttribute("selectedRecipients", form.getRecipientIds().stream().distinct().map(id -> {
            try { return recipients.find(principal.adminUserId(), id); }
            catch (RuntimeException ignored) { return null; }
        }).filter(java.util.Objects::nonNull).toList());
    }
    private java.time.Instant parseExpiry(ConsentRequestForm form, BindingResult result) {
        java.time.Instant expires;
        try {
            expires = LocalDateTime.parse(form.getExpiresAt()).atZone(KOREA).toInstant();
        } catch (RuntimeException exception) {
            result.rejectValue("expiresAt", "invalid", "올바른 만료일시를 입력해 주세요.");
            return null;
        }
        if (!expires.isAfter(java.time.Instant.now())) {
            result.rejectValue("expiresAt", "future", "응답 만료일시는 현재 이후로 설정해 주세요.");
            return null;
        }
        return expires;
    }
    public record RecipientOptionsResponse(List<RecipientOption> items, int page, boolean hasNext) { }
    public record RequestListItem(kr.or.arte.seal.request.application.ConsentRequestSummary summary,
            kr.or.arte.seal.recipient.application.RecipientDetail recipient,
            kr.or.arte.seal.template.application.PublishedConsentTemplateOption template) { }
    public record RecipientOption(UUID recipientId, String affiliationName, String jobTitle,
            String maskedName, String maskedEmail, String collectionBasis) {
        static RecipientOption from(kr.or.arte.seal.recipient.application.RecipientDetail detail) {
            return new RecipientOption(detail.recipientId(), detail.affiliationName(), detail.jobTitle(),
                    detail.maskedName(), detail.maskedEmail(), detail.collectionBasis());
        }
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
        try{return LocalDateTime.parse(value.strip().replace(' ', 'T')).atZone(KOREA).toInstant();}
        catch(RuntimeException exception){throw new IllegalArgumentException("올바른 발송 예약일시를 입력해 주세요.");}
    }
}
