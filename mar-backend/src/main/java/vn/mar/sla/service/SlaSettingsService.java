package vn.mar.sla.service;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.tenant.TenantContext;
import vn.mar.common.time.TimeProvider;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.DueTimeCalculationCommand;
import vn.mar.sla.api.DueTimeCalculationResult;
import vn.mar.sla.api.SlaPolicyLookupService;
import vn.mar.sla.api.SlaPolicySnapshot;
import vn.mar.sla.dto.request.SlaPolicyItemRequest;
import vn.mar.sla.dto.request.UpdateSlaPoliciesRequest;
import vn.mar.sla.dto.request.UpdateWorkingHoursRequest;
import vn.mar.sla.dto.request.WorkingHoursDayRequest;
import vn.mar.sla.dto.response.SlaPoliciesResponse;
import vn.mar.sla.dto.response.SlaPolicyItemResponse;
import vn.mar.sla.dto.response.WorkingHoursConfigResponse;
import vn.mar.sla.dto.response.WorkingHoursDayResponse;
import vn.mar.sla.entity.SlaPolicy;
import vn.mar.sla.entity.WorkingHoursConfig;
import vn.mar.sla.model.SlaPolicyStatus;
import vn.mar.sla.model.SlaPolicyType;
import vn.mar.sla.model.WorkingHoursConfigStatus;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@Service
public class SlaSettingsService implements SlaPolicyLookupService {

    private static final Logger log = LoggerFactory.getLogger(SlaSettingsService.class);
    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final int HOT_DUE_MINUTES = 15;
    private static final int NORMAL_DUE_MINUTES = 60;
    private static final int AFTER_HOURS_DUE_MINUTES = 0;
    private static final int MAX_LOOKAHEAD_DAYS = 14;
    private static final String SOURCE_BRANCH = "BRANCH";
    private static final String SOURCE_TENANT = "TENANT";
    private static final String SOURCE_DEFAULT = "DEFAULT";
    private static final String ROLE_SALES_LEAD = "SALES_LEAD";

    private final WorkingHoursConfigRepository workingHoursConfigRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public SlaSettingsService(
            WorkingHoursConfigRepository workingHoursConfigRepository,
            SlaPolicyRepository slaPolicyRepository,
            TenantRepository tenantRepository,
            BranchRepository branchRepository,
            UserBranchRepository userBranchRepository,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            TimeProvider timeProvider) {
        this.workingHoursConfigRepository = workingHoursConfigRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.userBranchRepository = userBranchRepository;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public WorkingHoursConfigResponse getWorkingHours(UUID branchId) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        assertCanViewSlaSettings(actor, branchId);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }
        return toWorkingHoursResponse(loadWorkingHoursPlan(tenantId, branchId));
    }

    @Transactional
    public WorkingHoursConfigResponse updateWorkingHours(UpdateWorkingHoursRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        UUID branchId = request.branchId();
        assertCanManageSlaSettings(actor, branchId);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }

        String timezone = normalizeTimezone(request.timezone());
        Map<DayOfWeek, WorkingHoursDayRequest> requestedDays = validateWorkingHoursDays(request.days());
        Instant now = timeProvider.now();
        List<WorkingHoursConfig> configs = new ArrayList<>();
        for (DayOfWeek weekday : DayOfWeek.values()) {
            WorkingHoursDayRequest day = requestedDays.get(weekday);
            LocalTime startTime = Boolean.TRUE.equals(day.workingDay()) ? day.startTime() : null;
            LocalTime endTime = Boolean.TRUE.equals(day.workingDay()) ? day.endTime() : null;
            WorkingHoursConfig config = findWorkingHoursConfig(tenantId, branchId, weekday)
                    .orElseGet(() -> WorkingHoursConfig.create(
                            deterministicId("working-hours", tenantId, branchId, weekday.name()),
                            tenantId,
                            branchId,
                            weekday,
                            startTime,
                            endTime,
                            timezone,
                            day.workingDay(),
                            actor.actorId(),
                            now
                    ));
            config.update(startTime, endTime, timezone, day.workingDay(), actor.actorId(), now);
            configs.add(config);
        }

        workingHoursConfigRepository.saveAll(configs);
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("tenant_id", tenantId.toString());
        afterData.put("branch_id", branchId == null ? null : branchId.toString());
        afterData.put("timezone", timezone);
        afterData.put("days", configs.stream().map(this::toWorkingHoursAuditData).toList());
        auditSettingsChange(
                actor,
                AuditActions.WORKING_HOURS_UPDATED,
                AuditResourceTypes.WORKING_HOURS_CONFIG,
                branchId,
                afterData
        );
        return toWorkingHoursResponse(loadWorkingHoursPlan(tenantId, branchId));
    }

    @Transactional(readOnly = true)
    public SlaPoliciesResponse getSlaPolicies(UUID branchId) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        assertCanViewSlaSettings(actor, branchId);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }
        return toSlaPoliciesResponse(loadSlaPolicyPlan(tenantId, branchId));
    }

    @Transactional
    public SlaPoliciesResponse updateSlaPolicies(UpdateSlaPoliciesRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = TenantContext.requireTenantId(actor);
        UUID branchId = request.branchId();
        assertCanManageSlaSettings(actor, branchId);
        if (branchId != null) {
            ensureBranchBelongsToTenant(tenantId, branchId);
        }

        String timezone = normalizeTimezone(request.timezone());
        Map<SlaPolicyType, SlaPolicyItemRequest> requestedPolicies = validateSlaPolicies(request.policies());
        Instant now = timeProvider.now();
        List<SlaPolicy> policies = new ArrayList<>();
        for (SlaPolicyType policyType : SlaPolicyType.values()) {
            SlaPolicyItemRequest requestPolicy = requestedPolicies.get(policyType);
            SlaPolicy policy = findSlaPolicy(tenantId, branchId, policyType)
                    .orElseGet(() -> SlaPolicy.create(
                            deterministicId("sla-policy", tenantId, branchId, policyType.name()),
                            tenantId,
                            branchId,
                            policyType,
                            requestPolicy.responseDueMinutes(),
                            timezone,
                            actor.actorId(),
                            now
                    ));
            policy.update(requestPolicy.responseDueMinutes(), timezone, actor.actorId(), now);
            policies.add(policy);
        }

        slaPolicyRepository.saveAll(policies);
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("tenant_id", tenantId.toString());
        afterData.put("branch_id", branchId == null ? null : branchId.toString());
        afterData.put("timezone", timezone);
        afterData.put("policies", policies.stream().map(this::toSlaPolicyAuditData).toList());
        auditSettingsChange(
                actor,
                AuditActions.SLA_POLICY_UPDATED,
                AuditResourceTypes.SLA_POLICY,
                branchId,
                afterData
        );
        return toSlaPoliciesResponse(loadSlaPolicyPlan(tenantId, branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public DueTimeCalculationResult calculateFirstResponseDueAt(DueTimeCalculationCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "SLA calculation command is required");
        }
        UUID tenantId = requireId(command.tenantId(), "tenant_id", "Tenant is required");
        Instant occurredAt = requireInstant(command.occurredAt(), "occurred_at", "Occurred time is required");
        LeadTemperature leadTemperature = command.leadTemperature() == null
                ? LeadTemperature.NORMAL
                : command.leadTemperature();

        WorkingHoursPlan workingHoursPlan = loadWorkingHoursPlan(tenantId, command.branchId());
        SlaPolicyPlan slaPolicyPlan = loadSlaPolicyPlan(tenantId, command.branchId());
        ZoneId zoneId = ZoneId.of(workingHoursPlan.timezone());
        ZonedDateTime localOccurredAt = occurredAt.atZone(zoneId);
        boolean outsideWorkingHours = !isWithinWorkingHours(localOccurredAt, workingHoursPlan);
        boolean afterHoursApplied = outsideWorkingHours || leadTemperature == LeadTemperature.AFTER_HOURS;
        SlaPolicyType policyType = afterHoursApplied
                ? SlaPolicyType.AFTER_HOURS
                : SlaPolicyType.fromLeadTemperature(leadTemperature);
        SlaPolicySnapshot policy = slaPolicyPlan.policies().get(policyType);
        if (policy == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "SLA policy is not available");
        }

        ZonedDateTime calculationStart = normalizeToWorkingStart(localOccurredAt, workingHoursPlan);
        ZonedDateTime dueAt = addWorkingMinutes(calculationStart, policy.responseDueMinutes(), workingHoursPlan);
        return new DueTimeCalculationResult(dueAt.toInstant(), policy, workingHoursPlan.source(), afterHoursApplied);
    }

    private WorkingHoursConfigResponse toWorkingHoursResponse(WorkingHoursPlan plan) {
        List<WorkingHoursDayResponse> days = Arrays.stream(DayOfWeek.values())
                .map(weekday -> {
                    WorkingHoursSlot slot = plan.days().get(weekday);
                    return new WorkingHoursDayResponse(
                            weekday.name(),
                            slot.workingDay(),
                            slot.startTime(),
                            slot.endTime(),
                            slot.source()
                    );
                })
                .toList();
        return new WorkingHoursConfigResponse(
                plan.tenantId(),
                plan.branchId(),
                plan.timezone(),
                plan.source(),
                plan.defaultApplied(),
                days
        );
    }

    private SlaPoliciesResponse toSlaPoliciesResponse(SlaPolicyPlan plan) {
        List<SlaPolicyItemResponse> policies = Arrays.stream(SlaPolicyType.values())
                .map(policyType -> {
                    SlaPolicySnapshot snapshot = plan.policies().get(policyType);
                    return new SlaPolicyItemResponse(
                            snapshot.slaPolicyId(),
                            snapshot.policyType(),
                            snapshot.responseDueMinutes(),
                            snapshot.source()
                    );
                })
                .toList();
        return new SlaPoliciesResponse(
                plan.tenantId(),
                plan.branchId(),
                plan.timezone(),
                plan.source(),
                plan.defaultApplied(),
                policies
        );
    }

    private WorkingHoursPlan loadWorkingHoursPlan(UUID tenantId, UUID branchId) {
        String tenantTimezone = resolveTenantTimezone(tenantId);
        EnumMap<DayOfWeek, WorkingHoursSlot> days = defaultWorkingHours(tenantTimezone);
        boolean defaultApplied = true;
        String source = SOURCE_DEFAULT;
        String timezone = tenantTimezone;

        List<WorkingHoursConfig> tenantConfigs = workingHoursConfigRepository
                .findByTenantIdAndBranchIdIsNullAndStatus(tenantId, WorkingHoursConfigStatus.ACTIVE);
        if (!tenantConfigs.isEmpty()) {
            defaultApplied = tenantConfigs.size() < DayOfWeek.values().length;
            timezone = tenantConfigs.getFirst().timezone();
            overlayWorkingHours(days, tenantConfigs, SOURCE_TENANT);
            source = SOURCE_TENANT;
        } else {
            log.warn("working hours config missing tenantId={} branchId={} usingDefaultPilot=true", tenantId, null);
        }

        if (branchId != null) {
            List<WorkingHoursConfig> branchConfigs = workingHoursConfigRepository
                    .findByTenantIdAndBranchIdAndStatus(tenantId, branchId, WorkingHoursConfigStatus.ACTIVE);
            if (!branchConfigs.isEmpty()) {
                timezone = branchConfigs.getFirst().timezone();
                overlayWorkingHours(days, branchConfigs, SOURCE_BRANCH);
                source = SOURCE_BRANCH;
            }
        }

        return new WorkingHoursPlan(tenantId, branchId, timezone, source, defaultApplied, days);
    }

    private SlaPolicyPlan loadSlaPolicyPlan(UUID tenantId, UUID branchId) {
        String tenantTimezone = resolveTenantTimezone(tenantId);
        EnumMap<SlaPolicyType, SlaPolicySnapshot> policies = defaultSlaPolicies(tenantId, branchId, tenantTimezone);
        boolean defaultApplied = true;
        String source = SOURCE_DEFAULT;
        String timezone = tenantTimezone;

        List<SlaPolicy> tenantPolicies = slaPolicyRepository
                .findByTenantIdAndBranchIdIsNullAndStatus(tenantId, SlaPolicyStatus.ACTIVE);
        if (!tenantPolicies.isEmpty()) {
            defaultApplied = tenantPolicies.size() < SlaPolicyType.values().length;
            timezone = tenantPolicies.getFirst().timezone();
            overlaySlaPolicies(policies, tenantPolicies, SOURCE_TENANT);
            source = SOURCE_TENANT;
        } else {
            log.warn("sla policy missing tenantId={} branchId={} usingDefaultPilot=true", tenantId, null);
        }

        if (branchId != null) {
            List<SlaPolicy> branchPolicies = slaPolicyRepository
                    .findByTenantIdAndBranchIdAndStatus(tenantId, branchId, SlaPolicyStatus.ACTIVE);
            if (!branchPolicies.isEmpty()) {
                timezone = branchPolicies.getFirst().timezone();
                overlaySlaPolicies(policies, branchPolicies, SOURCE_BRANCH);
                source = SOURCE_BRANCH;
            }
        }

        return new SlaPolicyPlan(tenantId, branchId, timezone, source, defaultApplied, policies);
    }

    private EnumMap<DayOfWeek, WorkingHoursSlot> defaultWorkingHours(String timezone) {
        EnumMap<DayOfWeek, WorkingHoursSlot> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek weekday : DayOfWeek.values()) {
            boolean workingDay = weekday != DayOfWeek.SUNDAY;
            days.put(weekday, new WorkingHoursSlot(
                    null,
                    weekday,
                    workingDay,
                    workingDay ? DEFAULT_START_TIME : null,
                    workingDay ? DEFAULT_END_TIME : null,
                    timezone,
                    SOURCE_DEFAULT
            ));
        }
        return days;
    }

    private EnumMap<SlaPolicyType, SlaPolicySnapshot> defaultSlaPolicies(UUID tenantId, UUID branchId, String timezone) {
        EnumMap<SlaPolicyType, SlaPolicySnapshot> policies = new EnumMap<>(SlaPolicyType.class);
        policies.put(SlaPolicyType.HOT, defaultPolicy(tenantId, branchId, SlaPolicyType.HOT, HOT_DUE_MINUTES, timezone));
        policies.put(SlaPolicyType.NORMAL, defaultPolicy(tenantId, branchId, SlaPolicyType.NORMAL, NORMAL_DUE_MINUTES, timezone));
        policies.put(SlaPolicyType.AFTER_HOURS, defaultPolicy(tenantId, branchId, SlaPolicyType.AFTER_HOURS, AFTER_HOURS_DUE_MINUTES, timezone));
        return policies;
    }

    private SlaPolicySnapshot defaultPolicy(UUID tenantId, UUID branchId, SlaPolicyType policyType, int minutes, String timezone) {
        return new SlaPolicySnapshot(null, tenantId, branchId, policyType.name(), minutes, timezone, SOURCE_DEFAULT);
    }

    private void overlayWorkingHours(
            EnumMap<DayOfWeek, WorkingHoursSlot> days,
            List<WorkingHoursConfig> configs,
            String source) {
        for (WorkingHoursConfig config : configs) {
            days.put(config.weekday(), new WorkingHoursSlot(
                    config.id(),
                    config.weekday(),
                    config.workingDay(),
                    config.startTime(),
                    config.endTime(),
                    config.timezone(),
                    source
            ));
        }
    }

    private void overlaySlaPolicies(
            EnumMap<SlaPolicyType, SlaPolicySnapshot> policies,
            List<SlaPolicy> entities,
            String source) {
        for (SlaPolicy policy : entities) {
            policies.put(policy.policyType(), new SlaPolicySnapshot(
                    policy.id(),
                    policy.tenantId(),
                    policy.branchId(),
                    policy.policyType().name(),
                    policy.responseDueMinutes(),
                    policy.timezone(),
                    source
            ));
        }
    }

    private boolean isWithinWorkingHours(ZonedDateTime time, WorkingHoursPlan plan) {
        WorkingHoursSlot slot = plan.days().get(time.getDayOfWeek());
        if (slot == null || !slot.workingDay()) {
            return false;
        }
        LocalTime localTime = time.toLocalTime();
        return !localTime.isBefore(slot.startTime()) && localTime.isBefore(slot.endTime());
    }

    private ZonedDateTime normalizeToWorkingStart(ZonedDateTime time, WorkingHoursPlan plan) {
        if (isWithinWorkingHours(time, plan)) {
            return time;
        }
        return nextWorkingStart(time, plan);
    }

    private ZonedDateTime addWorkingMinutes(ZonedDateTime start, int minutes, WorkingHoursPlan plan) {
        ZonedDateTime cursor = normalizeToWorkingStart(start, plan);
        int remainingMinutes = minutes;
        while (remainingMinutes > 0) {
            WorkingHoursSlot slot = plan.days().get(cursor.getDayOfWeek());
            ZonedDateTime dayEnd = cursor.toLocalDate().atTime(slot.endTime()).atZone(cursor.getZone());
            long availableMinutes = Duration.between(cursor, dayEnd).toMinutes();
            if (remainingMinutes <= availableMinutes) {
                return cursor.plusMinutes(remainingMinutes);
            }
            remainingMinutes -= (int) availableMinutes;
            cursor = nextWorkingStart(dayEnd.plusNanos(1), plan);
        }
        return cursor;
    }

    private ZonedDateTime nextWorkingStart(ZonedDateTime time, WorkingHoursPlan plan) {
        for (int offset = 0; offset <= MAX_LOOKAHEAD_DAYS; offset++) {
            LocalDate date = time.toLocalDate().plusDays(offset);
            WorkingHoursSlot slot = plan.days().get(date.getDayOfWeek());
            if (slot == null || !slot.workingDay()) {
                continue;
            }
            ZonedDateTime start = date.atTime(slot.startTime()).atZone(time.getZone());
            ZonedDateTime end = date.atTime(slot.endTime()).atZone(time.getZone());
            if (time.isBefore(start) || time.isEqual(start)) {
                return start;
            }
            if (time.isBefore(end)) {
                return time;
            }
        }
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "No working day is available for SLA calculation");
    }

    private Map<DayOfWeek, WorkingHoursDayRequest> validateWorkingHoursDays(List<WorkingHoursDayRequest> days) {
        if (days == null || days.size() != DayOfWeek.values().length) {
            throw ValidationException.of("days", "INVALID_SIZE", "Exactly seven working-hour days are required");
        }
        Map<DayOfWeek, WorkingHoursDayRequest> byWeekday = new EnumMap<>(DayOfWeek.class);
        for (WorkingHoursDayRequest day : days) {
            DayOfWeek weekday = parseWeekday(day.weekday());
            if (byWeekday.putIfAbsent(weekday, day) != null) {
                throw ValidationException.of("days", "DUPLICATE_WEEKDAY", "Weekday must be unique");
            }
            validateWorkingHoursDay(day, weekday);
        }
        for (DayOfWeek weekday : DayOfWeek.values()) {
            if (!byWeekday.containsKey(weekday)) {
                throw ValidationException.of("days", "MISSING_WEEKDAY", "Every weekday must be configured");
            }
        }
        return byWeekday;
    }

    private void validateWorkingHoursDay(WorkingHoursDayRequest day, DayOfWeek weekday) {
        if (Boolean.TRUE.equals(day.workingDay())) {
            if (day.startTime() == null || day.endTime() == null) {
                throw ValidationException.of(weekday.name().toLowerCase(Locale.ROOT), "WORKING_TIME_REQUIRED", "Working day requires start time and end time");
            }
            if (!day.startTime().isBefore(day.endTime())) {
                throw ValidationException.of(weekday.name().toLowerCase(Locale.ROOT), "INVALID_TIME_RANGE", "Start time must be before end time");
            }
        }
    }

    private Map<SlaPolicyType, SlaPolicyItemRequest> validateSlaPolicies(List<SlaPolicyItemRequest> policies) {
        if (policies == null || policies.size() != SlaPolicyType.values().length) {
            throw ValidationException.of("policies", "INVALID_SIZE", "Exactly three SLA policies are required");
        }
        Map<SlaPolicyType, SlaPolicyItemRequest> byType = new EnumMap<>(SlaPolicyType.class);
        for (SlaPolicyItemRequest policy : policies) {
            SlaPolicyType policyType = parseSlaPolicyType(policy.policyType());
            if (policy.responseDueMinutes() == null) {
                throw ValidationException.of(policyType.name().toLowerCase(Locale.ROOT), "REQUIRED", "Response due minutes are required");
            }
            if (byType.putIfAbsent(policyType, policy) != null) {
                throw ValidationException.of("policies", "DUPLICATE_POLICY_TYPE", "SLA policy type must be unique");
            }
        }
        for (SlaPolicyType policyType : SlaPolicyType.values()) {
            if (!byType.containsKey(policyType)) {
                throw ValidationException.of("policies", "MISSING_POLICY_TYPE", "Every SLA policy type must be configured");
            }
        }
        return byType;
    }

    private Optional<WorkingHoursConfig> findWorkingHoursConfig(UUID tenantId, UUID branchId, DayOfWeek weekday) {
        return branchId == null
                ? workingHoursConfigRepository.findByTenantIdAndBranchIdIsNullAndWeekdayAndStatus(
                        tenantId,
                        weekday,
                        WorkingHoursConfigStatus.ACTIVE)
                : workingHoursConfigRepository.findByTenantIdAndBranchIdAndWeekdayAndStatus(
                        tenantId,
                        branchId,
                        weekday,
                        WorkingHoursConfigStatus.ACTIVE);
    }

    private Optional<SlaPolicy> findSlaPolicy(UUID tenantId, UUID branchId, SlaPolicyType policyType) {
        return branchId == null
                ? slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndPolicyTypeAndStatus(
                        tenantId,
                        policyType,
                        SlaPolicyStatus.ACTIVE)
                : slaPolicyRepository.findByTenantIdAndBranchIdAndPolicyTypeAndStatus(
                        tenantId,
                        branchId,
                        policyType,
                        SlaPolicyStatus.ACTIVE);
    }

    private DayOfWeek parseWeekday(String value) {
        if (!StringUtils.hasText(value)) {
            throw ValidationException.of("weekday", "REQUIRED", "Weekday is required");
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("weekday", "INVALID", "Weekday is invalid");
        }
    }

    private SlaPolicyType parseSlaPolicyType(String value) {
        if (!StringUtils.hasText(value)) {
            throw ValidationException.of("policy_type", "REQUIRED", "SLA policy type is required");
        }
        try {
            return SlaPolicyType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("policy_type", "INVALID", "SLA policy type is invalid");
        }
    }

    private String normalizeTimezone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            throw ValidationException.of("timezone", "REQUIRED", "Timezone is required");
        }
        String normalized = timezone.trim();
        try {
            ZoneId.of(normalized);
            return normalized;
        } catch (RuntimeException exception) {
            throw ValidationException.of("timezone", "INVALID", "Timezone is invalid");
        }
    }

    private String resolveTenantTimezone(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("tenant_id", "Tenant was not found"));
        return StringUtils.hasText(tenant.timezone()) ? tenant.timezone().trim() : DEFAULT_TIMEZONE;
    }

    private void ensureBranchBelongsToTenant(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("branch_id", "Branch was not found"));
    }

    private void assertCanViewSlaSettings(CurrentUser actor, UUID branchId) {
        if (!actor.hasPermission(PermissionCodes.SLA_VIEW) && !actor.hasPermission(PermissionCodes.SLA_MANAGE)) {
            throw BusinessException.forbidden("permission", "MISSING_PERMISSION", "SLA view permission is required");
        }
        assertBranchScope(actor, branchId, false);
    }

    private void assertCanManageSlaSettings(CurrentUser actor, UUID branchId) {
        if (!actor.hasPermission(PermissionCodes.SLA_MANAGE)) {
            throw BusinessException.forbidden("permission", "MISSING_PERMISSION", "SLA manage permission is required");
        }
        assertBranchScope(actor, branchId, true);
    }

    private void assertBranchScope(CurrentUser actor, UUID branchId, boolean managing) {
        if (!ROLE_SALES_LEAD.equals(actor.roleCode())) {
            return;
        }
        if (branchId == null) {
            String message = managing
                    ? "Sales Lead can only manage branch SLA settings"
                    : "Sales Lead must specify a branch to view SLA settings";
            throw BusinessException.forbidden("branch_id", "BRANCH_SCOPE_REQUIRED", message);
        }
        boolean assigned = userBranchRepository
                .findByTenantIdAndUserIdAndStatus(actor.tenantId(), actor.actorId(), UserBranchStatus.ACTIVE)
                .stream()
                .anyMatch(userBranch -> branchId.equals(userBranch.branchId()));
        if (!assigned) {
            throw BusinessException.forbidden("branch_id", "OUT_OF_SCOPE", "Branch is outside the actor scope");
        }
    }


    private UUID requireId(UUID id, String field, String message) {
        if (id == null) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return id;
    }

    private Instant requireInstant(Instant instant, String field, String message) {
        if (instant == null) {
            throw ValidationException.of(field, "REQUIRED", message);
        }
        return instant;
    }

    private UUID deterministicId(String prefix, UUID tenantId, UUID branchId, String key) {
        String raw = prefix + ":" + tenantId + ":" + (branchId == null ? "TENANT" : branchId) + ":" + key;
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> toWorkingHoursAuditData(WorkingHoursConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("working_hours_id", config.id().toString());
        data.put("weekday", config.weekday().name());
        data.put("working_day", config.workingDay());
        data.put("start_time", config.startTime() == null ? null : config.startTime().toString());
        data.put("end_time", config.endTime() == null ? null : config.endTime().toString());
        data.put("timezone", config.timezone());
        return data;
    }

    private Map<String, Object> toSlaPolicyAuditData(SlaPolicy policy) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sla_policy_id", policy.id().toString());
        data.put("policy_type", policy.policyType().name());
        data.put("response_due_minutes", policy.responseDueMinutes());
        data.put("timezone", policy.timezone());
        return data;
    }

    private void auditSettingsChange(
            CurrentUser actor,
            String action,
            String resourceType,
            UUID branchId,
            Map<String, Object> afterData) {
        UUID resourceId = branchId == null ? actor.tenantId() : branchId;
        String resourceKey = branchId == null
                ? actor.tenantId().toString()
                : actor.tenantId() + ":" + branchId;
        auditService.record(new AuditRecordCommand(
                actor.tenantId(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                resourceType,
                resourceId,
                resourceKey,
                null,
                afterData,
                Map.of("actor_tenant_id", actor.tenantId().toString()),
                null,
                LogContext.requestId()
        ));
    }




    private record WorkingHoursSlot(
            UUID id,
            DayOfWeek weekday,
            boolean workingDay,
            LocalTime startTime,
            LocalTime endTime,
            String timezone,
            String source
    ) {
    }

    private record WorkingHoursPlan(
            UUID tenantId,
            UUID branchId,
            String timezone,
            String source,
            boolean defaultApplied,
            EnumMap<DayOfWeek, WorkingHoursSlot> days
    ) {
    }

    private record SlaPolicyPlan(
            UUID tenantId,
            UUID branchId,
            String timezone,
            String source,
            boolean defaultApplied,
            EnumMap<SlaPolicyType, SlaPolicySnapshot> policies
    ) {
    }

}
