package com.soundshelf.api.catalog;

import com.soundshelf.api.catalog.dto.CatalogItem;
import com.soundshelf.api.catalog.dto.ItunesResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Component
public class CatalogMapper {

    private static final String THUMBNAIL_SIZE = "100x100bb";
    private static final String DISPLAY_SIZE = "600x600bb";

    public CatalogItem toCatalogItem(ItunesResult result) {
        return new CatalogItem(
                result.collectionId(),
                result.collectionName(),
                result.artistName(),
                blankToNull(result.primaryGenreName()),
                parseReleaseDate(result.releaseDate()),
                result.trackCount(),
                upscaleArtwork(result.artworkUrl100()),
                result.collectionPrice() != null && result.collectionPrice().signum() >= 0
                        ? result.collectionPrice()
                        : null,
                result.collectionViewUrl(),
                false);
    }

    /**
     * iTunes hands back a 100px thumbnail. The URL is templated on its dimensions,
     * so swapping the segment gives a sharp image in the grid at no extra request.
     */
    public static String upscaleArtwork(String artworkUrl100) {
        if (artworkUrl100 == null || artworkUrl100.isBlank()) {
            return null;
        }
        return artworkUrl100.replace(THUMBNAIL_SIZE, DISPLAY_SIZE);
    }

    public static LocalDate parseReleaseDate(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ex) {
            // Some older catalog entries carry a bare date; a missing date is not worth failing over.
            try {
                return LocalDate.parse(isoTimestamp.substring(0, Math.min(10, isoTimestamp.length())));
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
