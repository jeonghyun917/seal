package kr.or.arte.seal.common.config;

import jakarta.servlet.DispatcherType;
import kr.or.arte.seal.admin.config.AdminPrincipal;
import kr.or.arte.seal.admin.config.MustChangePasswordFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            kr.or.arte.seal.admin.config.AdminSessionGuard sessionGuard) throws Exception {
        boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));
        var defaultAccessDeniedHandler = new AccessDeniedHandlerImpl();
        http.addFilterAfter(new MustChangePasswordFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new kr.or.arte.seal.admin.config.AdminSessionValidityFilter(sessionGuard),
                UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                            .requestMatchers("/", "/login", "/resources/**", "/consent/**",
                                    "/actuator/health", "/error").permitAll();
                    if (localProfile) authorize.requestMatchers("/system/status").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, denied) -> {
                    boolean loginRequest = (request.getContextPath() + "/login")
                            .equals(request.getRequestURI());
                    boolean expiredCsrf = denied instanceof MissingCsrfTokenException
                            || denied instanceof InvalidCsrfTokenException;
                    if (loginRequest && expiredCsrf) {
                        var session = request.getSession(false);
                        if (session != null) {
                            session.invalidate();
                        }
                        response.sendRedirect(request.getContextPath() + "/login?expired");
                        return;
                    }
                    defaultAccessDeniedHandler.handle(request, response, denied);
                }))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .successHandler((request, response, authentication) -> {
                            String destination = authentication.getPrincipal() instanceof AdminPrincipal principal
                                    && principal.mustChangePassword() ? "/account/password/change" : "/admin";
                            response.sendRedirect(request.getContextPath() + destination);
                        }).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny()));
        return http.build();
    }
}
