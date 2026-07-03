package vn.mar.lead.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.model.Contactability;
import vn.mar.lead.model.LeadContactField;
import vn.mar.lead.model.LeadNormalizationErrorCode;
import vn.mar.lead.model.LeadStatus;

class DefaultLeadNormalizationServiceTest {

    private final DefaultLeadNormalizationService service = new DefaultLeadNormalizationService();

    @Test
    void normalize_whenPhoneUsesVietnamCountryCode_shouldReturnDedupStableLocalPhone() {
        LeadNormalizationResult local = service.normalize(new LeadNormalizationRequest("090 123 4567", null, null));
        LeadNormalizationResult countryCode = service.normalize(new LeadNormalizationRequest("+84 90 123 4567", null, null));
        LeadNormalizationResult internationalPrefix = service.normalize(new LeadNormalizationRequest("0084-90-123-4567", null, null));

        assertThat(local.phoneNormalized()).isEqualTo("0901234567");
        assertThat(countryCode.phoneNormalized()).isEqualTo("0901234567");
        assertThat(internationalPrefix.phoneNormalized()).isEqualTo("0901234567");
        assertThat(countryCode.leadStatus()).isEqualTo(LeadStatus.VALID);
        assertThat(countryCode.contactability()).isEqualTo(Contactability.HIGH);
        assertThat(countryCode.issues()).isEmpty();
    }

    @Test
    void normalize_whenEmailHasUppercaseAndSpaces_shouldTrimAndLowercase() {
        LeadNormalizationResult result = service.normalize(new LeadNormalizationRequest(null, "  Valid.User@Example.COM  ", null));

        assertThat(result.email()).isEqualTo("valid.user@example.com");
        assertThat(result.leadStatus()).isEqualTo(LeadStatus.VALID);
        assertThat(result.contactability()).isEqualTo(Contactability.LOW);
        assertThat(result.hasContactIdentifier()).isTrue();
    }

    @Test
    void normalize_whenOnlyZaloIdPresent_shouldBeValidWithMediumContactability() {
        LeadNormalizationResult result = service.normalize(new LeadNormalizationRequest(null, null, "  zalo-123  "));

        assertThat(result.zaloId()).isEqualTo("zalo-123");
        assertThat(result.leadStatus()).isEqualTo(LeadStatus.VALID);
        assertThat(result.contactability()).isEqualTo(Contactability.MEDIUM);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void normalize_whenContactIdentifiersMissing_shouldReturnInvalidLead() {
        LeadNormalizationResult result = service.normalize(LeadNormalizationRequest.empty());

        assertThat(result.valid()).isFalse();
        assertThat(result.leadStatus()).isEqualTo(LeadStatus.INVALID);
        assertThat(result.contactability()).isEqualTo(Contactability.LOW);
        assertThat(result.issues()).extracting(issue -> issue.field())
                .containsExactly(LeadContactField.CONTACT_IDENTIFIER);
        assertThat(result.issues()).extracting(issue -> issue.code())
                .containsExactly(LeadNormalizationErrorCode.CONTACT_IDENTIFIER_REQUIRED);
    }

    @Test
    void normalize_whenPhoneInvalidButEmailValid_shouldPreserveEmailAndReturnInvalidStatus() {
        LeadNormalizationResult result = service.normalize(new LeadNormalizationRequest("123", "valid@example.com", null));

        assertThat(result.phoneNormalized()).isNull();
        assertThat(result.email()).isEqualTo("valid@example.com");
        assertThat(result.leadStatus()).isEqualTo(LeadStatus.INVALID);
        assertThat(result.contactability()).isEqualTo(Contactability.LOW);
        assertThat(result.issues()).extracting(issue -> issue.code())
                .containsExactly(LeadNormalizationErrorCode.INVALID_PHONE);
    }

    @Test
    void normalize_whenEmailInvalidAndNoOtherIdentifier_shouldReturnBothEmailAndContactErrors() {
        LeadNormalizationResult result = service.normalize(new LeadNormalizationRequest(null, "not-an-email", null));

        assertThat(result.email()).isNull();
        assertThat(result.leadStatus()).isEqualTo(LeadStatus.INVALID);
        assertThat(result.issues()).extracting(issue -> issue.code())
                .containsExactly(
                        LeadNormalizationErrorCode.INVALID_EMAIL,
                        LeadNormalizationErrorCode.CONTACT_IDENTIFIER_REQUIRED
                );
    }
}
