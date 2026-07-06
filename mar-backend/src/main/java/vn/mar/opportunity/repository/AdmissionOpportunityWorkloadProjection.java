package vn.mar.opportunity.repository;

import java.util.UUID;

public interface AdmissionOpportunityWorkloadProjection {

    UUID getOwnerId();

    long getWorkload();
}
