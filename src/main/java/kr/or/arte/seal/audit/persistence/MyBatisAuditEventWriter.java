package kr.or.arte.seal.audit.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAuditEventWriter implements AuditEventWriter {

    private final AuditEventMapper mapper;
    private final ObjectMapper objectMapper;

    public MyBatisAuditEventWriter(AuditEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(AuditEventCommand command) {
        try {
            String metadataJson = objectMapper.writeValueAsString(command.metadata());
            int inserted = mapper.insert(new AuditEventRow(
                    command.occurredAt(),
                    command.actorType(),
                    command.actorId(),
                    command.actionCode(),
                    command.targetType(),
                    command.targetId(),
                    command.result(),
                    command.reasonCode(),
                    command.correlationId(),
                    metadataJson
            ));
            if (inserted != 1) {
                throw new IllegalStateException("audit event insert did not affect exactly one row");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("audit metadata cannot be serialized", exception);
        }
    }
}
