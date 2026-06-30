package vn.mar.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";

    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._:-]+$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);

        try {
            RequestContext.setRequestId(requestId);
            MDC.put("requestId", requestId);
            MDC.put("httpMethod", request.getMethod());
            MDC.put("path", request.getRequestURI());
            MDC.put("clientIp", request.getRemoteAddr());
            response.setHeader(HEADER_NAME, requestId);

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incomingRequestId = request.getHeader(HEADER_NAME);
        if (isValidRequestId(incomingRequestId)) {
            return incomingRequestId;
        }
        return "req_" + UUID.randomUUID();
    }

    private boolean isValidRequestId(String requestId) {
        return requestId != null
                && !requestId.isBlank()
                && requestId.length() <= MAX_REQUEST_ID_LENGTH
                && SAFE_REQUEST_ID.matcher(requestId).matches();
    }
}
