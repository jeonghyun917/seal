package kr.or.arte.seal.recipient.importing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.recipient.application.RecipientInstitution;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RecipientImportController {
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final RecipientImportService service;
    private final ClamAvProperties clamAv;

    public RecipientImportController(RecipientImportService service, ClamAvProperties clamAv) {
        this.service = service;
        this.clamAv = clamAv;
    }

    @GetMapping("/admin/recipients/import")
    @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String form(@AuthenticationPrincipal AdminPrincipal principal, Model model) {
        base(principal, model);
        return "admin/recipients/import-form";
    }

    @PostMapping("/admin/recipients/import")
    @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String upload(@RequestParam MultipartFile file,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) throws IOException {
        try {
            var preview = service.validate(principal.adminUserId(),
                    RecipientInstitution.ORGANIZATION_UNIT_ID, file.getBytes());
            return "redirect:/admin/recipients/import/" + preview.batchId();
        } catch (IllegalArgumentException | RecipientWorkbookException exception) {
            base(principal, model);
            model.addAttribute("error", exception.getMessage());
            return "admin/recipients/import-form";
        }
    }

    @GetMapping("/admin/recipients/import/{batchId}")
    @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String preview(@PathVariable UUID batchId,
            @AuthenticationPrincipal AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("preview", service.preview(principal.adminUserId(), batchId));
        return "admin/recipients/import-preview";
    }

    @PostMapping("/admin/recipients/import/{batchId}/confirm")
    @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String confirm(@PathVariable UUID batchId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        service.confirm(principal.adminUserId(), batchId);
        return "redirect:/admin/recipients/import/" + batchId + "?completed";
    }

    @GetMapping({"/admin/recipients/import/template.xlsx", "/admin/recipients/import/template"})
    @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] workbookBytes;
        try (var book = RecipientWorkbookTemplate.create();
                var output = new ByteArrayOutputStream()) {
            book.write(output);
            workbookBytes = output.toByteArray();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, XLSX_MEDIA_TYPE.toString())
                .contentLength(workbookBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=recipient-import-template.xlsx")
                .body(workbookBytes);
    }

    private void base(AdminPrincipal principal, Model model) {
        model.addAttribute("administrator", principal);
        model.addAttribute("institutionName", RecipientInstitution.ORGANIZATION_NAME);
        model.addAttribute("clamAvDisabled", !clamAv.enabled());
    }
}
