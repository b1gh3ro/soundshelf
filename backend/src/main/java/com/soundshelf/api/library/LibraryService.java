package com.soundshelf.api.library;

import com.soundshelf.api.catalog.CatalogMapper;
import com.soundshelf.api.catalog.ItunesClient;
import com.soundshelf.api.catalog.dto.ItunesResult;
import com.soundshelf.api.common.ConflictException;
import com.soundshelf.api.common.NotFoundException;
import com.soundshelf.api.library.dto.LibraryDtos.LibraryItemResponse;
import com.soundshelf.api.library.dto.LibraryDtos.PagedResponse;
import com.soundshelf.api.library.dto.LibraryDtos.SaveItemRequest;
import com.soundshelf.api.library.dto.LibraryDtos.UpdateItemRequest;
import com.soundshelf.api.user.User;
import com.soundshelf.api.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LibraryService {

    private final LibraryRepository library;
    private final UserRepository users;
    private final ItunesClient itunes;
    private final LibraryMapper mapper;

    public LibraryService(LibraryRepository library, UserRepository users, ItunesClient itunes, LibraryMapper mapper) {
        this.library = library;
        this.users = users;
        this.itunes = itunes;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<LibraryItemResponse> list(Long userId, String genre, String search, Pageable pageable) {
        Page<LibraryItem> page = library.findAll(
                LibrarySpecifications.matching(userId, LibraryFilter.forList(genre, search)), pageable);
        return mapper.toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public List<String> genres(Long userId) {
        return library.findDistinctGenres(userId);
    }

    @Transactional
    public LibraryItemResponse save(Long userId, SaveItemRequest request) {
        if (library.existsByUserIdAndAppleCatalogId(userId, request.appleCatalogId())) {
            throw new ConflictException("That album is already in your library");
        }

        ItunesResult album = itunes.lookupAlbum(request.appleCatalogId())
                .orElseThrow(() -> new NotFoundException(
                        "No album found in the iTunes catalog with id " + request.appleCatalogId()));

        User owner = users.getReferenceById(userId);

        LibraryItem item = new LibraryItem();
        item.setUser(owner);
        item.setAppleCatalogId(album.collectionId());
        item.setTitle(album.collectionName());
        item.setArtistName(album.artistName());
        item.setGenre(album.primaryGenreName());
        item.setReleaseDate(CatalogMapper.parseReleaseDate(album.releaseDate()));
        item.setTrackCount(album.trackCount());
        item.setArtworkUrl(CatalogMapper.upscaleArtwork(album.artworkUrl100()));
        item.setCollectionPrice(album.collectionPrice());
        item.setUserRating(request.userRating());
        item.setUserNotes(trimToNull(request.userNotes()));

        try {
            return mapper.toResponse(library.save(item));
        } catch (DataIntegrityViolationException ex) {
            // Two saves of the same album racing each other; the unique constraint is the real guard.
            throw new ConflictException("That album is already in your library");
        }
    }

    @Transactional
    public LibraryItemResponse update(Long userId, Long itemId, UpdateItemRequest request) {
        LibraryItem item = requireOwned(userId, itemId);
        item.setUserRating(request.userRating());
        item.setUserNotes(trimToNull(request.userNotes()));
        return mapper.toResponse(library.save(item));
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        library.delete(requireOwned(userId, itemId));
    }

    /**
     * Returns 404 rather than 403 for an item owned by someone else — a 403 would
     * confirm the id exists, which is more than a caller needs to know.
     */
    private LibraryItem requireOwned(Long userId, Long itemId) {
        return library.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("No library item with id " + itemId));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
