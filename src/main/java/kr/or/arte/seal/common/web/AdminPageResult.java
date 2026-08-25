package kr.or.arte.seal.common.web;

import java.util.List;

public record AdminPageResult<T>(List<T> items, int page, int size, boolean hasNext) {
    public AdminPageResult {
        items = List.copyOf(items);
        if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("페이지 범위가 올바르지 않습니다.");
    }

    public static <T> AdminPageResult<T> of(List<T> fetched, int requestedPage, int requestedSize) {
        int page = Math.max(1, requestedPage);
        int size = Math.max(1, Math.min(100, requestedSize));
        boolean hasNext = fetched.size() > size;
        return new AdminPageResult<>(fetched.stream().limit(size).toList(), page, size, hasNext);
    }

    public int offset() { return (page - 1) * size; }
    public int fetchSize() { return size + 1; }
    public boolean hasPrevious() { return page > 1; }
}
