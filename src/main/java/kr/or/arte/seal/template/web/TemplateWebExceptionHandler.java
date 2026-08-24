package kr.or.arte.seal.template.web;

import kr.or.arte.seal.template.application.TemplateBusinessConflictException;
import kr.or.arte.seal.template.application.TemplateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ConsentTemplateController.class)
public class TemplateWebExceptionHandler {
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("동의서 템플릿을 찾을 수 없습니다.");
    }

    @ExceptionHandler(TemplateBusinessConflictException.class)
    public ResponseEntity<String> conflict(TemplateBusinessConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }
}
