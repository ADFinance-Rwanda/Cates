package com.example.backend.common.exception;

import com.example.backend.common.dto.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JsonErrors {

    private JsonErrors() {
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getOutputStream(), new ApiError(code, message, Map.of(), MDC.get("requestId")));
    }
}
