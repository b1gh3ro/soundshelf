"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { formatPrice, releaseYear } from "@/lib/format";
import type { LibraryItem, PagedResponse } from "@/lib/types";
import { AlbumArt } from "@/components/AlbumArt";
import { Button, Input, Select, StarRating } from "@/components/ui/Controls";
import { AlbumGridSkeleton, EmptyState, ErrorState } from "@/components/ui/States";

const PAGE_SIZE = 12;
const DEBOUNCE_MS = 300;

export default function LibraryPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [genre, setGenre] = useState("");
  const [sort, setSort] = useState("createdAt,desc");

  const [data, setData] = useState<PagedResponse<LibraryItem> | null>(null);
  const [genres, setGenres] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<LibraryItem | null>(null);

  const inFlight = useRef<AbortController | null>(null);

  const load = useCallback(async () => {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;

    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE), sort });
      if (search.trim()) params.set("search", search.trim());
      if (genre) params.set("genre", genre);

      const result = await api.get<PagedResponse<LibraryItem>>(`/api/library?${params}`, controller.signal);
      setData(result);
    } catch (cause) {
      if (controller.signal.aborted) return;
      setError(cause instanceof ApiError ? cause.message : "Could not load your library.");
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }, [page, search, genre, sort]);

  useEffect(() => {
    const timer = setTimeout(load, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    api.get<string[]>("/api/library/genres").then(setGenres).catch(() => setGenres([]));
  }, [data?.totalElements]);

  useEffect(() => () => inFlight.current?.abort(), []);

  // Any filter change invalidates the current page number.
  function changeFilter(apply: () => void) {
    apply();
    setPage(0);
  }

  async function remove(item: LibraryItem) {
    if (!window.confirm(`Remove “${item.title}” from your library?`)) return;
    try {
      await api.delete(`/api/library/${item.id}`);
      load();
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not remove that album.");
    }
  }

  async function saveEdits(item: LibraryItem, rating: number | null, notes: string) {
    const updated = await api.put<LibraryItem>(`/api/library/${item.id}`, {
      userRating: rating,
      userNotes: notes.trim() || null,
    });
    setData((current) =>
      current
        ? { ...current, content: current.content.map((row) => (row.id === updated.id ? updated : row)) }
        : current,
    );
    setEditing(null);
  }

  const filtersActive = Boolean(search.trim() || genre);
  const items = data?.content ?? [];

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-xl font-semibold tracking-tight">Your library</h1>
        {data ? (
          <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
            {data.totalElements} album{data.totalElements === 1 ? "" : "s"}
          </p>
        ) : null}
      </header>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          value={search}
          onChange={(event) => changeFilter(() => setSearch(event.target.value))}
          placeholder="Filter by album or artist"
          aria-label="Filter your library"
        />
        <Select
          value={genre}
          onChange={(event) => changeFilter(() => setGenre(event.target.value))}
          aria-label="Filter by genre"
        >
          <option value="">All genres</option>
          {genres.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </Select>
        <Select value={sort} onChange={(event) => changeFilter(() => setSort(event.target.value))} aria-label="Sort by">
          <option value="createdAt,desc">Recently added</option>
          <option value="releaseDate,desc">Newest release</option>
          <option value="releaseDate,asc">Oldest release</option>
          <option value="title,asc">Title A–Z</option>
          <option value="artistName,asc">Artist A–Z</option>
          <option value="userRating,desc">Highest rated</option>
        </Select>
      </div>

      {error ? <ErrorState message={error} onRetry={load} /> : null}

      {loading && !data ? (
        <AlbumGridSkeleton count={PAGE_SIZE} />
      ) : items.length === 0 ? (
        filtersActive ? (
          <EmptyState
            title="Nothing matches those filters"
            body="Try a different genre, or clear the search box."
            action={
              <Button
                variant="secondary"
                onClick={() =>
                  changeFilter(() => {
                    setSearch("");
                    setGenre("");
                  })
                }
              >
                Clear filters
              </Button>
            }
          />
        ) : (
          <EmptyState
            title="Your library is empty"
            body="Search the catalog and save a few albums — the analytics and Ask pages fill in from here."
            action={
              <Link href="/search">
                <Button>Find albums</Button>
              </Link>
            }
          />
        )
      ) : (
        <>
          <div
            className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4"
            style={{ opacity: loading ? 0.6 : 1, transition: "opacity 150ms" }}
          >
            {items.map((item) => (
              <article key={item.id} className="flex flex-col">
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

                <div className="mt-2">
                  <StarRating value={item.userRating} size={16} />
                </div>

                {item.userNotes ? (
                  <p className="mt-1.5 line-clamp-2 text-xs italic" style={{ color: "var(--text-secondary)" }}>
                    “{item.userNotes}”
                  </p>
                ) : null}

                <div className="mt-2.5 flex gap-2">
                  <Button variant="secondary" onClick={() => setEditing(item)} className="flex-1 px-2 py-1.5 text-xs">
                    Edit
                  </Button>
                  <Button variant="danger" onClick={() => remove(item)} className="px-2 py-1.5 text-xs" aria-label={`Remove ${item.title}`}>
                    Remove
                  </Button>
                </div>
              </article>
            ))}
          </div>

          {data && data.totalPages > 1 ? (
            <nav className="flex items-center justify-between gap-3 pt-2" aria-label="Pagination">
              <Button variant="secondary" onClick={() => setPage((p) => p - 1)} disabled={data.first || loading}>
                Previous
              </Button>
              <span className="text-sm" style={{ color: "var(--text-secondary)" }}>
                Page {data.page + 1} of {data.totalPages}
              </span>
              <Button variant="secondary" onClick={() => setPage((p) => p + 1)} disabled={data.last || loading}>
                Next
              </Button>
            </nav>
          ) : null}
        </>
      )}

      {editing ? <EditDialog item={editing} onClose={() => setEditing(null)} onSave={saveEdits} /> : null}
    </div>
  );
}

function EditDialog({
  item,
  onClose,
  onSave,
}: {
  item: LibraryItem;
  onClose: () => void;
  onSave: (item: LibraryItem, rating: number | null, notes: string) => Promise<void>;
}) {
  const [rating, setRating] = useState(item.userRating);
  const [notes, setNotes] = useState(item.userNotes ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSave(item, rating, notes);
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not save those changes.");
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center p-4 sm:items-center"
      style={{ background: "rgba(0,0,0,0.5)" }}
      onClick={onClose}
      role="presentation"
    >
      <div
        className="w-full max-w-md rounded-xl border p-5 shadow-xl"
        style={{ background: "var(--surface-card)", borderColor: "var(--border)" }}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={`Edit ${item.title}`}
      >
        <h2 className="text-base font-semibold leading-snug">{item.title}</h2>
        <p className="mt-0.5 text-sm" style={{ color: "var(--text-secondary)" }}>
          {item.artistName} · {releaseYear(item.releaseDate)} · {formatPrice(item.collectionPrice)}
        </p>

        <form onSubmit={submit} className="mt-5 space-y-4">
          <div>
            <span className="mb-1.5 block text-sm font-medium" style={{ color: "var(--text-secondary)" }}>
              Your rating
            </span>
            <StarRating value={rating} onChange={setRating} size={26} />
          </div>

          <label className="block">
            <span className="mb-1.5 block text-sm font-medium" style={{ color: "var(--text-secondary)" }}>
              Notes
            </span>
            <textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              maxLength={1000}
              rows={3}
              placeholder="What do you want to remember about this one?"
              className="w-full resize-y rounded-md border px-3 py-2 text-sm outline-none focus:border-[var(--accent)]"
              style={{ background: "var(--surface-page)", color: "var(--text-primary)", borderColor: "var(--axis)" }}
            />
            <span className="mt-1 block text-right text-xs" style={{ color: "var(--text-muted)" }}>
              {notes.length}/1000
            </span>
          </label>

          {error ? (
            <p className="text-sm" style={{ color: "var(--danger)" }} role="alert">
              {error}
            </p>
          ) : null}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? "Saving…" : "Save changes"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
