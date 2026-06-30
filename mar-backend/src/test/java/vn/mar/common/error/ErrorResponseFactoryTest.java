package vn.mar.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.mar.common.logging.RequestContext;

class ErrorResponseFactoryTest {

    private final ErrorResponseFactory errorResponseFactory = new ErrorResponseFactory();

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void create_whenRequestContextExists_shouldUseSameRequestId() {
        RequestContext.setRequestId("req_factory_001");

        ErrorResponse response = errorResponseFactory.create(ErrorCode.PERMISSION_DENIED);

        assertThat(response.error().code()).isEqualTo("PERMISSION_DENIED");
        assertThat(response.error().details()).isEmpty();
        assertThat(response.meta().requestId()).isEqualTo("req_factory_001");
    }

    @Test
    void create_whenDetailsProvided_shouldKeepFieldCodeAndMessage() {
        RequestContext.setRequestId("req_factory_002");
        ErrorDetail detail = ErrorDetail.of("tenant_name", "REQUIRED", "Tenant name is required");

        ErrorResponse response = errorResponseFactory.create(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(detail)
        );

        assertThat(response.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.error().details()).containsExactly(detail);
        assertThat(response.meta().requestId()).isEqualTo("req_factory_002");
    }
}
