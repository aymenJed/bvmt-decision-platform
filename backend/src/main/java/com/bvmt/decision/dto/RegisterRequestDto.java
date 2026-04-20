package com.bvmt.decision.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Size(min = 3, max = 64)  String username,
        @NotBlank @Email                    String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        String fullName
) {}
