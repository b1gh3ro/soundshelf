package com.soundshelf.api.ai;

import com.soundshelf.api.ai.dto.InsightDtos.QueryRequest;
import com.soundshelf.api.ai.dto.InsightDtos.QueryResponse;
import com.soundshelf.api.library.LibraryFilter;
import com.soundshelf.api.library.LibraryItem;
import com.soundshelf.api.library.LibraryMapper;
import com.soundshelf.api.library.LibraryRepository;
import com.soundshelf.api.library.LibrarySpecifications;
import com.soundshelf.api.library.dto.LibraryDtos.LibraryItemResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InsightService {

    private static final int MAX_RESULTS = 50;

    private final ClaudeClient claude;
    private final KeywordQueryParser fallbackParser;
    private final LibraryRepository library;
    private final LibraryMapper mapper;

    public InsightService(ClaudeClient claude, KeywordQueryParser fallbackParser,
                          LibraryRepository library, LibraryMapper mapper) {
        this.claude = claude;
        this.fallbackParser = fallbackParser;
        this.library = library;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public QueryResponse answer(Long userId, QueryRequest request) {
        String question = request.question().trim();

        // The model is asked first; the keyword parser covers a missing key or a failed call.
        boolean usedModel = claude.isEnabled();
        InterpretedFilter interpreted = claude.interpret(question).orElse(null);
        if (interpreted == null) {
            interpreted = fallbackParser.parse(question);
            usedModel = false;
        }

        LibraryFilter filter = interpreted.toLibraryFilter();
        List<LibraryItem> matches = library.findAll(
                LibrarySpecifications.matching(userId, filter),
                PageRequest.of(0, MAX_RESULTS, Sort.by(Sort.Direction.DESC, "releaseDate"))).getContent();

        List<LibraryItemResponse> results = matches.stream().map(mapper::toResponse).toList();

        return new QueryResponse(
                question,
                interpreted.interpretationOr("Showing the albums that matched."),
                filter,
                usedModel ? "model" : "keyword-fallback",
                results.size(),
                results);
    }
}
