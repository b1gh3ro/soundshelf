package com.soundshelf.api.ai;

import com.soundshelf.api.ai.dto.InsightDtos.QueryRequest;
import com.soundshelf.api.ai.dto.InsightDtos.QueryResponse;
import com.soundshelf.api.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class InsightController {

    private final InsightService insightService;
    private final ClaudeClient claude;
    private final QueryRateLimiter rateLimiter;

    public InsightController(InsightService insightService, ClaudeClient claude, QueryRateLimiter rateLimiter) {
        this.insightService = insightService;
        this.claude = claude;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = CurrentUser.idOf(jwt);
        rateLimiter.check(userId);
        return insightService.answer(userId, request);
    }

    /** Lets the UI tell the user upfront whether answers are coming from the model or the fallback. */
    @GetMapping("/status")
    public Status status() {
        return new Status(claude.isEnabled(), claude.isEnabled() ? "model" : "keyword-fallback");
    }

    public record Status(boolean modelEnabled, String mode) {
    }
}
