package kr.or.arte.seal.template.web;

import kr.or.arte.seal.template.application.TemplateNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ConsentTemplateController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TemplateWebExceptionHandler {
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("동의서 양식을 찾을 수 없습니다.");
    }

}
