package vn.mar.branch.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.branch.dto.response.BranchDetailResponse;
import vn.mar.branch.entity.Branch;

@Component
public class BranchMapper {

    public BranchDetailResponse toDetailResponse(Branch branch) {
        return new BranchDetailResponse(
                branch.id(),
                branch.tenantId(),
                branch.branchCode(),
                branch.branchName(),
                branch.city(),
                branch.phoneNumber(),
                branch.address(),
                branch.status().name(),
                branch.createdAt(),
                branch.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(Branch branch) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("branch_id", branch.id().toString());
        data.put("tenant_id", branch.tenantId().toString());
        data.put("branch_code", branch.branchCode());
        data.put("branch_name", branch.branchName());
        data.put("city", branch.city());
        data.put("phone_number", branch.phoneNumber());
        data.put("address", branch.address());
        data.put("status", branch.status().name());
        return data;
    }
}
