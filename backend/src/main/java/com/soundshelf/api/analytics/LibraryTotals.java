package com.soundshelf.api.analytics;

public interface LibraryTotals {
    long getAlbums();

    long getArtists();

    long getGenres();

    long getTracks();

    Double getAvgRating();

    Double getAvgTracks();

    Double getLibraryValue();
}
