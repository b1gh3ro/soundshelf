package com.soundshelf.api.library;

import com.soundshelf.api.analytics.LabelCount;
import com.soundshelf.api.analytics.LibraryTotals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Every method here is scoped by userId. There is deliberately no "find by id" that
 * ignores ownership — the only way to reach a row is through the user who owns it.
 */
public interface LibraryRepository extends JpaRepository<LibraryItem, Long>, JpaSpecificationExecutor<LibraryItem> {

    Optional<LibraryItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndAppleCatalogId(Long userId, Long appleCatalogId);

    @Query("select i.appleCatalogId from LibraryItem i where i.user.id = :userId and i.appleCatalogId in :catalogIds")
    List<Long> findSavedCatalogIds(@Param("userId") Long userId, @Param("catalogIds") Collection<Long> catalogIds);

    @Query("select distinct i.genre from LibraryItem i where i.user.id = :userId and i.genre is not null order by i.genre")
    List<String> findDistinctGenres(@Param("userId") Long userId);

    // --- Analytics -----------------------------------------------------------
    // These are aggregates, not row fetches. Doing them in SQL keeps the response
    // O(number of groups) instead of shipping the whole library to the app to reduce.

    @Query(value = """
            select coalesce(nullif(trim(genre), ''), 'Unknown') as label,
                   count(*) as total
            from library_items
            where user_id = :userId
            group by 1
            order by total desc, label asc
            """, nativeQuery = true)
    List<LabelCount> countByGenre(@Param("userId") Long userId);

    @Query(value = """
            select ((extract(year from release_date)::int / 10) * 10) || 's' as label,
                   count(*) as total
            from library_items
            where user_id = :userId and release_date is not null
            group by 1
            order by 1
            """, nativeQuery = true)
    List<LabelCount> countByDecade(@Param("userId") Long userId);

    @Query(value = """
            select extract(year from release_date)::int::text as label,
                   count(*) as total
            from library_items
            where user_id = :userId and release_date is not null
            group by 1
            order by 1
            """, nativeQuery = true)
    List<LabelCount> countByReleaseYear(@Param("userId") Long userId);

    @Query(value = """
            select case
                       when track_count is null then 'Unknown'
                       when track_count <= 5 then '1-5'
                       when track_count <= 10 then '6-10'
                       when track_count <= 15 then '11-15'
                       when track_count <= 20 then '16-20'
                       else '21+'
                   end as label,
                   count(*) as total
            from library_items
            where user_id = :userId
            group by 1
            """, nativeQuery = true)
    List<LabelCount> countByTrackCountBucket(@Param("userId") Long userId);

    @Query(value = """
            select artist_name as label, count(*) as total
            from library_items
            where user_id = :userId
            group by 1
            order by total desc, label asc
            limit 10
            """, nativeQuery = true)
    List<LabelCount> countTopArtists(@Param("userId") Long userId);

    @Query(value = """
            select to_char(created_at at time zone 'UTC', 'YYYY-MM-DD') as label,
                   count(*) as total
            from library_items
            where user_id = :userId
            group by 1
            order by 1
            """, nativeQuery = true)
    List<LabelCount> countAddedPerDay(@Param("userId") Long userId);

    @Query(value = """
            select count(*)                                        as albums,
                   count(distinct artist_name)                     as artists,
                   count(distinct genre)                           as genres,
                   coalesce(sum(track_count), 0)                   as tracks,
                   coalesce(avg(user_rating)::float8, 0)           as "avgRating",
                   coalesce(avg(track_count)::float8, 0)           as "avgTracks",
                   coalesce(sum(collection_price)::float8, 0)      as "libraryValue"
            from library_items
            where user_id = :userId
            """, nativeQuery = true)
    LibraryTotals loadTotals(@Param("userId") Long userId);
}
