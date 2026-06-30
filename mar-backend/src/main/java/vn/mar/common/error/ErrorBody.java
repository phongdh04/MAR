package vn.mar.common.error;

import java.util.List;

public record ErrorBody(
        String code,
        String message,
        List<ErrorDetail> details
) {

    public ErrorBody {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
