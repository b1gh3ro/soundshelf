package com.soundshelf.api.library;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class LibrarySpecifications {

    private LibrarySpecifications() {
    }

    /**
     * The user predicate is added first and unconditionally, so no combination of
     * filter values can produce a query that reaches another user's rows.
     */
    public static Specification<LibraryItem> matching(Long userId, LibraryFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (filter == null) {
                return cb.and(predicates.toArray(Predicate[]::new));
            }

            if (filter.genres() != null && !filter.genres().isEmpty()) {
                List<Predicate> genreMatches = filter.genres().stream()
                        .filter(genre -> genre != null && !genre.isBlank())
                        .map(genre -> cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()))
                        .map(Predicate.class::cast)
                        .toList();
                if (!genreMatches.isEmpty()) {
                    predicates.add(cb.or(genreMatches.toArray(Predicate[]::new)));
                }
            }

            if (filter.isFreeTextSearch()) {
                String pattern = like(filter.titleContains());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("artistName")), pattern)));
            } else {
                if (notBlank(filter.titleContains())) {
                    predicates.add(cb.like(cb.lower(root.get("title")), like(filter.titleContains())));
                }
                if (notBlank(filter.artistContains())) {
                    predicates.add(cb.like(cb.lower(root.get("artistName")), like(filter.artistContains())));
                }
            }

            if (filter.yearFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("releaseDate"), LocalDate.of(filter.yearFrom(), 1, 1)));
            }
            if (filter.yearTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("releaseDate"), LocalDate.of(filter.yearTo(), 12, 31)));
            }
            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("userRating"), filter.minRating().shortValue()));
            }
            if (filter.maxRating() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("userRating"), filter.maxRating().shortValue()));
            }
            if (filter.minTracks() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("trackCount"), filter.minTracks()));
            }
            if (filter.maxTracks() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("trackCount"), filter.maxTracks()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
