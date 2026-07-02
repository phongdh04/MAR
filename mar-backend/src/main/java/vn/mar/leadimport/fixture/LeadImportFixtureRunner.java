package vn.mar.leadimport.fixture;

import java.util.Arrays;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.service.LeadImportFixtureService;

@Component
@ConditionalOnProperty(prefix = "mar.import.fixture", name = "enabled", havingValue = "true")
public class LeadImportFixtureRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeadImportFixtureRunner.class);

    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;
    private final LeadImportFixtureService leadImportFixtureService;

    public LeadImportFixtureRunner(
            Environment environment,
            ConfigurableApplicationContext applicationContext,
            LeadImportFixtureService leadImportFixtureService) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.leadImportFixtureService = leadImportFixtureService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureNotProduction();
        UUID tenantId = requireUuid("mar.import.fixture.tenant-id");
        UUID actorId = requireUuid("mar.import.fixture.actor-id");
        ImportBatchDetailResponse response = leadImportFixtureService.seedDefaultLeadFixture(tenantId, actorId);
        LOGGER.info(
                "lead import fixture seeded tenantId={} batchId={} totalRows={} errorCount={}",
                response.tenantId(),
                response.batchId(),
                response.totalRows(),
                response.errorCount()
        );
        if (environment.getProperty("mar.import.fixture.exit-on-complete", Boolean.class, false)) {
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }

    private void ensureNotProduction() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
        if (production) {
            throw new IllegalStateException("Import fixture runner is disabled for prod profile");
        }
    }

    private UUID requireUuid(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required import fixture property: " + propertyName);
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid UUID import fixture property: " + propertyName, exception);
        }
    }
}
