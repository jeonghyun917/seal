package kr.or.arte.seal.request.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class ConsentRequestSnapshotPresenter {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObjectMapper objectMapper;

    public ConsentRequestSnapshotPresenter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ConsentRequestSnapshotView present(String snapshotJson) {
        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            return new ConsentRequestSnapshotView(
                    text(root, "institution"),
                    text(root, "requestCode"),
                    text(root, "title"),
                    text(root, "introductionHtml"),
                    text(root, "processingActivityName"),
                    text(root, "processingPurpose"),
                    text(root, "retentionDisplayText"),
                    DATE_TIME.format(Instant.parse(text(root, "expiresAt")).atZone(KOREA)),
                    text(root, "withdrawalNotice"),
                    text(root, "processingActivityVersionId"),
                    text(root, "retentionPolicyVersionId"),
                    text(root, "templateVersionId"),
                    text(root, "templateContentHash"),
                    sha256(snapshotJson),
                    items(root.path("items")));
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new IllegalArgumentException("동의 요청 확정 내용을 읽을 수 없습니다.", exception);
        }
    }

    private static List<ConsentRequestSnapshotView.Item> items(JsonNode nodes) {
        List<ConsentRequestSnapshotView.Item> items = new ArrayList<>();
        for (JsonNode node : nodes) {
            List<String> dataItemNames = new ArrayList<>();
            for (JsonNode dataItem : node.path("dataItems")) dataItemNames.add(text(dataItem, "name"));
            items.add(new ConsentRequestSnapshotView.Item(
                    node.path("order").asInt(),
                    text(node, "code"),
                    typeLabel(text(node, "type")),
                    text(node, "title"),
                    node.path("required").asBoolean(),
                    text(node, "purpose"),
                    text(node, "retentionText"),
                    text(node, "refusalNotice"),
                    text(node, "thirdPartyRecipient"),
                    dataItemNames));
        }
        return items;
    }

    private static String typeLabel(String type) {
        return switch (type) {
            case "COLLECTION_USE" -> "개인정보 수집·이용";
            case "THIRD_PARTY_PROVISION" -> "제3자 제공";
            case "OPTIONAL" -> "기타 선택 동의";
            default -> type;
        };
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("");
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