package com.soundshelf.api.analytics.dto;

import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Slice(String label, long total) {
    }

    public record CumulativePoint(String label, long total, long cumulative) {
    }

    public record Totals(
            long albums,
            long artists,
            long genres,
            long tracks,
            double avgRating,
            double avgTrackCount,
            double libraryValue
    ) {
    }

    public record AnalyticsSummary(
            Totals totals,
            List<Slice> byGenre,
            List<Slice> byDecade,
            List<Slice> releasesByYear,
            List<Slice> trackCountBuckets,
            List<Slice> topArtists,
            List<CumulativePoint> addedOverTime
    ) {
    }
}
