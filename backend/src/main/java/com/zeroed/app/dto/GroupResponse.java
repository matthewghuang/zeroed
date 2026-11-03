package com.zeroed.app.dto;

import java.time.Instant;

public record GroupResponse(Long id, String name, Instant createdAt) {
}
