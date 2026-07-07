package vn.mar.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import vn.mar.common.exception.ValidationException;

class EnumParserTest {

    @Test
    void optionalEnumReturnsNullForBlankValue() {
        TestStatus status = EnumParser.optionalEnum(TestStatus.class, "   ", "status", "INVALID", "Status is invalid");

        assertThat(status).isNull();
    }

    @Test
    void optionalEnumParsesTrimmedCaseInsensitiveValue() {
        TestStatus status = EnumParser.optionalEnum(TestStatus.class, " active ", "status", "INVALID", "Status is invalid");

        assertThat(status).isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    void requiredEnumRejectsBlankValueWithConfiguredDetail() {
        assertThatThrownBy(() -> EnumParser.requiredEnum(TestStatus.class, null, "status", "REQUIRED", "Status is required"))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getDetails()).singleElement().satisfies(detail -> {
                        assertThat(detail.field()).isEqualTo("status");
                        assertThat(detail.code()).isEqualTo("REQUIRED");
                        assertThat(detail.message()).isEqualTo("Status is required");
                    });
                });
    }

    @Test
    void requiredEnumRejectsInvalidValueWithConfiguredDetail() {
        assertThatThrownBy(() -> EnumParser.requiredEnum(TestStatus.class, "missing", "status", "INVALID", "Status is invalid"))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getDetails()).singleElement().satisfies(detail -> {
                        assertThat(detail.field()).isEqualTo("status");
                        assertThat(detail.code()).isEqualTo("INVALID");
                        assertThat(detail.message()).isEqualTo("Status is invalid");
                    });
                });
    }

    @Test
    void optionalEnumRejectsInvalidValueWithConfiguredDetail() {
        assertThatThrownBy(() -> EnumParser.optionalEnum(TestStatus.class, "missing", "status", "INVALID", "Status is invalid"))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getDetails()).singleElement().satisfies(detail -> {
                        assertThat(detail.field()).isEqualTo("status");
                        assertThat(detail.code()).isEqualTo("INVALID");
                        assertThat(detail.message()).isEqualTo("Status is invalid");
                    });
                });
    }

    private enum TestStatus {
        ACTIVE
    }
}
