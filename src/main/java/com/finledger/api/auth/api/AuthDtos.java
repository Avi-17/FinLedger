package com.finledger.api.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(
            @Schema(example = "rahul@test.com") @NotBlank @Email String email,
            @Schema(example = "Rahul") @NotBlank @Size(max = 80) String name,
            @Schema(example = "password123") @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(@Schema(example = "rahul@test.com") @NotBlank @Email String email,
                               @Schema(example = "password123") @NotBlank String password) {}

    public record UserDto(Long id, String email, String name) {}

    public record AuthResponse(String token, UserDto user) {}
}