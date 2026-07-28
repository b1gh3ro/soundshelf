package com.soundshelf.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItunesResponse(int resultCount, List<ItunesResult> results) {

    public List<ItunesResult> safeResults() {
        return results == null ? List.of() : results;
    }
}
