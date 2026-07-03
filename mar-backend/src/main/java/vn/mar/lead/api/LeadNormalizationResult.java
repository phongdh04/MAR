package vn.mar.lead.api;

import java.util.List;
import vn.mar.lead.model.Contactability;
import vn.mar.lead.model.LeadStatus;

public record LeadNormalizationResult(
        String phoneNormalized,
        String email,
        String zaloId,
        Contactability contactability,
        LeadStatus leadStatus,
        List<LeadNormalizationIssue> issues
) {

    public LeadNormalizationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasContactIdentifier() {
        return hasText(phoneNormalized) || hasText(email) || hasText(zaloId);
    }

    public boolean valid() {
        return LeadStatus.VALID == leadStatus;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
