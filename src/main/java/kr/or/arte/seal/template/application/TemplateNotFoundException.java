package kr.or.arte.seal.template.application;

public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException() {
        super("동의서 양식을 찾을 수 없습니다.");
    }
}
