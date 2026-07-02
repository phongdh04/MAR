package vn.mar.leadimport.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.mar.common.error.ErrorDetail;

public record ImportRowErrorResponse(
        UUID importRowId,
        UUID batchId,
        int rowNumber,
        String field,
        String code,
        String message,
        List<ErrorDetail> errors,
        Instant createdAt
) {
}
