package com.soundshelf.api.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs when no API key is configured, or when the model call fails. It handles the
 * common shapes of question — decades, genres, ratings, album length — so the feature
 * degrades to something useful rather than disappearing. The demo never depends on a
 * key being present.
 */
@Component
public class KeywordQueryParser {

    private static final Pattern DECADE = Pattern.compile("\\b(?:19|20)?(\\d0)s\\b");
    private static final Pattern EXPLICIT_YEAR = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final Pattern STAR_RATING = Pattern.compile("\\b([1-5])\\s*(?:\\+\\s*)?star");

    private static final Map<String, String> GENRE_KEYWORDS = Map.ofEntries(
            Map.entry("alternative", "Alternative"),
            Map.entry("indie", "Alternative"),
            Map.entry("rock", "Rock"),
            Map.entry("pop", "Pop"),
            Map.entry("hip hop", "Hip-Hop/Rap"),
            Map.entry("hip-hop", "Hip-Hop/Rap"),
            Map.entry("rap", "Hip-Hop/Rap"),
            Map.entry("jazz", "Jazz"),
            Map.entry("soul", "R&B/Soul"),
            Map.entry("r&b", "R&B/Soul"),
            Map.entry("country", "Country"),
            Map.entry("metal", "Metal"),
            Map.entry("reggae", "Reggae"),
            Map.entry("dance", "Dance"),
            Map.entry("electronic", "Dance"),
            Map.entry("bollywood", "Bollywood"),
            Map.entry("soundtrack", "Soundtrack"));

    public InterpretedFilter parse(String question) {
        String text = question.toLowerCase(Locale.ROOT);
        List<String> described = new ArrayList<>();

        List<String> genres = matchGenres(text, described);
        Integer[] years = matchYears(text, described);
        Integer minRating = matchMinRating(text, described);
        Integer minTracks = null;
        Integer maxTracks = null;

        if (text.contains("long album") || text.contains("longest")) {
            minTracks = 15;
            described.add("with at least 15 tracks");
        } else if (text.contains("short album") || text.contains("ep")) {
            maxTracks = 6;
            described.add("with 6 tracks or fewer");
        }

        String interpretation = described.isEmpty()
                ? "Showing your whole library — I could not pick out a specific filter from that."
                : "Showing albums " + String.join(", ", described) + ".";

        return new InterpretedFilter(
                interpretation, genres, null, null,
                years[0], years[1], minRating, null, minTracks, maxTracks);
    }

    private List<String> matchGenres(String text, List<String> described) {
        List<String> genres = GENRE_KEYWORDS.entrySet().stream()
                .filter(entry -> text.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();

        if (!genres.isEmpty()) {
            described.add("in " + String.join(" or ", genres));
        }
        return genres.isEmpty() ? null : genres;
    }

    private Integer[] matchYears(String text, List<String> described) {
        Matcher decade = DECADE.matcher(text);
        if (decade.find()) {
            int shorthand = Integer.parseInt(decade.group(1));
            // "90s" means 1990s; "20s" and "10s" mean the 2000s-era decades.
            int start = shorthand >= 30 ? 1900 + shorthand : 2000 + shorthand;
            described.add("released in the " + start + "s");
            return new Integer[]{start, start + 9};
        }

        Matcher year = EXPLICIT_YEAR.matcher(text);
        if (year.find()) {
            int value = Integer.parseInt(year.group(1));
            boolean after = text.contains("after") || text.contains("since") || text.contains("newer");
            boolean before = text.contains("before") || text.contains("older");

            if (after) {
                described.add("released after " + value);
                return new Integer[]{value, null};
            }
            if (before) {
                described.add("released before " + value);
                return new Integer[]{null, value};
            }
            described.add("released in " + value);
            return new Integer[]{value, value};
        }
        return new Integer[]{null, null};
    }

    private Integer matchMinRating(String text, List<String> described) {
        Matcher stars = STAR_RATING.matcher(text);
        if (stars.find()) {
            int rating = Integer.parseInt(stars.group(1));
            described.add("rated " + rating + " or higher");
            return rating;
        }
        if (text.contains("favourite") || text.contains("favorite") || text.contains("best")
                || text.contains("top rated") || text.contains("love")) {
            described.add("rated 4 or higher");
            return 4;
        }
        return null;
    }
}
