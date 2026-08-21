package kr.or.arte.seal.audit.application;

public interface AuditEventWriter {

    void append(AuditEventCommand command);
}
