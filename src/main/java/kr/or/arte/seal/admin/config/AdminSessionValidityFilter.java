package kr.or.arte.seal.admin.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public final class AdminSessionValidityFilter extends OncePerRequestFilter {
    private final AdminSessionGuard guard;
    public AdminSessionValidityFilter(AdminSessionGuard guard) { this.guard = guard; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        final boolean invalid;
        try { invalid = guard.isInvalid(authentication); }
        catch (RuntimeException exception) {
            // A database outage must never turn into fail-open authorization.
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (invalid) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.sendRedirect(request.getContextPath() + "/login?revoked");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/resources/") || path.equals("/logout");
    }
}
