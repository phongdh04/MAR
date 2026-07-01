package vn.mar.security.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.mar.authz.service.PermissionProfileResolver;
import vn.mar.common.logging.LogContext;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserPrincipal;
import vn.mar.security.handler.ApiAuthenticationEntryPoint;
import vn.mar.security.jwt.JwtClaims;
import vn.mar.security.jwt.JwtTokenProvider;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionProfileResolver permissionProfileResolver;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            PermissionProfileResolver permissionProfileResolver,
            ApiAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.permissionProfileResolver = permissionProfileResolver;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            commence(request, response, "Unsupported authentication scheme", null);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            commence(request, response, "Bearer token is required", null);
            return;
        }

        try {
            JwtClaims claims = jwtTokenProvider.parse(token);
            Set<String> permissionCodes = permissionProfileResolver.resolvePermissionCodes(
                    claims.tenantId(),
                    claims.roleCode()
            );
            CurrentUser currentUser = new CurrentUser(
                    claims.actorId(),
                    claims.tenantId(),
                    claims.roleCode(),
                    permissionCodes,
                    LogContext.requestId()
            );
            CurrentUserPrincipal principal = new CurrentUserPrincipal(currentUser);
            MDC.put("tenantId", claims.tenantId().toString());
            MDC.put("actorId", claims.actorId().toString());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    permissionCodes.stream()
                            .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
                            .toList()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            commence(request, response, "Invalid bearer token", exception);
        } catch (DataAccessException exception) {
            commence(request, response, "Authentication context cannot be resolved", exception);
        }
    }

    private void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            String message,
            Exception cause) throws IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(message, cause));
    }
}
