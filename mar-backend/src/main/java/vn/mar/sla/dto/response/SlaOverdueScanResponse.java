package vn.mar.sla.dto.response;

import java.time.Instant;

public record SlaOverdueScanResponse(
        Instant scannedAt,
        int markedOverdueCount,
        int escalatedSalesLeadCount
) {
}
