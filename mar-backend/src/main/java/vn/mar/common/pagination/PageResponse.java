package vn.mar.common.pagination;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static <T> PageResponse<T> empty(Pageable pageable) {
        return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
    }

    public <R> PageResponse<R> map(Function<? super T, R> mapper) {
        return new PageResponse<>(
                items.stream().map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
