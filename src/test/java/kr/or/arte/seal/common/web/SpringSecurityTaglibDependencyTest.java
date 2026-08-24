package kr.or.arte.seal.common.web;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

class SpringSecurityTaglibDependencyTest {
    @Test void packagedApplicationContainsAuthorizeTagForProtectedJspActions() {
        assertThatCode(() -> Class.forName("org.springframework.security.taglibs.authz.JspAuthorizeTag"))
                .doesNotThrowAnyException();
    }
}
