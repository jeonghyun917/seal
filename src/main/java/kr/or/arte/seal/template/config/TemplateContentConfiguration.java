package kr.or.arte.seal.template.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.template.application.TemplateContentHasher;
import kr.or.arte.seal.template.application.TemplateHtmlSanitizer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateContentConfiguration {

    @Bean
    public PolicyFactory templateHtmlPolicy() {
        return new HtmlPolicyBuilder()
                .allowElements("p", "br", "ul", "ol", "li", "strong", "em", "h3", "h4")
                .toFactory();
    }

    @Bean
    public TemplateHtmlSanitizer templateHtmlSanitizer(PolicyFactory templateHtmlPolicy) {
        return new TemplateHtmlSanitizer(templateHtmlPolicy);
    }

    @Bean
    public TemplateContentHasher templateContentHasher(ObjectMapper objectMapper) {
        return new TemplateContentHasher(objectMapper);
    }
}
