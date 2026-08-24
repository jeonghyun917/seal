package kr.or.arte.seal.recipient.importing;

import jakarta.servlet.http.HttpServletResponse;
import kr.or.arte.seal.admin.application.organization.OrganizationManagement;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.util.UUID;

@Controller
public class RecipientImportController {
    private final RecipientImportService service; private final OrganizationManagement organizations; private final ClamAvProperties clamAv;
    public RecipientImportController(RecipientImportService service,OrganizationManagement organizations,ClamAvProperties clamAv){this.service=service;this.organizations=organizations;this.clamAv=clamAv;}
    @GetMapping("/admin/recipients/import") @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String form(@AuthenticationPrincipal AdminPrincipal p,Model m){base(p,m);return "admin/recipients/import-form";}
    @PostMapping("/admin/recipients/import") @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String upload(@RequestParam UUID organizationUnitId,@RequestParam String collectionBasis,@RequestParam MultipartFile file,
            @AuthenticationPrincipal AdminPrincipal p,Model m) throws IOException{
        try{var preview=service.validate(p.adminUserId(),organizationUnitId,collectionBasis,file.getBytes());return "redirect:/admin/recipients/import/"+preview.batchId();}
        catch(IllegalArgumentException|RecipientWorkbookException e){base(p,m);m.addAttribute("error",e.getMessage());m.addAttribute("organizationUnitId",organizationUnitId);m.addAttribute("collectionBasis",collectionBasis);return "admin/recipients/import-form";}
    }
    @GetMapping("/admin/recipients/import/{batchId}") @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String preview(@PathVariable UUID batchId,@AuthenticationPrincipal AdminPrincipal p,Model m){m.addAttribute("administrator",p);m.addAttribute("preview",service.preview(p.adminUserId(),batchId));return "admin/recipients/import-preview";}
    @PostMapping("/admin/recipients/import/{batchId}/confirm") @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public String confirm(@PathVariable UUID batchId,@AuthenticationPrincipal AdminPrincipal p){service.confirm(p.adminUserId(),batchId);return "redirect:/admin/recipients/import/"+batchId+"?completed";}
    @GetMapping("/admin/recipients/import/template.xlsx") @PreAuthorize("hasAuthority('RECIPIENT_IMPORT')")
    public void template(HttpServletResponse response) throws IOException{response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=recipient-import-template.xlsx");
        try(var book=new XSSFWorkbook()){var sheet=book.createSheet("대상자");var h=sheet.createRow(0);h.createCell(0).setCellValue("이름");h.createCell(1).setCellValue("이메일");sheet.setColumnWidth(0,5000);sheet.setColumnWidth(1,9000);book.write(response.getOutputStream());}}
    private void base(AdminPrincipal p,Model m){m.addAttribute("administrator",p);m.addAttribute("organizations",organizations.findVisibleOrganizations(p.adminUserId()));m.addAttribute("clamAvDisabled",!clamAv.enabled());}
}
