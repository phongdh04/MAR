package vn.mar.lead.api;

public record LeadNormalizationRequest(
        String phone,
        String email,
        String zaloId
) {

    public static LeadNormalizationRequest empty() {
        return new LeadNormalizationRequest(null, null, null);
    }
}
