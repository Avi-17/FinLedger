package com.finledger.api.auth.api;

import com.finledger.api.auth.api.AuthDtos.*;
import com.finledger.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) { return authService.register(req); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return authService.login(req); }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Long userId) { return authService.me(userId); }

    @GetMapping("/health")
    public String health() { return "ok"; }
}
