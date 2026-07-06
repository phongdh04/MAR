package vn.mar.sla;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.mar.sla.repository.SlaTaskRepository;

@Configuration
class SlaTaskRepositoryMockConfig {

    @Bean
    SlaTaskRepository slaTaskRepository() {
        return mock(SlaTaskRepository.class);
    }
}
