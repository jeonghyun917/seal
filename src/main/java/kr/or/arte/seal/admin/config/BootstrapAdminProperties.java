package kr.or.arte.seal.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("seal.bootstrap-admin")
public record BootstrapAdminProperties(String id, String name, String password) {

    public boolean complete() {
        return hasText(id) && hasText(name) && hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "BootstrapAdminProperties[id=" + id + ", name=" + name + ", password=<redacted>]";
    }
}
