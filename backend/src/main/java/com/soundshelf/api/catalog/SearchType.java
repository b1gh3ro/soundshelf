package com.soundshelf.api.catalog;

import java.util.Arrays;

/**
 * The library only ever stores albums, so all three search modes resolve to albums:
 * a song search returns the album the song sits on, and an artist search returns
 * that artist's albums. One entity end to end, three ways to find it.
 */
public enum SearchType {
    ALBUM("album"),
    SONG("song"),
    ARTIST("musicArtist");

    private final String itunesEntity;

    SearchType(String itunesEntity) {
        this.itunesEntity = itunesEntity;
    }

    public String itunesEntity() {
        return itunesEntity;
    }

    public static SearchType from(String value) {
        if (value == null || value.isBlank()) {
            return ALBUM;
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "type must be one of: album, song, artist (got '" + value + "')"));
    }
}
