package vn.mar.lead.api;

import vn.mar.lead.model.LeadContactField;
import vn.mar.lead.model.LeadNormalizationErrorCode;

public record LeadNormalizationIssue(
        LeadContactField field,
        LeadNormalizationErrorCode code,
        String message
) {
}
