package vn.mar.sla.api;

import java.time.Instant;

public record SlaOverdueScanSnapshot(
        Instant scannedAt,
        int markedOverdueCount,
        int escalatedSalesLeadCount
) {
}
