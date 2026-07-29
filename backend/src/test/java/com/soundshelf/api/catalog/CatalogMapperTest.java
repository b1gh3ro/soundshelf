package com.soundshelf.api.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMapperTest {

    @ParameterizedTest
    @CsvSource({
            "2000-07-10T07:00:00Z, 2000-07-10",
            "1957-01-01T08:00:00Z, 1957-01-01",
            "2024-04-19T12:00:00Z, 2024-04-19",
    })
    @DisplayName("iTunes timestamps become plain release dates")
    void parsesIsoTimestamps(String input, String expected) {
        assertThat(CatalogMapper.parseReleaseDate(input)).isEqualTo(LocalDate.parse(expected));
    }

    @Test
    @DisplayName("a bare date still parses — some older catalog rows have no time component")
    void parsesBareDate() {
        assertThat(CatalogMapper.parseReleaseDate("1968-11-22")).isEqualTo(LocalDate.of(1968, 11, 22));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not a date", "0000"})
    @DisplayName("an unparseable date is null rather than an exception — one bad row should not fail a search")
    void unparseableDatesBecomeNull(String input) {
        assertThat(CatalogMapper.parseReleaseDate(input)).isNull();
    }

    @Test
    @DisplayName("artwork is upscaled from the 100px thumbnail iTunes returns")
    void upscalesArtwork() {
        String thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/Music/abc.jpg/100x100bb.jpg";
        assertThat(CatalogMapper.upscaleArtwork(thumbnail)).endsWith("/600x600bb.jpg");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("a missing artwork url stays null instead of producing a broken link")
    void missingArtworkStaysNull(String input) {
        assertThat(CatalogMapper.upscaleArtwork(input)).isNull();
    }
}
