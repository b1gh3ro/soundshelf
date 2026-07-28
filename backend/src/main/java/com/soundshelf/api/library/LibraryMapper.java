package com.soundshelf.api.library;

import com.soundshelf.api.library.dto.LibraryDtos.LibraryItemResponse;
import com.soundshelf.api.library.dto.LibraryDtos.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class LibraryMapper {

    public LibraryItemResponse toResponse(LibraryItem item) {
        return new LibraryItemResponse(
                item.getId(),
                item.getAppleCatalogId(),
                item.getTitle(),
                item.getArtistName(),
                item.getGenre(),
                item.getReleaseDate(),
                item.getTrackCount(),
                item.getArtworkUrl(),
                item.getCollectionPrice(),
                item.getUserRating(),
                item.getUserNotes(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public PagedResponse<LibraryItemResponse> toPagedResponse(Page<LibraryItem> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
