package kr.or.arte.seal.template.config;

import kr.or.arte.seal.template.application.TemplatePreviewRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplatePreviewConfiguration {
    @Bean
    TemplatePreviewRenderer templatePreviewRenderer() {
        return new TemplatePreviewRenderer();
    }
}
