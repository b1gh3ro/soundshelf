package com.soundshelf.api.analytics;

import com.soundshelf.api.analytics.dto.AnalyticsDtos.AnalyticsSummary;
import com.soundshelf.api.auth.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Every chart on the dashboard comes from this single call. Six round trips for
     * six charts would be slower and would let the tiles disagree with each other
     * if the library changed mid-load.
     */
    @GetMapping("/summary")
    public AnalyticsSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.summarise(CurrentUser.idOf(jwt));
    }
}
