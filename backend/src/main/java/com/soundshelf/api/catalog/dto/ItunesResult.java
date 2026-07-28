package com.soundshelf.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * The subset of an iTunes result this app reads. Apple adds and removes fields
 * without notice, so unknown properties are ignored rather than treated as errors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItunesResult(
        String wrapperType,
        Long collectionId,
        Long artistId,
        String collectionName,
        String artistName,
        String primaryGenreName,
        String releaseDate,
        Integer trackCount,
        String artworkUrl100,
        BigDecimal collectionPrice,
        String collectionViewUrl
) {
    public boolean isAlbum() {
        return collectionId != null && collectionName != null && artistName != null;
    }
}
