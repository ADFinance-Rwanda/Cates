package com.example.backend.config;

import com.example.backend.organization.Organization;
import com.example.backend.security.JwtAuthenticationConverter;
import tools.jackson.databind.ObjectMapper;
import com.example.backend.common.dto.ApiError;
import com.example.backend.security.RateLimitFilter;
import com.example.backend.security.TenantContextFilter;
import com.example.backend.security.IdentityService;
import com.example.backend.security.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            ObjectMapper objectMapper,
            RateLimiter rateLimiter,
            IdentityService identityService,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimiter, objectMapper);
        TenantContextFilter tenantContextFilter = new TenantContextFilter(identityService, objectMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, exception) -> {
                            log.warn("authentication_failed path={}", request.getRequestURI());
                            write(response, objectMapper, 401, "UNAUTHORIZED", "Authentication required");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            log.warn("authorization_failed path={}", request.getRequestURI());
                            write(response, objectMapper, 403, "FORBIDDEN", "You do not have permission to do that");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/ready", "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) -> {
                            log.warn("authentication_failed path={}", request.getRequestURI());
                            write(response, objectMapper, 401, "UNAUTHORIZED", "Authentication required");
                        }));
        http.addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(tenantContextFilter, RateLimitFilter.class);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(AppProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getSecurity().getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.getSecurity().getIssuer());
        OAuth2TokenValidator<Jwt> audience = token -> {
            if (token.getAudience() != null && token.getAudience().contains(properties.getSecurity().getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    @Bean
    public com.example.backend.security.JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new com.example.backend.security.JwtAuthenticationConverter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(properties.getCors().getAllowedOrigins().split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id", "X-Organization-Id", "If-Match"));
        configuration.setExposedHeaders(List.of("X-Request-Id", "ETag", "Retry-After", "Idempotent-Replay"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        return request -> configuration;
    }

    private void write(HttpServletResponse response, ObjectMapper objectMapper, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ApiError(code, message, Map.of(), MDC.get("requestId")));
    }
}
