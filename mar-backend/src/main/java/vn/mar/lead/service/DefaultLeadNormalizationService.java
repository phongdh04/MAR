package vn.mar.lead.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.mar.lead.api.LeadNormalizationIssue;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.api.LeadNormalizationService;
import vn.mar.lead.model.Contactability;
import vn.mar.lead.model.LeadContactField;
import vn.mar.lead.model.LeadNormalizationErrorCode;
import vn.mar.lead.model.LeadStatus;

@Service
public class DefaultLeadNormalizationService implements LeadNormalizationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public LeadNormalizationResult normalize(LeadNormalizationRequest request) {
        LeadNormalizationRequest safeRequest = request == null ? LeadNormalizationRequest.empty() : request;
        List<LeadNormalizationIssue> issues = new ArrayList<>();
        String normalizedPhone = normalizePhone(safeRequest.phone());
        String normalizedEmail = normalizeEmail(safeRequest.email());
        String normalizedZaloId = normalizeText(safeRequest.zaloId());

        if (StringUtils.hasText(safeRequest.phone()) && !isValidPhone(normalizedPhone)) {
            normalizedPhone = null;
            issues.add(issue(
                    LeadContactField.PHONE,
                    LeadNormalizationErrorCode.INVALID_PHONE,
                    "Phone number format is invalid"
            ));
        }
        if (StringUtils.hasText(safeRequest.email()) && !isValidEmail(normalizedEmail)) {
            normalizedEmail = null;
            issues.add(issue(
                    LeadContactField.EMAIL,
                    LeadNormalizationErrorCode.INVALID_EMAIL,
                    "Email format is invalid"
            ));
        }
        if (!hasAnyContactIdentifier(normalizedPhone, normalizedEmail, normalizedZaloId)) {
            issues.add(issue(
                    LeadContactField.CONTACT_IDENTIFIER,
                    LeadNormalizationErrorCode.CONTACT_IDENTIFIER_REQUIRED,
                    "Lead must include phone, email, or Zalo ID"
            ));
        }

        return new LeadNormalizationResult(
                normalizedPhone,
                normalizedEmail,
                normalizedZaloId,
                calculateContactability(normalizedPhone, normalizedEmail, normalizedZaloId),
                issues.isEmpty() ? LeadStatus.VALID : LeadStatus.INVALID,
                issues
        );
    }

    private String normalizePhone(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.startsWith("0084") && digits.length() >= 13) {
            return "0" + digits.substring(4);
        }
        if (digits.startsWith("84") && digits.length() >= 11 && digits.length() <= 12) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    private boolean isValidPhone(String normalizedPhone) {
        return StringUtils.hasText(normalizedPhone)
                && normalizedPhone.length() >= 9
                && normalizedPhone.length() <= 15;
    }

    private String normalizeEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String normalizedEmail) {
        return StringUtils.hasText(normalizedEmail)
                && EMAIL_PATTERN.matcher(normalizedEmail).matches();
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasAnyContactIdentifier(String phone, String email, String zaloId) {
        return StringUtils.hasText(phone) || StringUtils.hasText(email) || StringUtils.hasText(zaloId);
    }

    private Contactability calculateContactability(String phone, String email, String zaloId) {
        if (StringUtils.hasText(phone)) {
            return Contactability.HIGH;
        }
        if (StringUtils.hasText(zaloId)) {
            return Contactability.MEDIUM;
        }
        return Contactability.LOW;
    }

    private LeadNormalizationIssue issue(
            LeadContactField field,
            LeadNormalizationErrorCode code,
            String message) {
        return new LeadNormalizationIssue(field, code, message);
    }
}
