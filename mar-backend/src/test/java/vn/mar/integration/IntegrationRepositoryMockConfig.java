package vn.mar.integration;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.mar.integration.repository.IntegrationEventRepository;

@Configuration
class IntegrationRepositoryMockConfig {

    @Bean
    IntegrationEventRepository integrationEventRepository() {
        return mock(IntegrationEventRepository.class);
    }
}
