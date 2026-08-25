package kr.or.arte.seal.delivery.application;
public enum MailOutboxStatus { QUEUED, PROCESSING, SMTP_ACCEPTED, RETRY_SCHEDULED, SEND_FAILED, CANCELLED, DESTROYED }
