package vn.mar.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.common.logging.RequestIdFilter;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiBootstrapSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_whenPublic_shouldReturnSuccessEnvelopeAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/health").header(RequestIdFilter.HEADER_NAME, "req_health_001"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_health_001"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("mar-api"))
                .andExpect(jsonPath("$.data.profile").value("test"))
                .andExpect(jsonPath("$.meta.request_id").value("req_health_001"));
    }

    @Test
    void protectedEndpoint_whenUnauthenticated_shouldReturnSecurityErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/protected-smoke").header(RequestIdFilter.HEADER_NAME, "req_auth_001"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_auth_001"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.message").value("Authentication is required"))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.meta.request_id").value("req_auth_001"));
    }
}
