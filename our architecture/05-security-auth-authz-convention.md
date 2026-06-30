# SECURITY, AUTHENTICATION & AUTHORIZATION CONVENTION - QUY TẮC BẢO MẬT MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Spring Security 6.x, Spring Boot 3.5.x, BCrypt, JWT/Bearer token option, PostgreSQL  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Pattern API security OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\auth_convention.md` - Pattern authentication
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\authz_convention.md` - Pattern authorization
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\user_management_convention.md` - Pattern user management
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\audit_convention.md` - Pattern security audit

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa bảo mật cho MAR, bao gồm authentication, authorization, tenant context, permission matrix, security headers, CORS, token/password handling và security audit.

Mục đích:

- Đảm bảo mọi API được bảo vệ theo default deny.
- Tách rõ authentication, authorization và tenant isolation.
- Không phụ thuộc frontend để enforce quyền.
- Chuẩn hóa permission code để map với BA docs và test QA.
- Ngăn leak password, token, tenant data và thông tin tuyển sinh nhạy cảm.

Nguyên tắc ghi nhớ:

> **"Frontend có thể ẩn nút; backend mới là nơi khóa cửa."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Spring Security filter chain.
- Login/authentication nếu MAR tự quản lý auth.
- JWT/Bearer token validation nếu MAR dùng token.
- Authorization service và method-level permission.
- Tenant context/current user context.
- User/role/permission management.
- Audit cho security event.
- Security tests cho controller/service.

Không áp dụng cho:

- Frontend XSS/CSP implementation chi tiết.
- Production secret manager cụ thể.
- External identity provider final nếu chưa chốt.
- SSO/OAuth2 enterprise flow ngoài scope Sprint 1.

## 3. NGUYÊN TẮC CHUNG

1. **Default deny:** endpoint không khai báo public thì phải được bảo vệ.
2. **Authenticate before business logic:** không chạy use case khi chưa có identity hợp lệ.
3. **Authorize every protected action:** mỗi action nghiệp vụ phải có permission rõ.
4. **Tenant isolation is mandatory:** quyền đúng nhưng sai tenant vẫn bị chặn.
5. **Least privilege:** role chỉ có quyền cần thiết.
6. **No UI-only security:** ẩn menu/nút không thay thế backend permission.
7. **No sensitive logging:** không log password, token, secret, OTP, raw file chứa PII.
8. **Audit security events:** login, permission denied, role change, user status change phải audit.
9. **Secret from environment:** không hard-code secret trong code/config commit.
10. **Fail closed:** khi không đọc được permission/context, reject request thay vì cho qua.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Package/class naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Security config | `<Topic>SecurityConfig` | `ApiSecurityConfig` |
| Password config | `PasswordEncoderConfig` | `PasswordEncoderConfig` |
| JWT provider | `JwtTokenProvider` | `JwtTokenProvider` |
| JWT filter | `JwtAuthenticationFilter` | `JwtAuthenticationFilter` |
| Current user | `CurrentUserContext` | `SpringSecurityCurrentUserContext` |
| Authz service | `AuthorizationService` | `PermissionAuthorizationService` |
| Permission evaluator | `PermissionEvaluator` | `MarPermissionEvaluator` |
| Access denied handler | `AccessDeniedHandler` | `ApiAccessDeniedHandler` |
| Auth entry point | `AuthenticationEntryPoint` | `ApiAuthenticationEntryPoint` |
| Audit service | `SecurityAuditService` | `SecurityAuditService` |

### 4.2. Permission code naming

Pattern:

```text
<resource>.<action>
```

Examples:

```text
tenant.view
tenant.manage
branch.view
branch.manage
user.view
user.manage
role.manage
permission.manage
catalog.view
catalog.manage
import.view
import.manage
audit.view
```

Rules:

- Lowercase.
- Dùng dấu chấm để phân tách resource/action.
- Không dùng label UI làm permission code.
- Permission code phải ổn định để audit/report/test dùng được.

### 4.3. Role code naming

Role code dùng UPPER_SNAKE_CASE:

```text
CEO
ADMIN
MARKETING
SALES_LEAD
ADVISOR
CSKH
FINANCE
```

Role label có thể hiển thị theo ngôn ngữ sau, nhưng role code không đổi tùy ngôn ngữ.

### 4.4. Method naming

| Mục đích | Method | Ví dụ |
|---|---|---|
| Lấy current user | `currentUser` | `currentUser()` |
| Lấy tenant | `currentTenantId` | `currentTenantId()` |
| Check một quyền | `hasPermission` | `hasPermission("branch.manage")` |
| Check nhiều quyền | `hasAnyPermission` | `hasAnyPermission(List.of(...))` |
| Assert quyền | `requirePermission` | `requirePermission("user.manage")` |
| Assert tenant | `requireTenantAccess` | `requireTenantAccess(tenantId)` |
| Audit denied | `recordPermissionDenied` | `recordPermissionDenied(...)` |

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Security package

```text
vn.mar.security
├── config
│   ├── ApiSecurityConfig.java
│   ├── CorsConfig.java
│   └── PasswordEncoderConfig.java
├── context
│   ├── CurrentUserContext.java
│   ├── CurrentUser.java
│   └── SpringSecurityCurrentUserContext.java
├── filter
│   ├── JwtAuthenticationFilter.java
│   ├── RequestIdFilter.java
│   └── SecurityHeadersFilter.java
├── handler
│   ├── ApiAccessDeniedHandler.java
│   └── ApiAuthenticationEntryPoint.java
└── jwt
    ├── JwtProperties.java
    └── JwtTokenProvider.java
```

### 5.2. Authentication package

```text
vn.mar.auth
├── controller
├── dto
│   ├── request
│   └── response
├── service
├── token
└── validator
```

Chỉ dùng nếu MAR tự quản lý login/password/token. Nếu dùng external identity provider, package `auth` chỉ giữ adapter để map identity vào `CurrentUser`.

### 5.3. Authorization package

```text
vn.mar.authz
├── annotation
├── evaluator
├── model
├── repository
└── service
```

Ví dụ:

```text
vn.mar.authz.service.PermissionAuthorizationService
vn.mar.authz.evaluator.MarPermissionEvaluator
vn.mar.authz.model.PermissionCode
```

### 5.4. User/role/permission tables

Các bảng thuộc user/security foundation:

```text
users
roles
permissions
permission_profiles
user_branches
```

Chốt Sprint 1:

- `permission_profiles` là source of truth cho tenant permission matrix.
- `permissions` có thể là global function-code catalog.
- `roles` là seed/lookup nếu có `/api/v1/roles`.
- Không implement `role_permissions` trong Sprint 1 nếu Tech Lead không approve rõ.
- Tất cả table tenant-scoped phải có `tenant_id`, trừ khi là permission catalog platform-level được SA chốt là global.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Security filter chain pattern

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

Rules:

- REST Bearer token API dùng stateless session.
- CSRF disabled cho stateless Bearer token API.
- Public endpoint phải liệt kê rõ.
- `anyRequest().authenticated()` là default.
- `ApiAuthenticationEntryPoint` format lỗi `401` phát sinh trong Spring Security filter chain.
- `ApiAccessDeniedHandler` format lỗi `403` phát sinh trong Spring Security filter chain.
- Cả hai handler phải dùng cùng `ErrorResponseFactory`/error DTO contract với `GlobalExceptionHandler`.

### 6.1.1. Request id filter pattern

`RequestIdFilter` là bắt buộc và phải chạy sớm trong filter chain:

```text
Incoming header: X-Request-Id
Response header: X-Request-Id
MDC key: requestId
Error meta: meta.request_id
```

Rules:

- Nếu client gửi `X-Request-Id` hợp lệ, backend propagate lại.
- Nếu thiếu hoặc invalid, backend generate request id mới.
- Request id được lưu vào MDC/request context trước khi security handler có thể trả `401/403`.
- Security audit event và error response phải dùng cùng request id.

### 6.2. Current user context pattern

```java
public record CurrentUser(
        UUID actorId,
        UUID tenantId,
        String roleCode,
        Set<String> permissionCodes,
        String requestId
) {
    public boolean hasPermission(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }
}
```

```java
public interface CurrentUserContext {

    CurrentUser currentUser();

    UUID currentTenantId();

    UUID currentActorId();
}
```

Request authenticated phải có:

- `actor_id`
- `tenant_id`
- `role_code`
- permission profile resolvable từ DB/cache
- `request_id`

`permissionCodes` trong `CurrentUser` là quyền đã resolve bởi backend từ DB/cache tại thời điểm request, không phải permission list dài hạn được tin tuyệt đối từ JWT.

### 6.3. JWT pattern

Nếu MAR tự phát hành JWT:

Header:

```text
Authorization: Bearer <token>
```

Claims tối thiểu:

```json
{
  "sub": "actor uuid",
  "tenant_id": "tenant uuid",
  "role_code": "ADMIN",
  "iat": 1782800000,
  "exp": 1782803600
}
```

Rules:

- Validate signature, expiration, issuer/audience nếu có.
- Secret/private key lấy từ environment/secret manager.
- Không log raw token.
- Token expiration phải có.
- Sprint 1 JWT chỉ chứa `actor_id`/`sub`, `tenant_id`, `role_code` và metadata cần thiết.
- Không nhúng permission list dài hạn vào JWT làm source of truth.
- Permission codes được resolve từ `permission_profiles` qua DB/cache mỗi request hoặc TTL ngắn.
- Permission matrix changes không phải chờ token expiration mới có hiệu lực.
- Nếu sau này muốn nhúng permission vào JWT, phải có `permission_version` hoặc token revocation/versioning decision.

### 6.4. Password pattern

Nếu MAR lưu password:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

Rules:

- Không lưu raw password.
- Không dùng MD5/SHA-1/SHA-256 raw cho password.
- Password reset/activation token chỉ hiển thị một lần.
- Token reset lưu dạng hash.
- Login failure phải có rate limit/lockout policy trước production.

Nếu dùng external identity provider:

- Không lưu password local.
- Chỉ lưu local profile, tenant, branch, role/permission mapping.

### 6.5. Authorization service pattern

```java
@Service("authz")
@RequiredArgsConstructor
public class PermissionAuthorizationService {

    private final CurrentUserContext currentUserContext;

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        CurrentUser user = currentUserContext.currentUser();
        return user.hasPermission(permissionCode);
    }

    public void requirePermission(String permissionCode) {
        if (!currentUserContext.currentUser().hasPermission(permissionCode)) {
            throw new AccessDeniedException("PERMISSION_DENIED");
        }
    }
}
```

Controller:

```java
@PreAuthorize("@authz.hasPermission(authentication, 'branch.manage')")
@PostMapping("/api/v1/branches")
public ApiResponse<BranchDetailResponse> createBranch(
        @Valid @RequestBody CreateBranchRequest request) {
    return ApiResponse.success(branchService.createBranch(request));
}
```

### 6.6. Tenant isolation pattern

Service phải query theo tenant:

```java
@Transactional(readOnly = true)
public BranchDetailResponse getBranch(UUID branchId) {
    UUID tenantId = currentUserContext.currentTenantId();
    Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("BRANCH_NOT_FOUND"));
    return branchMapper.toDetailResponse(branch);
}
```

Rules:

- Không tin `tenant_id` từ request body của normal user.
- Platform admin endpoint phải có permission riêng.
- Direct-id cross-tenant access mặc định trả `404` để giảm resource enumeration.

### 6.7. CORS pattern

```java
@Bean
CorsConfigurationSource corsConfigurationSource(MarCorsProperties properties) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(properties.allowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    config.setExposedHeaders(List.of("X-Request-Id"));
    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Rules:

- Không dùng wildcard origin cho production.
- Không dùng `allowCredentials=true` với origin `*`.
- CORS config theo environment.
- Local dev chỉ allow localhost frontend origins đã biết.
- Production allowed origins phải lấy từ environment config.

### 6.8. Security audit pattern

Security event cần audit:

- Login success/failure.
- Logout nếu có.
- Permission denied.
- Cross-tenant access attempt.
- Role changed.
- Permission matrix changed.
- User status changed.
- Password reset/invite token generated nếu MAR owns auth.

Audit event không được chứa raw password/token.

## 7. QUY TẮC RIÊNG CỦA MAR SECURITY

### 7.1. Authentication baseline Sprint 1

Sprint 1 default:

```text
Local JWT unless an external identity provider is confirmed before kickoff.
```

Hai hướng vẫn phải giữ cùng request context:

| Option | Dùng khi | Rule |
|---|---|---|
| Local JWT | MAR cần tự login nhanh cho MVP | Bearer token, BCrypt password, permission load từ DB/cache |
| External auth adapter | Tổ chức đã có identity provider trước kickoff | Adapter map identity sang `CurrentUser` |

Nếu Local JWT:

- R1A technical bootstrap phải có login, password hashing, JWT issue/validate, `CurrentUserContext`, seed admin user.
- Login success/failure phải audit nếu auth flow được implement.
- Rate limit/lockout là P1 trước production, nhưng không được log raw password/token.

Nếu external auth adapter:

- Bootstrap phải implement mapping external identity -> `CurrentUser`.
- Adapter vẫn phải resolve tenant, role, permission profile và request id theo cùng shape.

### 7.2. Role baseline

Role gốc theo nghiệp vụ tuyển sinh:

| Role | Ý nghĩa |
|---|---|
| `CEO` | Xem tổng quan, quyền quản trị cao theo tenant |
| `ADMIN` | Quản trị hệ thống tenant |
| `MARKETING` | Quản lý nguồn lead/campaign nếu vào scope |
| `SALES_LEAD` | Quản lý đội tư vấn |
| `ADVISOR` | Tư vấn/nhập xử lý lead |
| `CSKH` | Chăm sóc khách hàng/học viên |
| `FINANCE` | Xem/xử lý thông tin tài chính theo scope |

Không hard-code role bypass kiểu `if role == ADMIN return true` trong business service. Permission matrix phải là nguồn quyết định.

### 7.3. Permission baseline

Permission Sprint 1 tối thiểu:

```text
tenant.view
tenant.manage
branch.view
branch.manage
user.view
user.manage
role.view
role.manage
permission.view
permission.manage
catalog.view
catalog.manage
import.view
import.manage
audit.view
```

`manage` có thể bao gồm create/update/deactivate tùy matrix BA chốt, nhưng phải document rõ.

Permission source of truth:

```text
permission_profiles(tenant_id, role_code, function_code, access_level, scope)
```

Permission code registry:

- Sprint 1 import foundation dùng `import.view` và `import.manage`.
- `lead.import` chỉ dùng khi official lead import preview/confirm hoặc lead pipeline vào scope.
- Không dùng đồng thời `import.manage` và `lead.import` cho cùng một hành động.
- Future R1A permissions như `duplicate.manage`, `opportunity.update`, `assignment.manage`, `sla.view_update` phải được thêm vào registry khi scope chính thức mở.

### 7.4. Cross-tenant policy

| Case | Response |
|---|---|
| Chưa authenticated | `401 UNAUTHENTICATED` |
| Có identity nhưng thiếu permission | `403 PERMISSION_DENIED` |
| Direct-id resource thuộc tenant khác | `404 NOT_FOUND` mặc định |
| Platform admin thiếu platform permission | `403 PERMISSION_DENIED` |

### 7.4.1. Platform admin/bootstrap tenant creation

Tenant creation là platform-level hoặc bootstrap-only:

- `POST /api/v1/tenants` không chạy bằng tenant context của tenant user thông thường.
- Normal tenant user không được tạo arbitrary tenant.
- Nếu có platform admin thật, phải có platform permission riêng, ví dụ `platform.tenant.manage`.
- Nếu chưa có platform admin UI, tenant đầu tiên/admin đầu tiên được tạo qua bootstrap/seed runner có kiểm soát.

### 7.5. Cache permission

Permission có thể cache để giảm query, nhưng:

- TTL ngắn hoặc có invalidation khi đổi permission.
- Không cache vĩnh viễn.
- Khi không load được permission, fail closed.
- Role/permission change phải audit.
- Permission cache key phải có `tenant_id + role_code` hoặc permission profile version.
- Permission matrix update phải evict cache ngay.

### 7.6. Public endpoint policy

Endpoint public phải nằm trong allowlist:

- Login.
- Health check.
- Password reset request nếu MAR owns auth.
- Activation/invitation verify nếu MAR owns auth.

Không public list/detail business data.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - protected controller

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("@authz.hasPermission(authentication, 'user.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
```

### 8.2. Good example - tenant-safe service

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final CurrentUserContext currentUserContext;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDetailResponse getUser(UUID userId) {
        UUID tenantId = currentUserContext.currentTenantId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));
        return userMapper.toDetailResponse(user);
    }
}
```

### 8.3. Bad example - insecure shortcut

```java
@GetMapping("/api/v1/users/{id}")
public User getUser(@PathVariable UUID id, @RequestParam UUID tenantId) {
    return userRepository.findById(id).orElseThrow();
}
```

Vấn đề:

- Trả entity trực tiếp.
- Nhận `tenantId` từ request thay vì context.
- Query chỉ bằng id.
- Không có permission.
- Không chuẩn hóa not found/error.

### 8.4. Bad example - token/password leak

```java
log.info("Login request email={}, password={}", email, password);
log.info("Authorization header={}", authorizationHeader);
```

Vấn đề:

- Log raw password.
- Log raw token.
- Log có thể bị đọc bởi người không có quyền security.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Dùng `permitAll` rộng cho `/api/**`.
- Hard-code admin bypass trong service.
- Tin role/tenant từ request body/header tự chế.
- Bỏ permission check vì frontend đã ẩn nút.
- Log JWT/password/token/secret.
- Lưu raw password hoặc hash yếu.
- JWT không có expiration.
- Hard-code JWT secret trong repo.
- Cache permission không hết hạn.
- Trả `403`/`404` không nhất quán với policy đã chốt.
- Tạo platform admin endpoint mà không có platform permission riêng.
- Public endpoint business data.
- Tin permission list dài hạn trong JWT sau khi permission matrix đã đổi.
- Implement `role_permissions` song song với `permission_profiles` khi chưa có approval.
- Trả `401/403` khác error envelope chuẩn.
- Ghi security audit event thiếu request id.

## 10. TESTING CONVENTIONS

### 10.1. Controller security test

Mỗi protected endpoint cần test:

- Không token -> `401`.
- Token hợp lệ nhưng thiếu permission -> `403`.
- Token hợp lệ đủ permission -> success.
- Cross-tenant direct id -> `404` mặc định.
- Request public endpoint vẫn truy cập được.
- `401/403` từ security filter chain trả cùng error envelope với `GlobalExceptionHandler`.
- `401/403` có `meta.request_id` và response header `X-Request-Id`.

### 10.2. Authz service test

Test:

- `hasPermission` trả true khi user có permission.
- Trả false khi thiếu permission.
- Fail closed khi current user không hợp lệ.
- Permission cache invalidation nếu có cache.
- Permission matrix update làm quyền mới có hiệu lực không cần chờ JWT hết hạn.

### 10.3. Tenant isolation test

Mỗi service detail/update/delete tenant-scoped cần test:

- Record đúng tenant được xử lý.
- Record khác tenant bị chặn.
- Không gọi repository method chỉ có `id` nếu use case tenant-scoped.

### 10.4. Password/token test

Nếu MAR owns auth:

- Password hash bằng BCrypt.
- Raw password không lưu DB.
- Login fail tăng fail counter/trigger lockout nếu đã implement.
- JWT hết hạn bị reject.
- JWT signature sai bị reject.
- JWT không chứa permission list dài hạn làm source of truth.
- Permission được resolve từ DB/cache theo `tenant_id + role_code`.
- Token reset lưu hash, không lưu raw.

### 10.5. Security audit test

Test security audit event cho:

- Permission denied.
- Role/permission change.
- User status change.
- Login failure nếu auth implemented.
- Security audit event có request id.
- Permission denied audit không chứa raw token/request body nhạy cảm.

## 11. CODE REVIEW CHECKLIST

- [ ] Endpoint có quyết định public/protected rõ.
- [ ] Protected endpoint có permission code.
- [ ] Permission code đúng convention `<resource>.<action>`.
- [ ] Backend enforce permission, không chỉ frontend.
- [ ] Tenant isolation được enforce ở service/repository.
- [ ] Cross-tenant case có test.
- [ ] Unauthorized trả `401`.
- [ ] Forbidden trả `403`.
- [ ] Direct-id cross-tenant trả theo policy.
- [ ] Không log password/token/secret.
- [ ] JWT secret/password config không hard-code.
- [ ] Sprint 1 auth mode chốt Local JWT hoặc external adapter trước khi code.
- [ ] JWT không nhúng permission list dài hạn làm source of truth.
- [ ] Permission resolve từ `permission_profiles`/DB/cache.
- [ ] Permission matrix update evict cache hoặc bump permission version.
- [ ] `RequestIdFilter` chạy trước security error handlers.
- [ ] `401/403` từ filter chain dùng cùng ErrorResponse factory với GlobalExceptionHandler.
- [ ] Platform-level tenant creation không dùng tenant user context.
- [ ] Permission code không lẫn `import.manage` và `lead.import` cho cùng action.
- [ ] Password dùng BCrypt nếu MAR lưu password.
- [ ] CORS whitelist theo environment.
- [ ] CSRF policy phù hợp stateless Bearer API.
- [ ] Security event quan trọng được audit.
- [ ] Permission cache có TTL/invalidation nếu dùng.

## 12. TÀI LIỆU LIÊN QUAN

- `03-rest-api-convention.md` - API status/error/permission annotation.
- `04-database-flyway-convention.md` - User/role/permission table và tenant_id.
- `06-exception-error-i18n-convention.md` - Error code `UNAUTHENTICATED`, `PERMISSION_DENIED`.
- `07-logging-observability-convention.md` - Masking log và request id.
- `08-audit-convention.md` - Security audit events.
- `09-testing-quality-convention.md` - Security test gates.
- OASIS reference: `api_security_convention.md`, `auth_convention.md`, `authz_convention.md`, `user_management_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Chốt Local JWT default nếu chưa có IdP, permission resolve từ `permission_profiles`, request-id/security error handler ownership, platform tenant creation, permission code registry và cache invalidation rules. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa security/auth/authz convention theo pattern OASIS, map sang REST API và tenant permission model của MAR. |
