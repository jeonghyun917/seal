package kr.or.arte.seal.template.application;

public class TemplateBusinessConflictException extends RuntimeException {
    public TemplateBusinessConflictException(String message) {
        super(message);
    }

    public TemplateBusinessConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
