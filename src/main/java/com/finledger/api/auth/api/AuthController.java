package com.finledger.api.auth.api;

import com.finledger.api.auth.api.AuthDtos.*;
import com.finledger.api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Returns a JWT so the user is logged in straight away.")
    @SecurityRequirements
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) { return authService.register(req); }

    @Operation(summary = "Log in", description = "Copy the returned token into Authorize (top right).")
    @SecurityRequirements
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }

    @Operation(summary = "Who am I", description = "The user the current token belongs to.")
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Long userId) { return authService.me(userId); }

    @Operation(summary = "Health check")
    @SecurityRequirements
    @GetMapping("/health")
    public String health() { return "ok"; }
}
