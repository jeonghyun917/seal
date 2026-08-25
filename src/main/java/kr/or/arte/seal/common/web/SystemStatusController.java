package kr.or.arte.seal.common.web;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;

@Controller
public class SystemStatusController {
    private final ObjectProvider<DataSource> dataSources;

    public SystemStatusController(ObjectProvider<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @GetMapping("/system/status")
    public String status(Model model) {
        model.addAttribute("runtimeVersion", "eGovFrame Runtime " + versionOf(EgovAbstractServiceImpl.class));
        model.addAttribute("javaVersion", System.getProperty("java.runtime.version", System.getProperty("java.version")));
        model.addAttribute("databaseVersion", databaseVersion());
        model.addAttribute("viewTechnology", "JSP / JSTL");
        model.addAttribute("deliveryScope", "이메일 + 일회용 보안 링크");
        model.addAttribute("excludedScope", "전자서명·SMS·OTP·실명 본인확인 제외");
        return "system/status";
    }

    private String databaseVersion() {
        DataSource dataSource = dataSources.getIfAvailable();
        if (dataSource == null) return "연결 정보 없음";
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            return metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion();
        } catch (Exception exception) {
            return "연결 확인 불가";
        }
    }

    private static String versionOf(Class<?> type) {
        String version = type.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "5.x" : version;
    }
}
