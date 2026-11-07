package com.example.backend.security;

import com.example.backend.security.IdentityService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class JwtAuthenticationConverter
        extends org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter {

    public JwtAuthenticationConverter() {
        setJwtGrantedAuthoritiesConverter(jwt -> {
            if (!IdentityService.isSuperAdmin(jwt)) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        });
    }
}
