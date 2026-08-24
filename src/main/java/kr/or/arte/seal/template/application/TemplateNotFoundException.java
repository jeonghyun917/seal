package kr.or.arte.seal.template.application;

public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException() {
        super("동의서 템플릿을 찾을 수 없습니다.");
    }
}
