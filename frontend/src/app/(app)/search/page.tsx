"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { releaseYear } from "@/lib/format";
import type { CatalogItem, SearchResponse, SearchType } from "@/lib/types";
import { AlbumArt } from "@/components/AlbumArt";
import { Button, Input, Select } from "@/components/ui/Controls";
import { AlbumGridSkeleton, EmptyState, ErrorState } from "@/components/ui/States";

const DEBOUNCE_MS = 300;

const TYPE_HINTS: Record<SearchType, string> = {
  album: "Searching album titles.",
  song: "Searching songs — results are the album each song appears on.",
  artist: "Searching artists — results are that artist's albums.",
};

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [type, setType] = useState<SearchType>("album");
  const [results, setResults] = useState<CatalogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);

  // Held across renders so a slower earlier response cannot overwrite a newer one.
  const inFlight = useRef<AbortController | null>(null);

  const runSearch = useCallback(async (term: string, entity: SearchType) => {
    inFlight.current?.abort();

    if (!term.trim()) {
      setResults([]);
      setSearched(false);
      setLoading(false);
      setError(null);
      return;
    }

    const controller = new AbortController();
    inFlight.current = controller;
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams({ query: term.trim(), type: entity, limit: "24" });
      const data = await api.get<SearchResponse>(`/api/search?${params}`, controller.signal);
      setResults(data.results);
      setSearched(true);
    } catch (cause) {
      if (controller.signal.aborted) return;
      setError(cause instanceof ApiError ? cause.message : "Search failed. Please try again.");
      setResults([]);
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => runSearch(query, type), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query, type, runSearch]);

  useEffect(() => () => inFlight.current?.abort(), []);

  async function save(item: CatalogItem) {
    setSavingId(item.appleCatalogId);
    try {
      await api.post("/api/library", { appleCatalogId: item.appleCatalogId });
      setResults((current) =>
        current.map((row) =>
          row.appleCatalogId === item.appleCatalogId ? { ...row, alreadySaved: true } : row,
        ),
      );
    } catch (cause) {
      // A 409 means someone already saved it — the end state is what the user wanted.
      if (cause instanceof ApiError && cause.status === 409) {
        setResults((current) =>
          current.map((row) =>
            row.appleCatalogId === item.appleCatalogId ? { ...row, alreadySaved: true } : row,
          ),
        );
      } else {
        setError(cause instanceof ApiError ? cause.message : "Could not save that album.");
      }
    } finally {
      setSavingId(null);
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">Search the catalog</h1>
        <p className="mt-1 text-sm" style={{ color: "var(--text-secondary)" }}>
          {TYPE_HINTS[type]}
        </p>
      </header>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Try “radiohead”, “shape of you”, or “miles davis”"
          aria-label="Search the iTunes catalog"
          autoFocus
        />
        <Select
          value={type}
          onChange={(event) => setType(event.target.value as SearchType)}
          aria-label="Search by"
        >
          <option value="album">By album</option>
          <option value="song">By song</option>
          <option value="artist">By artist</option>
        </Select>
      </div>

      {error ? <ErrorState message={error} onRetry={() => runSearch(query, type)} /> : null}

      {loading ? (
        <AlbumGridSkeleton count={8} />
      ) : !query.trim() ? (
        <EmptyState
          title="Find something to save"
          body="Search by album, by song to find the album it's on, or by artist to browse their discography."
        />
      ) : searched && results.length === 0 ? (
        <EmptyState
          title={`Nothing found for “${query.trim()}”`}
          body="Try a different spelling, or switch the search mode above."
        />
      ) : (
        <>
          {results.length > 0 ? (
            <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
              {results.length} album{results.length === 1 ? "" : "s"}
            </p>
          ) : null}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {results.map((item) => (
              <article key={item.appleCatalogId} className="flex flex-col">
                <AlbumArt src={item.artworkUrl} alt={`${item.title} by ${item.artistName}`} />
                <h2 className="mt-2.5 line-clamp-2 text-sm font-medium leading-snug">{item.title}</h2>
                <p className="mt-0.5 line-clamp-1 text-sm" style={{ color: "var(--text-secondary)" }}>
                  {item.artistName}
                </p>
                <p className="mt-0.5 text-xs" style={{ color: "var(--text-muted)" }}>
                  {releaseYear(item.releaseDate)}
                  {item.genre ? ` · ${item.genre}` : ""}
                  {item.trackCount ? ` · ${item.trackCount} tracks` : ""}
                </p>
                <div className="mt-2.5">
                  {item.alreadySaved ? (
                    <span
                      className="inline-flex items-center gap-1.5 text-sm font-medium"
                      style={{ color: "var(--success)" }}
                    >
                      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M20 6L9 17l-5-5" />
                      </svg>
                      In your library
                    </span>
                  ) : (
                    <Button
                      variant="secondary"
                      onClick={() => save(item)}
                      disabled={savingId === item.appleCatalogId}
                      className="w-full"
                    >
                      {savingId === item.appleCatalogId ? "Saving…" : "Save"}
                    </Button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
