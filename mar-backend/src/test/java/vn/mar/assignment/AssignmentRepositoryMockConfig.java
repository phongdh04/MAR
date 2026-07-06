package vn.mar.assignment;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.mar.assignment.repository.AssignmentHistoryRepository;
import vn.mar.assignment.repository.AssignmentPoolStateRepository;
import vn.mar.assignment.repository.AssignmentRuleAdvisorRepository;
import vn.mar.assignment.repository.AssignmentRuleRepository;
import vn.mar.assignment.repository.UnassignedAssignmentItemRepository;

@Configuration
class AssignmentRepositoryMockConfig {

    @Bean
    AssignmentRuleRepository assignmentRuleRepository() {
        return mock(AssignmentRuleRepository.class);
    }

    @Bean
    AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository() {
        return mock(AssignmentRuleAdvisorRepository.class);
    }

    @Bean
    AssignmentPoolStateRepository assignmentPoolStateRepository() {
        return mock(AssignmentPoolStateRepository.class);
    }

    @Bean
    AssignmentHistoryRepository assignmentHistoryRepository() {
        return mock(AssignmentHistoryRepository.class);
    }

    @Bean
    UnassignedAssignmentItemRepository unassignedAssignmentItemRepository() {
        return mock(UnassignedAssignmentItemRepository.class);
    }
}
