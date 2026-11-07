package com.example.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.example.backend.common.exception.JsonErrors;
import com.example.backend.security.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!limited(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }
        String route = request.getRequestURI().replaceAll(
                "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "/{id}");
        boolean allowed = rateLimiter.allow(jwtAuthentication.getToken().getSubject(), request.getMethod(), route);
        if (!allowed) {
            response.setHeader("Retry-After", String.valueOf(rateLimiter.windowSeconds()));
            JsonErrors.write(response, objectMapper, 429, "RATE_LIMITED", "Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean limited(HttpServletRequest request) {
        String method = request.getMethod();
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("PATCH") && !method.equals("DELETE")) {
            return false;
        }
        String path = request.getRequestURI();
        return path.equals("/api/tasks")
                || path.startsWith("/api/tasks/")
                || path.equals("/api/members")
                || path.startsWith("/api/members/");
    }
}
