package vn.mar.leadimport.fixture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import vn.mar.leadimport.dto.response.ImportBatchDetailResponse;
import vn.mar.leadimport.service.LeadImportFixtureService;

class LeadImportFixtureRunnerTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void run_whenLocalProfileAndPropertiesValid_shouldSeedFixture() {
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        LeadImportFixtureService fixtureService = mock(LeadImportFixtureService.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"qa"});
        when(environment.getProperty("mar.import.fixture.tenant-id")).thenReturn(TENANT_ID.toString());
        when(environment.getProperty("mar.import.fixture.actor-id")).thenReturn(ACTOR_ID.toString());
        when(environment.getProperty("mar.import.fixture.exit-on-complete", Boolean.class, false)).thenReturn(false);
        when(fixtureService.seedDefaultLeadFixture(TENANT_ID, ACTOR_ID)).thenReturn(response());

        new LeadImportFixtureRunner(environment, applicationContext, fixtureService).run(null);

        verify(fixtureService).seedDefaultLeadFixture(TENANT_ID, ACTOR_ID);
    }

    @Test
    void run_whenProdProfile_shouldFailClosed() {
        Environment environment = mock(Environment.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
        LeadImportFixtureService fixtureService = mock(LeadImportFixtureService.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        assertThatThrownBy(() -> new LeadImportFixtureRunner(environment, applicationContext, fixtureService).run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    private ImportBatchDetailResponse response() {
        return new ImportBatchDetailResponse(
                BATCH_ID,
                TENANT_ID,
                "LEAD",
                "CSV",
                "DRAFT",
                "qa-lead-import-fixture.csv",
                null,
                3,
                1,
                1,
                1,
                null,
                null,
                null
        );
    }
}
