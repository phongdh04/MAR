package vn.mar.auth.service;

import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.auth.dto.request.LoginRequest;
import vn.mar.auth.dto.response.LoginResponse;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.jwt.JwtToken;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.user.entity.User;
import vn.mar.user.repository.UserRepository;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionProfileRepository permissionProfileRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            PermissionProfileRepository permissionProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionProfileRepository = permissionProfileRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByTenantIdAndEmailIgnoreCase(
                        request.tenantId(),
                        request.email().trim()
                )
                .filter(User::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!StringUtils.hasText(user.passwordHash())
                || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        Set<String> permissionCodes = permissionProfileRepository.findActivePermissionCodes(
                user.tenantId(),
                user.roleCode()
        );
        JwtToken accessToken = jwtTokenProvider.createAccessToken(user.id(), user.tenantId(), user.roleCode());

        return new LoginResponse(
                accessToken.token(),
                TOKEN_TYPE,
                jwtTokenProvider.expiresInSeconds(accessToken),
                user.id(),
                user.tenantId(),
                user.roleCode(),
                permissionCodes
        );
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED, "Invalid credentials");
    }
}
