package com.soundshelf.api.auth;

import com.soundshelf.api.auth.dto.AuthDtos.AuthResponse;
import com.soundshelf.api.auth.dto.AuthDtos.LoginRequest;
import com.soundshelf.api.auth.dto.AuthDtos.RegisterRequest;
import com.soundshelf.api.auth.dto.AuthDtos.UserSummary;
import com.soundshelf.api.common.ConflictException;
import com.soundshelf.api.common.NotFoundException;
import com.soundshelf.api.user.User;
import com.soundshelf.api.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), trimToNull(request.displayName()));
        return respondWithToken(users.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalise(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return respondWithToken(user);
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(Long userId) {
        return users.findById(userId)
                .map(AuthService::toSummary)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private AuthResponse respondWithToken(User user) {
        TokenService.IssuedToken token = tokenService.issue(user);
        long expiresIn = Duration.between(Instant.now(), token.expiresAt()).toSeconds();
        return new AuthResponse(token.value(), "Bearer", expiresIn, toSummary(user));
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
