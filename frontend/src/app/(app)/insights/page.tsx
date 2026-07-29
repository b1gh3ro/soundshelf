"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { releaseYear } from "@/lib/format";
import type { AiStatus, InsightResponse, LibraryFilter } from "@/lib/types";
import { AlbumArt } from "@/components/AlbumArt";
import { Button, Input, StarRating } from "@/components/ui/Controls";
import { AlbumGridSkeleton, EmptyState, ErrorState } from "@/components/ui/States";

const EXAMPLES = [
  "which alternative albums from the 2000s do I have?",
  "my 5 star jazz records",
  "long albums released after 2015",
  "my favourite hip hop",
];

export default function InsightsPage() {
  const [question, setQuestion] = useState("");
  const [status, setStatus] = useState<AiStatus | null>(null);
  const [answer, setAnswer] = useState<InsightResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<AiStatus>("/api/ai/status").then(setStatus).catch(() => setStatus(null));
  }, []);

  async function ask(text: string) {
    const trimmed = text.trim();
    if (!trimmed) return;

    setQuestion(trimmed);
    setLoading(true);
    setError(null);
    try {
      setAnswer(await api.post<InsightResponse>("/api/ai/query", { question: trimmed }));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not answer that just now.");
      setAnswer(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">Ask your library</h1>
        <p className="mt-1 text-sm" style={{ color: "var(--text-secondary)" }}>
          Describe what you're after in plain English and I'll filter your saved albums.
        </p>
        {status && !status.modelEnabled ? (
          <p
            className="mt-3 rounded-lg border px-3 py-2 text-xs"
            style={{ borderColor: "var(--axis)", color: "var(--text-secondary)" }}
          >
            No language-model key is configured, so questions are handled by a keyword parser.
            It understands decades, genres, ratings and album length.
          </p>
        ) : null}
      </header>

      <form
        onSubmit={(event) => {
          event.preventDefault();
          ask(question);
        }}
        className="flex flex-col gap-2 sm:flex-row"
      >
        <Input
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          placeholder="e.g. my favourite alternative albums from the 90s"
          aria-label="Ask a question about your library"
          maxLength={300}
        />
        <Button type="submit" disabled={loading || !question.trim()}>
          {loading ? "Thinking…" : "Ask"}
        </Button>
      </form>

      <div className="flex flex-wrap gap-2">
        {EXAMPLES.map((example) => (
          <button
            key={example}
            type="button"
            onClick={() => ask(example)}
            disabled={loading}
            className="rounded-full border px-3 py-1.5 text-xs transition-opacity hover:opacity-75 disabled:opacity-50"
            style={{ borderColor: "var(--axis)", color: "var(--text-secondary)" }}
          >
            {example}
          </button>
        ))}
      </div>

      {error ? <ErrorState message={error} onRetry={() => ask(question)} /> : null}

      {loading ? (
        <AlbumGridSkeleton count={4} />
      ) : answer ? (
        <div className="space-y-4">
          {/* Showing the interpretation is what makes a misread visible instead of silent. */}
          <div
            className="rounded-xl border p-4"
            style={{ background: "var(--surface-card)", borderColor: "var(--border)" }}
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wide" style={{ color: "var(--text-muted)" }}>
                  How I read that
                </p>
                <p className="mt-1 text-sm">{answer.interpretation}</p>
              </div>
              <span
                className="shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium"
                style={{ background: "var(--accent-wash)", color: "var(--accent-strong)" }}
              >
                {answer.source === "model" ? "model" : "keyword"}
              </span>
            </div>
            <FilterChips filter={answer.filter} />
          </div>

          {answer.results.length === 0 ? (
            <EmptyState
              title="No albums matched"
              body="Nothing in your library fits that description. Try loosening the question, or save more albums."
            />
          ) : (
            <>
              <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
                {answer.count} album{answer.count === 1 ? "" : "s"}
              </p>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {answer.results.map((item) => (
                  <article key={item.id} className="flex flex-col">
                    <AlbumArt src={item.artworkUrl} alt={`${item.title} by ${item.artistName}`} />
                    <h2 className="mt-2.5 line-clamp-2 text-sm font-medium leading-snug">{item.title}</h2>
                    <p className="mt-0.5 line-clamp-1 text-sm" style={{ color: "var(--text-secondary)" }}>
                      {item.artistName}
                    </p>
                    <p className="mt-0.5 text-xs" style={{ color: "var(--text-muted)" }}>
                      {releaseYear(item.releaseDate)}
                      {item.genre ? ` · ${item.genre}` : ""}
                    </p>
                    {item.userRating ? (
                      <div className="mt-1.5">
                        <StarRating value={item.userRating} size={14} />
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>
            </>
          )}
        </div>
      ) : (
        <EmptyState
          title="Ask something"
          body="Pick one of the examples above, or describe the albums you're looking for."
        />
      )}
    </div>
  );
}

/** Shows the exact filter that ran, so the answer is auditable rather than magic. */
function FilterChips({ filter }: { filter: LibraryFilter }) {
  const chips: string[] = [];

  if (filter.genres?.length) chips.push(filter.genres.join(" or "));
  if (filter.artistContains) chips.push(`artist ~ ${filter.artistContains}`);
  if (filter.titleContains) chips.push(`title ~ ${filter.titleContains}`);
  if (filter.yearFrom && filter.yearTo) chips.push(`${filter.yearFrom}–${filter.yearTo}`);
  else if (filter.yearFrom) chips.push(`from ${filter.yearFrom}`);
  else if (filter.yearTo) chips.push(`up to ${filter.yearTo}`);
  if (filter.minRating) chips.push(`${filter.minRating}+ stars`);
  if (filter.minTracks) chips.push(`${filter.minTracks}+ tracks`);
  if (filter.maxTracks) chips.push(`≤ ${filter.maxTracks} tracks`);

  if (chips.length === 0) return null;

  return (
    <div className="mt-3 flex flex-wrap gap-1.5">
      {chips.map((chip) => (
        <span
          key={chip}
          className="rounded px-1.5 py-0.5 font-mono text-[11px]"
          style={{ background: "color-mix(in srgb, var(--text-muted) 14%, transparent)", color: "var(--text-secondary)" }}
        >
          {chip}
        </span>
      ))}
    </div>
  );
}
