package kr.or.arte.seal.consent.application;
import java.util.*;
public final class ConsentDecisionValidator {private ConsentDecisionValidator(){}public static void validate(List<UUID> expected,Map<UUID,ConsentDecision> actual){if(actual==null||actual.size()!=expected.size()||!actual.keySet().equals(new HashSet<>(expected))||actual.values().stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("모든 동의 항목에 동의 또는 미동의를 선택해 주세요.");}}
