package vn.mar.opportunity.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.opportunity.entity.Touchpoint;

public interface TouchpointRepository extends JpaRepository<Touchpoint, UUID> {
}
