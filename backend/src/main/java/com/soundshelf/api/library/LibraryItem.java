package com.soundshelf.api.library;

import com.soundshelf.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One saved album in a user's library. Everything except {@code userRating} and
 * {@code userNotes} is a snapshot of the iTunes catalog record taken at save time,
 * fetched server-side rather than accepted from the client.
 */
@Entity
@Table(name = "library_items")
@Getter
@Setter
@NoArgsConstructor
public class LibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "apple_catalog_id", nullable = false)
    private Long appleCatalogId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 500)
    private String artistName;

    @Column(length = 120)
    private String genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "track_count")
    private Integer trackCount;

    @Column(name = "artwork_url", length = 1000)
    private String artworkUrl;

    @Column(name = "collection_price", precision = 10, scale = 2)
    private BigDecimal collectionPrice;

    @Column(name = "user_rating")
    private Short userRating;

    @Column(name = "user_notes", columnDefinition = "text")
    private String userNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
