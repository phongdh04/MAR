package vn.mar.user.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.time.TimeProvider;
import vn.mar.role.model.RoleCode;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.user.dto.request.CreateUserRequest;
import vn.mar.user.dto.request.UpdateUserRequest;
import vn.mar.user.dto.request.UserSearchRequest;
import vn.mar.user.dto.response.UserDetailResponse;
import vn.mar.user.entity.User;
import vn.mar.user.mapper.UserMapper;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@Service
public class UserService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final UserBranchRepository userBranchRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            UserBranchRepository userBranchRepository,
            BranchRepository branchRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.userBranchRepository = userBranchRepository;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
    }

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        UUID userId = UUID.randomUUID();
        String email = requireEmail(request.email());
        assertEmailAvailableForCreate(tenantId, email);

        String roleCode = resolveActiveRole(request.role(), true);
        UserStatus status = resolveWritableStatus(request.status(), UserStatus.ACTIVE);
        Set<UUID> requestedBranchIds = normalizeBranchIds(request.branchIds());
        assertCanReceiveAssignments(status, requestedBranchIds);
        List<Branch> branches = resolveActiveBranches(tenantId, requestedBranchIds);

        User user = User.create(
                userId,
                tenantId,
                email,
                requireFullName(request.fullName()),
                normalizeOptional(request.phone()),
                roleCode,
                status,
                actor.actorId(),
                now
        );

        User savedUser = userRepository.save(user);
        List<UserBranch> assignments = createAssignments(actor, savedUser, branches, now);
        Set<UUID> assignedBranchIds = toBranchIdSet(assignments);

        auditUserChange(AuditActions.USER_CREATED, actor, savedUser, null, userMapper.toAuditData(savedUser, assignedBranchIds), null);
        if (!assignedBranchIds.isEmpty()) {
            auditUserChange(AuditActions.USER_BRANCH_ASSIGNED, actor, savedUser, Map.of("branch_ids", List.of()), Map.of("branch_ids", List.copyOf(assignedBranchIds)), null);
        }
        return userMapper.toDetailResponse(savedUser, assignedBranchIds);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUser(UUID userId) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        User user = findUser(tenantId, userId);
        Set<UUID> branchIds = findActiveBranchIds(tenantId, userId);
        return userMapper.toDetailResponse(user, branchIds);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDetailResponse> searchUsers(UserSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        int page = resolvePage(request.page());
        int size = resolveSize(request.size());
        UserStatus status = resolveWritableStatus(request.status(), null);
        String roleCode = resolveActiveRole(request.role(), false);
        String keyword = normalizeKeyword(request.keyword());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = userRepository.search(tenantId, status, roleCode, keyword, pageable);
        List<UUID> userIds = users.getContent().stream().map(User::id).toList();
        Map<UUID, List<UUID>> branchIdsByUserId = findActiveBranchIdsByUserId(tenantId, userIds);
        Page<UserDetailResponse> responsePage = users.map(user -> userMapper.toDetailResponse(
                user,
                branchIdsByUserId.getOrDefault(user.id(), List.of())
        ));
        return PageResponse.from(responsePage);
    }

    @Transactional
    public UserDetailResponse updateUser(UUID userId, UpdateUserRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        User user = findUser(tenantId, userId);
        List<UserBranch> activeAssignments = findActiveAssignments(tenantId, userId);
        Set<UUID> beforeBranchIds = toBranchIdSet(activeAssignments);
        Map<String, Object> beforeData = userMapper.toAuditData(user, beforeBranchIds);
        UserStatus previousStatus = user.status();

        String email = resolveEmailForUpdate(request.email(), user.email(), tenantId, user.id());
        String fullName = resolveFullNameForUpdate(request.fullName(), user.fullName());
        String roleCode = request.role() == null ? user.roleCode() : resolveActiveRole(request.role(), true);
        UserStatus status = resolveWritableStatus(request.status(), user.status());
        Set<UUID> requestedBranchIds = request.branchIds() == null ? null : normalizeBranchIds(request.branchIds());
        if (requestedBranchIds != null) {
            assertCanReceiveAssignments(status, requestedBranchIds);
        }

        boolean userFieldChanged = request.email() != null
                || request.fullName() != null
                || request.phone() != null
                || request.role() != null
                || request.status() != null;
        if (userFieldChanged) {
            user.update(
                    email,
                    fullName,
                    resolveOptionalForUpdate(request.phone(), user.phoneNumber()),
                    roleCode,
                    status,
                    actor.actorId(),
                    now
            );
            userRepository.save(user);
        }

        BranchAssignmentChange assignmentChange = applyBranchAssignments(
                actor,
                user,
                activeAssignments,
                requestedBranchIds,
                now
        );
        Set<UUID> afterBranchIds = assignmentChange.afterBranchIds();

        if (userFieldChanged) {
            String action = previousStatus == user.status()
                    ? AuditActions.USER_UPDATED
                    : AuditActions.USER_STATUS_CHANGED;
            auditUserChange(
                    action,
                    actor,
                    user,
                    beforeData,
                    userMapper.toAuditData(user, afterBranchIds),
                    normalizeOptional(request.reason())
            );
        }
        if (assignmentChange.changed()) {
            auditUserChange(
                    AuditActions.USER_BRANCH_ASSIGNED,
                    actor,
                    user,
                    Map.of("branch_ids", List.copyOf(beforeBranchIds)),
                    Map.of("branch_ids", List.copyOf(afterBranchIds)),
                    normalizeOptional(request.reason())
            );
        }
        return userMapper.toDetailResponse(user, afterBranchIds);
    }

    private User findUser(UUID tenantId, UUID userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String requireFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            throw validation("full_name", "REQUIRED", "Full name is required");
        }
        return fullName.trim();
    }

    private String resolveFullNameForUpdate(String requestedFullName, String currentFullName) {
        if (requestedFullName == null) {
            return currentFullName;
        }
        return requireFullName(requestedFullName);
    }

    private String requireEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw validation("email", "REQUIRED", "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveEmailForUpdate(String requestedEmail, String currentEmail, UUID tenantId, UUID userId) {
        if (requestedEmail == null) {
            return currentEmail;
        }
        String email = requireEmail(requestedEmail);
        if (!email.equalsIgnoreCase(currentEmail)
                && userRepository.existsByTenantIdAndEmailIgnoreCaseAndIdNot(tenantId, email, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_EMAIL, ErrorCode.DUPLICATE_USER_EMAIL.defaultMessage());
        }
        return email;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveOptionalForUpdate(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        return normalizeOptional(requestedValue);
    }

    private String resolveActiveRole(String requestedRole, boolean required) {
        if (!StringUtils.hasText(requestedRole)) {
            if (required) {
                throw validation("role", "REQUIRED", "Role is required");
            }
            return null;
        }
        String roleCode = requestedRole.trim().toUpperCase(Locale.ROOT);
        try {
            RoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            throw validation("role", "INVALID_ROLE", "Role is invalid");
        }
        if (!roleRepository.existsByRoleCodeAndStatus(roleCode, RoleStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.INVALID_PARENT_STATUS, "Role is inactive or not found");
        }
        return roleCode;
    }

    private UserStatus resolveWritableStatus(String requestedStatus, UserStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw validation("status", "INVALID_STATUS", "User status is invalid");
        }
        try {
            UserStatus status = UserStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
            if (status == UserStatus.ACTIVE || status == UserStatus.INACTIVE) {
                return status;
            }
            throw validation("status", "INVALID_STATUS", "User status is invalid");
        } catch (IllegalArgumentException exception) {
            throw validation("status", "INVALID_STATUS", "User status is invalid");
        }
    }

    private Set<UUID> normalizeBranchIds(Set<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return Set.of();
        }
        return branchIds.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertCanReceiveAssignments(UserStatus status, Set<UUID> branchIds) {
        if (status != UserStatus.ACTIVE && branchIds != null && !branchIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Inactive user cannot receive branch assignment");
        }
    }

    private List<Branch> resolveActiveBranches(UUID tenantId, Set<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return List.of();
        }
        List<Branch> branches = branchRepository.findByTenantIdAndIdIn(tenantId, branchIds);
        if (branches.size() != branchIds.size()) {
            throw new ResourceNotFoundException("Branch not found");
        }
        boolean hasInactiveBranch = branches.stream().anyMatch(branch -> branch.status() != BranchStatus.ACTIVE);
        if (hasInactiveBranch) {
            throw new BusinessException(ErrorCode.INVALID_PARENT_STATUS, "Branch is inactive");
        }
        Map<UUID, Branch> branchById = branches.stream()
                .collect(Collectors.toMap(Branch::id, Function.identity()));
        return branchIds.stream().map(branchById::get).toList();
    }

    private List<UserBranch> createAssignments(CurrentUser actor, User user, List<Branch> branches, Instant now) {
        if (branches.isEmpty()) {
            return List.of();
        }
        List<UserBranch> assignments = branches.stream()
                .map(branch -> UserBranch.create(UUID.randomUUID(), user.tenantId(), user.id(), branch.id(), actor.actorId(), now))
                .toList();
        userBranchRepository.saveAll(assignments);
        return assignments;
    }

    private BranchAssignmentChange applyBranchAssignments(
            CurrentUser actor,
            User user,
            List<UserBranch> activeAssignments,
            Set<UUID> requestedBranchIds,
            Instant now) {
        Set<UUID> beforeBranchIds = toBranchIdSet(activeAssignments);
        if (user.status() != UserStatus.ACTIVE) {
            List<UserBranch> inactivatedAssignments = inactivateAssignments(actor, activeAssignments, now);
            Set<UUID> afterBranchIds = Set.of();
            return new BranchAssignmentChange(afterBranchIds, !inactivatedAssignments.isEmpty());
        }
        if (requestedBranchIds == null) {
            return new BranchAssignmentChange(beforeBranchIds, false);
        }
        List<Branch> requestedBranches = resolveActiveBranches(user.tenantId(), requestedBranchIds);
        Set<UUID> requestedIds = requestedBranches.stream()
                .map(Branch::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UserBranch> changedAssignments = new ArrayList<>();
        for (UserBranch assignment : activeAssignments) {
            if (!requestedIds.contains(assignment.branchId())) {
                assignment.inactivate(actor.actorId(), now);
                changedAssignments.add(assignment);
            }
        }
        for (UUID branchId : requestedIds) {
            if (!beforeBranchIds.contains(branchId)) {
                changedAssignments.add(UserBranch.create(UUID.randomUUID(), user.tenantId(), user.id(), branchId, actor.actorId(), now));
            }
        }
        if (!changedAssignments.isEmpty()) {
            userBranchRepository.saveAll(changedAssignments);
        }
        return new BranchAssignmentChange(requestedIds, !changedAssignments.isEmpty());
    }

    private List<UserBranch> inactivateAssignments(CurrentUser actor, List<UserBranch> activeAssignments, Instant now) {
        if (activeAssignments.isEmpty()) {
            return List.of();
        }
        activeAssignments.forEach(assignment -> assignment.inactivate(actor.actorId(), now));
        userBranchRepository.saveAll(activeAssignments);
        return activeAssignments;
    }

    private Set<UUID> findActiveBranchIds(UUID tenantId, UUID userId) {
        return toBranchIdSet(findActiveAssignments(tenantId, userId));
    }

    private List<UserBranch> findActiveAssignments(UUID tenantId, UUID userId) {
        return userBranchRepository.findByTenantIdAndUserIdAndStatus(tenantId, userId, UserBranchStatus.ACTIVE);
    }

    private Map<UUID, List<UUID>> findActiveBranchIdsByUserId(UUID tenantId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userBranchRepository.findByTenantIdAndUserIdInAndStatus(tenantId, userIds, UserBranchStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(
                        UserBranch::userId,
                        LinkedHashMap::new,
                        Collectors.mapping(UserBranch::branchId, Collectors.toList())
                ));
    }

    private Set<UUID> toBranchIdSet(Collection<UserBranch> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Set.of();
        }
        return assignments.stream()
                .map(UserBranch::branchId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void assertEmailAvailableForCreate(UUID tenantId, String email) {
        if (userRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_EMAIL, ErrorCode.DUPLICATE_USER_EMAIL.defaultMessage());
        }
    }

    private int resolvePage(Integer requestedPage) {
        if (requestedPage == null) {
            return DEFAULT_PAGE;
        }
        if (requestedPage < 0) {
            throw validation("page", "MIN_VALUE", "Page must be greater than or equal to 0");
        }
        return requestedPage;
    }

    private int resolveSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_SIZE;
        }
        if (requestedSize < 1 || requestedSize > MAX_SIZE) {
            throw validation("size", "INVALID_SIZE", "Size must be between 1 and 100");
        }
        return requestedSize;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void auditUserChange(
            String action,
            CurrentUser actor,
            User user,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                user.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                AuditResourceTypes.USER,
                user.id(),
                user.email(),
                beforeData,
                afterData,
                auditMetadata(actor),
                reason,
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(CurrentUser actor) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (actor.tenantId() != null) {
            metadata.put("actor_tenant_id", actor.tenantId().toString());
        }
        return metadata;
    }

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }

    private record BranchAssignmentChange(Set<UUID> afterBranchIds, boolean changed) {
    }
}
