package kr.or.arte.seal.rights.web;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.UUID;

@ControllerAdvice(basePackages = {
        "kr.or.arte.seal.admin.web", "kr.or.arte.seal.delivery.web", "kr.or.arte.seal.evidence.web",
        "kr.or.arte.seal.policy.web", "kr.or.arte.seal.recipient", "kr.or.arte.seal.request.web",
        "kr.or.arte.seal.retention.web", "kr.or.arte.seal.rights.web", "kr.or.arte.seal.statistics.web",
        "kr.or.arte.seal.template.web"
})
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminWebExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(RuntimeException exception, HttpServletResponse response, Model model) {
        return error("error/404", exception, response, model, false);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(RuntimeException exception, HttpServletResponse response, Model model) {
        return error("error/400", exception, response, model, false);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(RuntimeException exception, HttpServletResponse response, Model model) {
        return error("error/409", exception, response, model, false);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(RuntimeException exception, HttpServletResponse response, Model model) {
        return error("error/403", exception, response, model, false);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String responseStatus(ResponseStatusException exception, HttpServletResponse response, Model model) {
        int status = exception.getStatusCode().value();
        response.setStatus(status);
        String view = switch (status) {
            case 400 -> "error/400";
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 409 -> "error/409";
            default -> "error/500";
        };
        return error(view, exception, response, model, status >= 500);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String internal(Exception exception, HttpServletResponse response, Model model) {
        return error("error/500", exception, response, model, true);
    }

    private static String error(String view, Exception exception, HttpServletResponse response,
            Model model, boolean logFailure) {
        String reference = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("errorReference", reference);
        if (logFailure) log.error("관리자 요청 처리 실패 reference={}", reference, exception);
        else log.info("관리자 요청 거부 reference={} type={}", reference, exception.getClass().getSimpleName());
        return view;
    }
}
