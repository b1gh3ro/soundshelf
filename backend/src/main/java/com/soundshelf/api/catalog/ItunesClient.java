package com.soundshelf.api.catalog;

import com.soundshelf.api.catalog.dto.ItunesResponse;
import com.soundshelf.api.catalog.dto.ItunesResult;
import com.soundshelf.api.common.UpstreamException;
import com.soundshelf.api.config.AppProperties;
import com.soundshelf.api.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class ItunesClient {

    private static final Logger log = LoggerFactory.getLogger(ItunesClient.class);

    private final RestClient client;
    private final AppProperties properties;

    public ItunesClient(RestClient itunesRestClient, AppProperties properties) {
        this.client = itunesRestClient;
        this.properties = properties;
    }

    @Cacheable(cacheNames = CacheConfig.ITUNES_CACHE, key = "'search:' + #entity + ':' + #term + ':' + #limit")
    public List<ItunesResult> search(String term, String entity, int limit) {
        ItunesResponse response = get(uri -> uri
                .path("/search")
                .queryParam("term", term)
                .queryParam("entity", entity)
                .queryParam("country", properties.itunes().country())
                .queryParam("limit", limit)
                .build());
        return response.safeResults();
    }

    @Cacheable(cacheNames = CacheConfig.ITUNES_CACHE, key = "'album:' + #collectionId")
    public Optional<ItunesResult> lookupAlbum(long collectionId) {
        ItunesResponse response = get(uri -> uri
                .path("/lookup")
                .queryParam("id", collectionId)
                .queryParam("entity", "album")
                .build());
        return response.safeResults().stream()
                .filter(ItunesResult::isAlbum)
                .findFirst();
    }

    /**
     * An artist search on its own returns nothing but a name and an id, so the id is
     * immediately pivoted into that artist's albums to keep results useful.
     */
    @Cacheable(cacheNames = CacheConfig.ITUNES_CACHE, key = "'artistAlbums:' + #artistId + ':' + #limit")
    public List<ItunesResult> albumsByArtist(long artistId, int limit) {
        ItunesResponse response = get(uri -> uri
                .path("/lookup")
                .queryParam("id", artistId)
                .queryParam("entity", "album")
                .queryParam("limit", limit)
                .build());
        // The first row echoes the artist back; only the collection rows are albums.
        return response.safeResults().stream()
                .filter(ItunesResult::isAlbum)
                .toList();
    }

    private ItunesResponse get(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriSpec) {
        try {
            ItunesResponse response = client.get()
                    .uri(uriSpec)
                    .retrieve()
                    .body(ItunesResponse.class);

            if (response == null) {
                throw new UpstreamException("The iTunes catalog returned an empty response");
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("iTunes request failed", ex);
            throw new UpstreamException("The iTunes catalog is not reachable right now", ex);
        }
    }
}
