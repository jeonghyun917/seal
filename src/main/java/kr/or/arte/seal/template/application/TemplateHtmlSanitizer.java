package kr.or.arte.seal.template.application;

import org.owasp.html.PolicyFactory;

import java.util.Objects;

public class TemplateHtmlSanitizer {
    private final PolicyFactory policy;

    public TemplateHtmlSanitizer(PolicyFactory policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public String sanitize(String html) {
        return policy.sanitize(html == null ? "" : html);
    }
}
