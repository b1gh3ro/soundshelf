package com.soundshelf.api.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class LibraryDtos {

    private LibraryDtos() {
    }

    /**
     * Only the catalog id and the user's own annotations are accepted. Title, artist
     * and the rest are fetched server-side from iTunes, so a client cannot inject
     * metadata that never existed in the catalog.
     */
    public record SaveItemRequest(
            @NotNull @Positive Long appleCatalogId,
            @Min(1) @Max(5) Short userRating,
            @Size(max = 1000) String userNotes
    ) {
    }

    public record UpdateItemRequest(
            @Min(1) @Max(5) Short userRating,
            @Size(max = 1000) String userNotes
    ) {
    }

    public record LibraryItemResponse(
            Long id,
            Long appleCatalogId,
            String title,
            String artistName,
            String genre,
            LocalDate releaseDate,
            Integer trackCount,
            String artworkUrl,
            BigDecimal collectionPrice,
            Short userRating,
            String userNotes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}
