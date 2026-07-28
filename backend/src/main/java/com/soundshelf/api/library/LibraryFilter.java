package com.soundshelf.api.library;

import java.util.List;

/**
 * The one filter shape used by both the library list endpoint and the AI query
 * endpoint. Keeping them on the same structure means the natural-language feature
 * can only ever express a query the normal API could already run.
 */
public record LibraryFilter(
        List<String> genres,
        String artistContains,
        String titleContains,
        Integer yearFrom,
        Integer yearTo,
        Integer minRating,
        Integer maxRating,
        Integer minTracks,
        Integer maxTracks
) {
    public static LibraryFilter empty() {
        return new LibraryFilter(null, null, null, null, null, null, null, null, null);
    }

    public static LibraryFilter forList(String genre, String text) {
        return new LibraryFilter(
                genre == null || genre.isBlank() ? null : List.of(genre),
                null,
                text == null || text.isBlank() ? null : text.trim(),
                null, null, null, null, null, null);
    }

    /** Text search on the list endpoint should match either the album or the artist. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isFreeTextSearch() {
        return titleContains != null && artistContains == null;
    }
}
