package kr.or.arte.seal.rights.application;
import kr.or.arte.seal.rights.domain.*;import java.time.*;import java.util.UUID;
public record RightsRequestSummary(UUID id,String code,RightsRequestType type,RightsRequestStatus status,LocalDate receivedDate,LocalDate dueDate,Instant createdAt,
        UUID recipientId, String templateTitle){
    @org.apache.ibatis.annotations.AutomapConstructor
    public RightsRequestSummary {}
    public RightsRequestSummary(UUID id,String code,RightsRequestType type,RightsRequestStatus status,
            LocalDate receivedDate,LocalDate dueDate,Instant createdAt) {
        this(id,code,type,status,receivedDate,dueDate,createdAt,null,null);
    }
}
