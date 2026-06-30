package vn.mar.common.health;

import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        String application = environment.getProperty("spring.application.name", "mar-api");
        String profile = String.join(",", Arrays.asList(environment.getActiveProfiles()));
        return ApiResponse.success(new HealthResponse("UP", application, profile));
    }
}
