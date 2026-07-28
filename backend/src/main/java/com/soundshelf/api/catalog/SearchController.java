package com.soundshelf.api.catalog;

import com.soundshelf.api.auth.CurrentUser;
import com.soundshelf.api.catalog.dto.CatalogItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final CatalogService catalogService;

    public SearchController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam @NotBlank @Size(max = 200) String query,
            @RequestParam(required = false, defaultValue = "album") String type,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(50) int limit,
            @AuthenticationPrincipal Jwt jwt) {

        SearchType searchType = SearchType.from(type);
        List<CatalogItem> results = catalogService.search(CurrentUser.idOf(jwt), query.trim(), searchType, limit);
        return new SearchResponse(query.trim(), searchType.name().toLowerCase(), results.size(), results);
    }

    public record SearchResponse(String query, String type, int count, List<CatalogItem> results) {
    }
}
