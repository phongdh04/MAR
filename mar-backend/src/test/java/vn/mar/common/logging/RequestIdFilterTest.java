package vn.mar.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    @Test
    void doFilter_whenRequestIdMissing_shouldGenerateRequestIdAndClearContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                requestIdInsideChain.set(RequestContext.currentRequestId().orElse(null));

        requestIdFilter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).startsWith("req_");
        assertThat(requestIdInsideChain.get()).isEqualTo(response.getHeader(RequestIdFilter.HEADER_NAME));
        assertThat(RequestContext.currentRequestId()).isEmpty();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilter_whenRequestIdValid_shouldPropagateIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader(RequestIdFilter.HEADER_NAME, "req_client_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                requestIdInsideChain.set(RequestContext.currentRequestId().orElse(null));

        requestIdFilter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("req_client_123");
        assertThat(requestIdInsideChain.get()).isEqualTo("req_client_123");
        assertThat(RequestContext.currentRequestId()).isEmpty();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilter_whenRequestIdInvalid_shouldGenerateNewRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader(RequestIdFilter.HEADER_NAME, "invalid request id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
        };

        requestIdFilter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).startsWith("req_");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isNotEqualTo("invalid request id");
    }
}
