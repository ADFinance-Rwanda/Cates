package com.example.backend.security;

import com.example.backend.organization.Organization;
import tools.jackson.databind.ObjectMapper;
import com.example.backend.common.exception.ApiException;
import com.example.backend.common.exception.JsonErrors;
import com.example.backend.security.IdentityService;
import com.example.backend.security.CurrentUser;
import com.example.backend.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantContextFilter extends OncePerRequestFilter {

    public static final String ORGANIZATION_HEADER = "X-Organization-Id";

    private final IdentityService identityService;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(IdentityService identityService, ObjectMapper objectMapper) {
        this.identityService = identityService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            CurrentUser principal = identityService.establish(
                    jwtAuthentication.getToken(),
                    request.getHeader(ORGANIZATION_HEADER));
            TenantContext.set(principal);
            MDC.put("userId", principal.userId().toString());
            if (principal.organizationId() != null) {
                MDC.put("orgId", principal.organizationId().toString());
            }
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            JsonErrors.write(response, objectMapper, exception.getStatus().value(), exception.getCode(), exception.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
