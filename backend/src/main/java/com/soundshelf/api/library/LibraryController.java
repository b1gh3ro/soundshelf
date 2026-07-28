package com.soundshelf.api.library;

import com.soundshelf.api.auth.CurrentUser;
import com.soundshelf.api.library.dto.LibraryDtos.LibraryItemResponse;
import com.soundshelf.api.library.dto.LibraryDtos.PagedResponse;
import com.soundshelf.api.library.dto.LibraryDtos.SaveItemRequest;
import com.soundshelf.api.library.dto.LibraryDtos.UpdateItemRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public PagedResponse<LibraryItemResponse> list(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Pageable capped = pageable.getPageSize() > MAX_PAGE_SIZE
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                : pageable;

        return libraryService.list(CurrentUser.idOf(jwt), genre, search, capped);
    }

    @GetMapping("/genres")
    public List<String> genres(@AuthenticationPrincipal Jwt jwt) {
        return libraryService.genres(CurrentUser.idOf(jwt));
    }

    @PostMapping
    public ResponseEntity<LibraryItemResponse> save(
            @Valid @RequestBody SaveItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        LibraryItemResponse saved = libraryService.save(CurrentUser.idOf(jwt), request);
        return ResponseEntity.created(URI.create("/api/library/" + saved.id())).body(saved);
    }

    @PutMapping("/{id}")
    public LibraryItemResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return libraryService.update(CurrentUser.idOf(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        libraryService.delete(CurrentUser.idOf(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
