package kr.or.arte.seal.retention.web;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.recipient.application.RecipientDetail;
import kr.or.arte.seal.recipient.application.RecipientQueryService;
import kr.or.arte.seal.retention.application.RetentionJobService;
import kr.or.arte.seal.retention.domain.RetentionAnchorType;
import kr.or.arte.seal.retention.domain.RetentionJobStatus;
import kr.or.arte.seal.request.application.ConsentRequestQueryService;
import kr.or.arte.seal.template.application.PublishedConsentTemplateQuery;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping({"/admin/retention", "/admin/retention-jobs"})
public class RetentionJobController {
    private final RetentionJobService service;
    private final RecipientQueryService recipients;
    private final ConsentRequestQueryService consentRequests;
    private final PublishedConsentTemplateQuery templates;

    public RetentionJobController(RetentionJobService service, RecipientQueryService recipients,
            ConsentRequestQueryService consentRequests, PublishedConsentTemplateQuery templates) {
        this.service = service;
        this.recipients = recipients;
        this.consentRequests = consentRequests;
        this.templates = templates;
    }

    @GetMapping("/request-options")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    @org.springframework.web.bind.annotation.ResponseBody
    public kr.or.arte.seal.common.web.AdminPageResult<RequestOption> requestOptions(
            @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="1") int page,
            @AuthenticationPrincipal AdminPrincipal principal) {
        var result=consentRequests.searchPage(principal.adminUserId(),null,q,page,20);
        var items=result.items().stream().map(item->{
            var recipient=recipients.find(principal.adminUserId(),item.recipientId());
            var request=consentRequests.require(principal.adminUserId(),item.requestId());
            String template=templates.findVersionOption(principal.adminUserId(),request.templateVersionId())
                    .map(option->option.templateName()).orElse("동의서 정보 확인 불가");
            return new RequestOption(item.requestId(),item.requestCode(),recipientLabel(recipient),template);
        }).toList();
        return new kr.or.arte.seal.common.web.AdminPageResult<>(items,result.page(),result.size(),result.hasNext());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RETENTION_VIEW')")
    public String list(@RequestParam(required = false) RetentionJobStatus status,
                       @RequestParam(defaultValue = "1") int page, @AuthenticationPrincipal AdminPrincipal principal,
                       Model model) {
        model.addAttribute("administrator", principal);
        var pageResult=service.searchPage(principal.adminUserId(),status,page,20);
        Map<UUID,String> recipientLabels=new LinkedHashMap<>();
        for(var item:pageResult.items()) recipientLabels.computeIfAbsent(item.recipientId(),
                id->recipientLabel(recipients.find(principal.adminUserId(),id)));
        model.addAttribute("items",pageResult.items()); model.addAttribute("pageResult",pageResult);
        model.addAttribute("recipientLabels",recipientLabels);
        var activeHolds = service.activeLegalHolds(principal.adminUserId());
        Map<UUID,String> holdRequestLabels = new LinkedHashMap<>();
        for (var hold : activeHolds) {
            holdRequestLabels.computeIfAbsent(hold.consentRequestId(),
                    requestId -> consentRequests.require(principal.adminUserId(), requestId).requestCode());
        }
        model.addAttribute("activeHolds", activeHolds);
        model.addAttribute("holdRequestLabels", holdRequestLabels);
        model.addAttribute("selectedStatus", status);
        return "admin/retention/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RETENTION_VIEW')")
    public String detail(@PathVariable UUID id,
                         @AuthenticationPrincipal AdminPrincipal principal,
                         Model model) {
        model.addAttribute("administrator", principal);
        var item=service.require(principal.adminUserId(), id);
        model.addAttribute("item", item);
        model.addAttribute("recipient", recipients.find(principal.adminUserId(),item.recipientId()));
        var consentRequest=consentRequests.require(principal.adminUserId(),item.consentRequestId());
        model.addAttribute("consentRequest", consentRequest);
        templates.findVersionOption(principal.adminUserId(),consentRequest.templateVersionId())
                .ifPresent(template->model.addAttribute("requestTemplate",template));
        model.addAttribute("actorNames", service.actorNames(principal.adminUserId(), id));
        return "admin/retention/detail";
    }

    @PostMapping("/anchors")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String recordAnchor(@RequestParam UUID consentRequestId,
                               @RequestParam RetentionAnchorType anchorType,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate,
                               @AuthenticationPrincipal AdminPrincipal principal) {
        service.recordAnchor(principal.adminUserId(), consentRequestId, anchorType, anchorDate);
        return "redirect:/admin/retention";
    }

    @PostMapping("/holds")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String placeHold(@RequestParam UUID consentRequestId,
                            @RequestParam String reasonCode,
                            @RequestParam String reasonText,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                            @AuthenticationPrincipal AdminPrincipal principal) {
        service.placeLegalHold(principal.adminUserId(), consentRequestId, reasonCode, reasonText, expiresOn);
        return "redirect:/admin/retention";
    }

    @PostMapping("/holds/{holdId}/release")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String releaseHold(@PathVariable UUID holdId,
                              @AuthenticationPrincipal AdminPrincipal principal) {
        service.releaseLegalHold(principal.adminUserId(), holdId);
        return "redirect:/admin/retention";
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('RETENTION_MANAGE')")
    public String submit(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.submit(principal.adminUserId(), id);
        return back(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RETENTION_APPROVE')")
    public String approve(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.approve(principal.adminUserId(), id);
        return back(id);
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('RETENTION_EXECUTE')")
    public String execute(@PathVariable UUID id, @RequestParam UUID idempotencyKey,
                          @AuthenticationPrincipal AdminPrincipal principal) {
        service.execute(principal.adminUserId(), id, idempotencyKey);
        return back(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('RETENTION_CONFIRM')")
    public String confirm(@PathVariable UUID id, @AuthenticationPrincipal AdminPrincipal principal) {
        service.confirm(principal.adminUserId(), id);
        return back(id);
    }

    private static String back(UUID id) {
        return "redirect:/admin/retention/" + id;
    }

    private static String recipientLabel(RecipientDetail recipient) {
        if (recipient.affiliationName()==null || recipient.affiliationName().isBlank()) {
            return recipient.maskedName();
        }
        return recipient.maskedName()+" · "+recipient.affiliationName();
    }

    public record RequestOption(UUID requestId,String requestCode,String recipientLabel,String templateName) { }
}
