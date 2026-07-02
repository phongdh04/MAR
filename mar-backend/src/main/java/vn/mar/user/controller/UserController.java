package vn.mar.user.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;
import vn.mar.user.dto.request.CreateUserRequest;
import vn.mar.user.dto.request.UpdateUserRequest;
import vn.mar.user.dto.request.UserSearchRequest;
import vn.mar.user.dto.response.UserDetailResponse;
import vn.mar.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasPermission(authentication, 'user.manage')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.createUser(request)));
    }

    @GetMapping
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'user.manage', 'user.view')")
    public ResponseEntity<ApiResponse<PageResponse<UserDetailResponse>>> searchUsers(
            UserSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(request)));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'user.manage', 'user.view')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'user.manage')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, request)));
    }
}
