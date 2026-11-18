package com.finledger.api.auth.service;

import com.finledger.api.auth.api.AuthDtos.*;
import com.finledger.api.common.AuthenticationFailedException;
import com.finledger.api.common.ConflictException;
import com.finledger.api.user.domain.User;
import com.finledger.api.user.repository.UserDirectory;
import com.finledger.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final UserDirectory userDirectory;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = UserDirectory.normalise(req.email());
        if (users.existsByEmail(email)) throw new ConflictException("Email already registered");
        User u = users.save(User.register(email, req.name(), encoder.encode(req.password())));
        return new AuthResponse(jwt.issue(u.getId()), toDto(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(UserDirectory.normalise(req.email()))
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
        return new AuthResponse(jwt.issue(u.getId()), toDto(u));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return toDto(userDirectory.get(userId));
    }

    private static UserDto toDto(User u) { return new UserDto(u.getId(), u.getEmail(), u.getName()); }
}
