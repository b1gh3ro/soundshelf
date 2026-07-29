package com.soundshelf.api.ai;

import com.soundshelf.api.library.LibraryFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These cover the boundary the AI feature rests on: whatever the model returns has
 * to come out the other side as a filter the normal API could have produced itself.
 */
class InterpretedFilterTest {

    @Test
    @DisplayName("out-of-range ratings are clamped rather than passed through to the query")
    void clampsRatings() {
        InterpretedFilter wild = filter(99, -4);

        LibraryFilter result = wild.toLibraryFilter();

        assertThat(result.minRating()).isEqualTo(5);
        assertThat(result.maxRating()).isEqualTo(1);
    }

    @Test
    @DisplayName("absurd years are clamped to a sane window")
    void clampsYears() {
        InterpretedFilter wild = new InterpretedFilter(
                "anything", null, null, null, 1200, 9999, null, null, null, null);

        LibraryFilter result = wild.toLibraryFilter();

        assertThat(result.yearFrom()).isEqualTo(1900);
        assertThat(result.yearTo()).isEqualTo(2100);
    }

    @Test
    @DisplayName("a runaway genre list is capped so one response cannot build a huge OR clause")
    void capsGenreList() {
        List<String> many = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> "Genre " + i)
                .toList();

        InterpretedFilter wild = new InterpretedFilter(
                "anything", many, null, null, null, null, null, null, null, null);

        assertThat(wild.toLibraryFilter().genres()).hasSize(10);
    }

    @Test
    @DisplayName("nulls stay null so an unconstrained field does not become a filter")
    void leavesNullsAlone() {
        InterpretedFilter empty = new InterpretedFilter(
                "everything", null, null, null, null, null, null, null, null, null);

        LibraryFilter result = empty.toLibraryFilter();

        assertThat(result.genres()).isNull();
        assertThat(result.minRating()).isNull();
        assertThat(result.yearFrom()).isNull();
        assertThat(result.artistContains()).isNull();
    }

    @Test
    @DisplayName("blank strings are treated as absent, not as a match-everything LIKE")
    void blankStringsBecomeNull() {
        InterpretedFilter blank = new InterpretedFilter(
                "  ", null, "   ", "", null, null, null, null, null, null);

        LibraryFilter result = blank.toLibraryFilter();

        assertThat(result.artistContains()).isNull();
        assertThat(result.titleContains()).isNull();
        assertThat(blank.interpretationOr("fallback")).isEqualTo("fallback");
    }

    private static InterpretedFilter filter(Integer minRating, Integer maxRating) {
        return new InterpretedFilter(
                "anything", null, null, null, null, null, minRating, maxRating, null, null);
    }
}
