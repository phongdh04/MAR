package vn.mar.permission.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.repository.PermissionProfileRepository;

@Service
public class PermissionMatrixInitializationService {

    private final PermissionProfileRepository permissionProfileRepository;

    public PermissionMatrixInitializationService(PermissionProfileRepository permissionProfileRepository) {
        this.permissionProfileRepository = permissionProfileRepository;
    }

    @Transactional
    public void initializeDefaults(UUID tenantId, UUID actorId, Instant now) {
        if (tenantId == null || permissionProfileRepository.existsByTenantId(tenantId)) {
            return;
        }
        List<String> activeFunctionCodes = permissionProfileRepository.findActiveFunctionCodes();
        if (activeFunctionCodes == null || activeFunctionCodes.isEmpty()) {
            return;
        }
        List<PermissionProfile> defaultProfiles = PermissionMatrixDefaultPolicy.createDefaultProfiles(
                tenantId,
                actorId,
                now,
                activeFunctionCodes
        );
        permissionProfileRepository.insertAll(defaultProfiles);
    }
}
