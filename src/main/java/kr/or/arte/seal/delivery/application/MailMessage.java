package kr.or.arte.seal.delivery.application;

public record MailMessage(String to, String subject, String html, String messageId) {
    public MailMessage(String to, String subject, String html) {
        this(to, subject, html, null);
    }
}
