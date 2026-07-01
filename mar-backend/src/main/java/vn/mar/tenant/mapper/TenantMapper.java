package vn.mar.tenant.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.tenant.dto.response.TenantDetailResponse;
import vn.mar.tenant.entity.Tenant;

@Component
public class TenantMapper {

    public TenantDetailResponse toDetailResponse(Tenant tenant) {
        return new TenantDetailResponse(
                tenant.id(),
                tenant.tenantCode(),
                tenant.tenantName(),
                tenant.timezone(),
                tenant.defaultCurrency(),
                tenant.status().name(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(Tenant tenant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenant_id", tenant.id().toString());
        data.put("tenant_code", tenant.tenantCode());
        data.put("tenant_name", tenant.tenantName());
        data.put("timezone", tenant.timezone());
        data.put("default_currency", tenant.defaultCurrency());
        data.put("status", tenant.status().name());
        return data;
    }
}
