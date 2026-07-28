package com.soundshelf.api.catalog;

import com.soundshelf.api.catalog.dto.CatalogItem;
import com.soundshelf.api.catalog.dto.ItunesResult;
import com.soundshelf.api.library.LibraryRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class CatalogService {

    private static final int MAX_LIMIT = 50;
    private static final int ARTIST_PIVOT_CANDIDATES = 1;

    private final ItunesClient itunes;
    private final CatalogMapper mapper;
    private final LibraryRepository library;

    public CatalogService(ItunesClient itunes, CatalogMapper mapper, LibraryRepository library) {
        this.itunes = itunes;
        this.mapper = mapper;
        this.library = library;
    }

    public List<CatalogItem> search(Long userId, String query, SearchType type, int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        List<ItunesResult> raw = switch (type) {
            case ALBUM, SONG -> itunes.search(query, type.itunesEntity(), safeLimit);
            case ARTIST -> albumsForFirstMatchingArtist(query, safeLimit);
        };

        List<CatalogItem> items = dedupeByCatalogId(raw, safeLimit);
        return markSaved(userId, items);
    }

    private List<ItunesResult> albumsForFirstMatchingArtist(String query, int limit) {
        List<ItunesResult> artists = itunes.search(query, SearchType.ARTIST.itunesEntity(), ARTIST_PIVOT_CANDIDATES);
        if (artists.isEmpty() || artists.getFirst().artistId() == null) {
            return List.of();
        }
        return itunes.albumsByArtist(artists.getFirst().artistId(), limit);
    }

    /**
     * A song search often returns several tracks from the same album, and the catalog
     * itself carries reissues under one id. Keeping the first occurrence preserves
     * the relevance order iTunes already applied.
     */
    private List<CatalogItem> dedupeByCatalogId(Collection<ItunesResult> raw, int limit) {
        LinkedHashMap<Long, CatalogItem> unique = new LinkedHashMap<>();
        for (ItunesResult result : raw) {
            if (result.isAlbum()) {
                unique.putIfAbsent(result.collectionId(), mapper.toCatalogItem(result));
            }
        }
        return unique.values().stream().limit(limit).toList();
    }

    private List<CatalogItem> markSaved(Long userId, List<CatalogItem> items) {
        if (items.isEmpty()) {
            return items;
        }
        List<Long> ids = items.stream().map(CatalogItem::appleCatalogId).toList();
        Set<Long> saved = Set.copyOf(library.findSavedCatalogIds(userId, ids));
        return items.stream()
                .map(item -> item.withSavedFlag(saved.contains(item.appleCatalogId())))
                .toList();
    }
}
