package com.soundshelf.api.analytics;

import com.soundshelf.api.analytics.dto.AnalyticsDtos.AnalyticsSummary;
import com.soundshelf.api.analytics.dto.AnalyticsDtos.CumulativePoint;
import com.soundshelf.api.analytics.dto.AnalyticsDtos.Slice;
import com.soundshelf.api.analytics.dto.AnalyticsDtos.Totals;
import com.soundshelf.api.library.LibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalyticsService {

    /**
     * Histogram buckets are ordered here rather than in SQL — sorting them by label
     * would put "11-15" before "6-10", and sorting by count would reorder the axis
     * every time the library changes.
     */
    private static final List<String> BUCKET_ORDER = List.of("1-5", "6-10", "11-15", "16-20", "21+", "Unknown");

    private final LibraryRepository library;

    public AnalyticsService(LibraryRepository library) {
        this.library = library;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summarise(Long userId) {
        return new AnalyticsSummary(
                totals(userId),
                slices(library.countByGenre(userId)),
                slices(library.countByDecade(userId)),
                slices(library.countByReleaseYear(userId)),
                orderedBuckets(library.countByTrackCountBucket(userId)),
                slices(library.countTopArtists(userId)),
                cumulative(library.countAddedPerDay(userId)));
    }

    private Totals totals(Long userId) {
        LibraryTotals raw = library.loadTotals(userId);
        if (raw == null) {
            return new Totals(0, 0, 0, 0, 0, 0, 0);
        }
        return new Totals(
                raw.getAlbums(),
                raw.getArtists(),
                raw.getGenres(),
                raw.getTracks(),
                round(orZero(raw.getAvgRating())),
                round(orZero(raw.getAvgTracks())),
                round(orZero(raw.getLibraryValue())));
    }

    private static List<Slice> slices(List<LabelCount> rows) {
        return rows.stream().map(row -> new Slice(row.getLabel(), row.getTotal())).toList();
    }

    private static List<Slice> orderedBuckets(List<LabelCount> rows) {
        return rows.stream()
                .map(row -> new Slice(row.getLabel(), row.getTotal()))
                .sorted(Comparator.comparingInt(slice -> {
                    int index = BUCKET_ORDER.indexOf(slice.label());
                    return index < 0 ? BUCKET_ORDER.size() : index;
                }))
                .toList();
    }

    /** The "library growth" line wants a running total, which is cheaper to build here than in SQL. */
    private static List<CumulativePoint> cumulative(List<LabelCount> rows) {
        List<CumulativePoint> points = new ArrayList<>(rows.size());
        long running = 0;
        for (LabelCount row : rows) {
            running += row.getTotal();
            points.add(new CumulativePoint(row.getLabel(), row.getTotal(), running));
        }
        return points;
    }

    private static double orZero(Double value) {
        return value == null ? 0d : value;
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
