package com.zeroed.app.dto;

import com.zeroed.app.models.UserRole;

public record MeResponse(String email, UserRole role) {
}
