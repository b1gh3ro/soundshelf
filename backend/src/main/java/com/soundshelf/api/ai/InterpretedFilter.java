package com.soundshelf.api.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soundshelf.api.library.LibraryFilter;

import java.util.List;

/**
 * Exactly what the model is allowed to emit. It never writes SQL and never sees the
 * database — it fills in this fixed set of fields, which is then clamped and turned
 * into a parameterised query scoped to the caller.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InterpretedFilter(
        String interpretation,
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
    private static final int EARLIEST_YEAR = 1900;
    private static final int LATEST_YEAR = 2100;

    /**
     * Clamps every value into a range the normal API would accept. A model that
     * hallucinates {@code minRating: 99} produces an empty result set, not an error
     * and not a query the UI could never have made itself.
     */
    public LibraryFilter toLibraryFilter() {
        return new LibraryFilter(
                genres == null || genres.isEmpty() ? null : genres.stream().limit(10).toList(),
                trimToNull(artistContains),
                trimToNull(titleContains),
                clamp(yearFrom, EARLIEST_YEAR, LATEST_YEAR),
                clamp(yearTo, EARLIEST_YEAR, LATEST_YEAR),
                clamp(minRating, 1, 5),
                clamp(maxRating, 1, 5),
                clamp(minTracks, 0, 500),
                clamp(maxTracks, 0, 500));
    }

    public String interpretationOr(String fallback) {
        return interpretation == null || interpretation.isBlank() ? fallback : interpretation.trim();
    }

    private static Integer clamp(Integer value, int min, int max) {
        if (value == null) {
            return null;
        }
        return Math.clamp(value, min, max);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
