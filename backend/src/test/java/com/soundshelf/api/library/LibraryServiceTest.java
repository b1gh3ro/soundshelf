package com.soundshelf.api.library;

import com.soundshelf.api.catalog.ItunesClient;
import com.soundshelf.api.catalog.dto.ItunesResult;
import com.soundshelf.api.common.ConflictException;
import com.soundshelf.api.common.NotFoundException;
import com.soundshelf.api.library.dto.LibraryDtos.SaveItemRequest;
import com.soundshelf.api.library.dto.LibraryDtos.UpdateItemRequest;
import com.soundshelf.api.user.User;
import com.soundshelf.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CATALOG_ID = 1122782080L;

    @Mock
    private LibraryRepository library;
    @Mock
    private UserRepository users;
    @Mock
    private ItunesClient itunes;
    @Mock
    private LibraryMapper mapper;

    @InjectMocks
    private LibraryService service;

    private ItunesResult album;

    @BeforeEach
    void setUp() {
        album = new ItunesResult("collection", CATALOG_ID, 471744L, "Parachutes", "Coldplay",
                "Alternative", "2000-07-10T07:00:00Z", 10,
                "https://example.com/art/100x100bb.jpg", new BigDecimal("9.99"),
                "https://music.apple.com/album/1122782080");
    }

    @Test
    @DisplayName("saving the same album twice is a conflict, not a duplicate row")
    void rejectsDuplicateSave() {
        when(library.existsByUserIdAndAppleCatalogId(USER_ID, CATALOG_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.save(USER_ID, new SaveItemRequest(CATALOG_ID, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in your library");

        verify(library, never()).save(any());
    }

    @Test
    @DisplayName("album metadata comes from iTunes, never from the request body")
    void storesCatalogMetadataFromLookup() {
        when(library.existsByUserIdAndAppleCatalogId(USER_ID, CATALOG_ID)).thenReturn(false);
        when(itunes.lookupAlbum(CATALOG_ID)).thenReturn(Optional.of(album));
        when(users.getReferenceById(USER_ID)).thenReturn(new User());
        when(library.save(any(LibraryItem.class))).thenAnswer(call -> call.getArgument(0));

        service.save(USER_ID, new SaveItemRequest(CATALOG_ID, (short) 4, "  spacious  "));

        ArgumentCaptor<LibraryItem> saved = ArgumentCaptor.forClass(LibraryItem.class);
        verify(library).save(saved.capture());

        LibraryItem item = saved.getValue();
        assertThat(item.getTitle()).isEqualTo("Parachutes");
        assertThat(item.getArtistName()).isEqualTo("Coldplay");
        assertThat(item.getGenre()).isEqualTo("Alternative");
        assertThat(item.getReleaseDate()).isEqualTo(LocalDate.of(2000, 7, 10));
        assertThat(item.getTrackCount()).isEqualTo(10);
        assertThat(item.getArtworkUrl()).endsWith("600x600bb.jpg");
        assertThat(item.getUserRating()).isEqualTo((short) 4);
        assertThat(item.getUserNotes()).isEqualTo("spacious");
    }

    @Test
    @DisplayName("an unknown catalog id is a 404, not a half-populated row")
    void rejectsUnknownCatalogId() {
        when(library.existsByUserIdAndAppleCatalogId(USER_ID, 99L)).thenReturn(false);
        when(itunes.lookupAlbum(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(USER_ID, new SaveItemRequest(99L, null, null)))
                .isInstanceOf(NotFoundException.class);

        verify(library, never()).save(any());
    }

    @Test
    @DisplayName("updating someone else's item reports 404 rather than confirming it exists")
    void updateOnForeignItemLooksLikeNotFound() {
        when(library.findByIdAndUserId(50L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OTHER_USER_ID, 50L, new UpdateItemRequest((short) 1, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deleting someone else's item reports 404 rather than confirming it exists")
    void deleteOnForeignItemLooksLikeNotFound() {
        when(library.findByIdAndUserId(50L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, 50L))
                .isInstanceOf(NotFoundException.class);

        // Typed matcher: LibraryRepository inherits delete(T) and delete(Specification).
        verify(library, never()).delete(any(LibraryItem.class));
    }

    @Test
    @DisplayName("blank notes are stored as null so the column stays clean")
    void blankNotesBecomeNull() {
        LibraryItem existing = new LibraryItem();
        when(library.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(existing));
        when(library.save(existing)).thenReturn(existing);

        service.update(USER_ID, 7L, new UpdateItemRequest((short) 3, "   "));

        assertThat(existing.getUserNotes()).isNull();
        assertThat(existing.getUserRating()).isEqualTo((short) 3);
    }
}
