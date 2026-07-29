"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { AnalyticsSummary } from "@/lib/types";
import { Button } from "@/components/ui/Controls";
import { EmptyState, ErrorState, Skeleton } from "@/components/ui/States";
import {
  DecadeDonutChart,
  GenreBarChart,
  LibraryGrowthChart,
  ReleasesByYearChart,
  TopArtistsChart,
  TrackCountHistogram,
} from "@/components/charts/LibraryCharts";

export default function AnalyticsPage() {
  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await api.get<AnalyticsSummary>("/api/analytics/summary"));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : "Could not load your analytics.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <div className="space-y-6" role="status" aria-label="Loading analytics">
        <Skeleton className="h-7 w-40" />
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-24" />
          ))}
        </div>
        <div className="grid gap-4 lg:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-80" />
          ))}
        </div>
      </div>
    );
  }

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!data) return null;

  if (data.totals.albums === 0) {
    return (
      <div className="space-y-6">
        <h1 className="text-xl font-semibold tracking-tight">Analytics</h1>
        <EmptyState
          title="Nothing to chart yet"
          body="Save some albums and this page fills in with your genres, decades, album lengths and most-saved artists."
          action={
            <Link href="/search">
              <Button>Find albums</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const { totals } = data;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-semibold tracking-tight">Analytics</h1>
        <p className="mt-1 text-sm" style={{ color: "var(--text-secondary)" }}>
          Everything on this page is computed from your saved library.
        </p>
      </header>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile label="Albums" value={formatNumber(totals.albums)} />
        <StatTile label="Artists" value={formatNumber(totals.artists)} hint={`across ${totals.genres} genres`} />
        <StatTile label="Tracks" value={formatNumber(totals.tracks)} hint={`${totals.avgTrackCount} per album`} />
        <StatTile
          label="Average rating"
          value={totals.avgRating > 0 ? totals.avgRating.toFixed(2) : "—"}
          hint={totals.avgRating > 0 ? "of the ones you rated" : "rate some albums"}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <GenreBarChart data={data.byGenre} />
        <DecadeDonutChart data={data.byDecade} />
        <ReleasesByYearChart data={data.releasesByYear} />
        <TrackCountHistogram data={data.trackCountBuckets} />
        <TopArtistsChart data={data.topArtists} />
        <LibraryGrowthChart data={data.addedOverTime} />
      </div>
    </div>
  );
}

function StatTile({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div
      className="rounded-xl border p-4"
      style={{ background: "var(--surface-card)", borderColor: "var(--border)" }}
    >
      <p className="text-xs font-medium uppercase tracking-wide" style={{ color: "var(--text-muted)" }}>
        {label}
      </p>
      <p className="mt-1.5 text-2xl font-semibold tabular-nums leading-none">{value}</p>
      {hint ? (
        <p className="mt-1.5 text-xs" style={{ color: "var(--text-secondary)" }}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}
