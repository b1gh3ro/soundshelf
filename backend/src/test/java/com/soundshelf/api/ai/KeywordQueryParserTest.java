package com.soundshelf.api.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordQueryParserTest {

    private final KeywordQueryParser parser = new KeywordQueryParser();

    @Test
    @DisplayName("a two-digit decade resolves to the right century")
    void resolvesDecades() {
        assertThat(parser.parse("albums from the 90s").yearFrom()).isEqualTo(1990);
        assertThat(parser.parse("albums from the 90s").yearTo()).isEqualTo(1999);
        assertThat(parser.parse("stuff from the 20s").yearFrom()).isEqualTo(2020);
        assertThat(parser.parse("anything from the 70s").yearFrom()).isEqualTo(1970);
    }

    @Test
    @DisplayName("before and after change the direction of a year bound")
    void readsYearDirection() {
        assertThat(parser.parse("albums after 2015").yearFrom()).isEqualTo(2015);
        assertThat(parser.parse("albums after 2015").yearTo()).isNull();
        assertThat(parser.parse("albums before 1980").yearTo()).isEqualTo(1980);
        assertThat(parser.parse("albums before 1980").yearFrom()).isNull();
    }

    @Test
    @DisplayName("genre synonyms map onto the names iTunes actually uses")
    void mapsGenreSynonyms() {
        assertThat(parser.parse("my rap albums").genres()).containsExactly("Hip-Hop/Rap");
        assertThat(parser.parse("indie records").genres()).containsExactly("Alternative");
        assertThat(parser.parse("some soul music").genres()).containsExactly("R&B/Soul");
    }

    @Test
    @DisplayName("rating language becomes a minimum rating")
    void readsRatings() {
        assertThat(parser.parse("my 5 star albums").minRating()).isEqualTo(5);
        assertThat(parser.parse("my favourite records").minRating()).isEqualTo(4);
        assertThat(parser.parse("the best ones").minRating()).isEqualTo(4);
    }

    @Test
    @DisplayName("album length language becomes a track-count bound")
    void readsAlbumLength() {
        assertThat(parser.parse("long albums").minTracks()).isEqualTo(15);
        assertThat(parser.parse("short albums").maxTracks()).isEqualTo(6);
    }

    @Test
    @DisplayName("a question it cannot read returns an empty filter and says so")
    void unparseableQuestionIsHonest() {
        InterpretedFilter result = parser.parse("what should I listen to tonight");

        assertThat(result.genres()).isNull();
        assertThat(result.yearFrom()).isNull();
        assertThat(result.minRating()).isNull();
        assertThat(result.interpretation()).contains("could not pick out a specific filter");
    }

    @Test
    @DisplayName("several constraints in one question all land")
    void combinesConstraints() {
        InterpretedFilter result = parser.parse("my favourite alternative albums from the 2000s");

        assertThat(result.genres()).containsExactly("Alternative");
        assertThat(result.yearFrom()).isEqualTo(2000);
        assertThat(result.yearTo()).isEqualTo(2009);
        assertThat(result.minRating()).isEqualTo(4);
    }
}
