package kr.or.arte.seal.request.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestSnapshotFactory {
    private final ObjectMapper objectMapper;

    public RequestSnapshotFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public RequestSnapshot create(RequestSnapshotSource source) {
        List<RequestSnapshot.Item> items = source.template().items().stream()
                .sorted(Comparator.comparingInt(item -> item.order()))
                .map(item -> new RequestSnapshot.Item(item.order(), item.code(), item.type().name(),
                        item.title(), item.required(), item.purpose(), item.retentionText(),
                        item.refusalNotice(), item.thirdPartyRecipient(), item.dataItemNames())).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("institution", source.institution());
        payload.put("requestCode", source.requestCode());
        payload.put("processingActivityVersionId", source.processingActivityVersionId());
        payload.put("retentionPolicyVersionId", source.retentionPolicyVersionId());
        payload.put("retentionDisplayText", source.retentionDisplayText());
        payload.put("templateVersionId", source.templateVersionId());
        payload.put("templateContentHash", source.templateContentHash());
        payload.put("title", source.template().title());
        payload.put("introductionHtml", normalizeLines(source.template().introductionHtml()));
        payload.put("expiresAt", source.expiresAt().toString());
        payload.put("withdrawalNotice", source.withdrawalNotice());
        payload.put("processingActivityName", source.template().processingActivityName());
        payload.put("processingPurpose", source.template().processingPurpose());
        payload.put("items", items);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("요청 스냅샷 직렬화에 실패했습니다.", exception);
        }
        return new RequestSnapshot(json, sha256(json), source.templateContentHash(), items);
    }

    private static String normalizeLines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
