package vn.mar.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import vn.mar.common.exception.ValidationException;

public final class PageRequestFactory {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    public static PageRequest of(Integer requestedPage, Integer requestedSize) {
        return PageRequest.of(resolvePage(requestedPage), resolveSize(requestedSize));
    }

    public static PageRequest of(Integer requestedPage, Integer requestedSize, Sort sort) {
        return PageRequest.of(resolvePage(requestedPage), resolveSize(requestedSize), sort);
    }

    public static int resolvePage(Integer requestedPage) {
        if (requestedPage == null) {
            return DEFAULT_PAGE;
        }
        if (requestedPage < 0) {
            throw ValidationException.of("page", "MIN_VALUE", "Page must be greater than or equal to 0");
        }
        return requestedPage;
    }

    public static int resolveSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_SIZE;
        }
        if (requestedSize < 1 || requestedSize > MAX_SIZE) {
            throw ValidationException.of("size", "INVALID_SIZE", "Size must be between 1 and 100");
        }
        return requestedSize;
    }
}
