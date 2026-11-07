package com.example.backend.common.dto;

import java.util.Map;

public record ApiError(String error, String message, Map<String, String> details, String requestId) {
}
