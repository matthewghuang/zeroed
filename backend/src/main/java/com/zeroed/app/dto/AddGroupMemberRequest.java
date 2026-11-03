package com.zeroed.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddGroupMemberRequest(@NotBlank @Email @Size(max = 254) String email) {
}
