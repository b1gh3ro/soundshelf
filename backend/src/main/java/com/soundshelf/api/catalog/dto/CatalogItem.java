package com.soundshelf.api.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A catalog album as the frontend sees it. {@code alreadySaved} is filled in per
 * request from the caller's own library so the search grid can show its real state.
 */
public record CatalogItem(
        Long appleCatalogId,
        String title,
        String artistName,
        String genre,
        LocalDate releaseDate,
        Integer trackCount,
        String artworkUrl,
        BigDecimal price,
        String appleUrl,
        boolean alreadySaved
) {
    public CatalogItem withSavedFlag(boolean saved) {
        return new CatalogItem(appleCatalogId, title, artistName, genre, releaseDate,
                trackCount, artworkUrl, price, appleUrl, saved);
    }
}
