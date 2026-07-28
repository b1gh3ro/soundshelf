package com.soundshelf.api.ai.dto;

import com.soundshelf.api.library.LibraryFilter;
import com.soundshelf.api.library.dto.LibraryDtos.LibraryItemResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class InsightDtos {

    private InsightDtos() {
    }

    public record QueryRequest(
            @NotBlank @Size(max = 300) String question
    ) {
    }

    /**
     * {@code interpretation} is shown above the results so the user can see how their
     * question was read, and {@code source} tells them whether the model or the
     * keyword fallback produced it.
     */
    public record QueryResponse(
            String question,
            String interpretation,
            LibraryFilter filter,
            String source,
            int count,
            List<LibraryItemResponse> results
    ) {
    }
}
