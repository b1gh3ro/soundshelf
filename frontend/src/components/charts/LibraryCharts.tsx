"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { CumulativePoint, Slice } from "@/lib/types";
import { AXIS_STROKE, AXIS_TICK, ChartCard, ChartTooltip, GRID_STROKE, SERIES } from "./ChartCard";

const HEIGHT = 260;

/**
 * Single-series charts use one hue and no legend — the title names the series,
 * so a legend box would only repeat it. Only the donut is categorical.
 */
const SINGLE = "var(--accent)";

export function GenreBarChart({ data }: { data: Slice[] }) {
  const top = data.slice(0, 8);

  return (
    <ChartCard title="Albums by genre" caption="Top 8 genres" rows={top} empty={top.length === 0}>
      <ResponsiveContainer width="100%" height={HEIGHT}>
        <BarChart data={top} margin={{ top: 8, right: 8, left: -18, bottom: 4 }}>
          <CartesianGrid stroke={GRID_STROKE} vertical={false} />
          <XAxis
            dataKey="label"
            tick={AXIS_TICK}
            stroke={AXIS_STROKE}
            tickLine={false}
            interval={0}
            angle={-30}
            textAnchor="end"
            height={64}
          />
          <YAxis tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} allowDecimals={false} width={40} />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: "color-mix(in srgb, var(--text-muted) 12%, transparent)" }} />
          {/* Rounded data-end only, anchored to the baseline. */}
          <Bar dataKey="total" fill={SINGLE} radius={[4, 4, 0, 0]} maxBarSize={38} />
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function DecadeDonutChart({ data }: { data: Slice[] }) {
  return (
    <ChartCard title="Share by decade" caption="Of albums with a known release date" rows={data} empty={data.length === 0}>
      <ResponsiveContainer width="100%" height={HEIGHT}>
        {/* Generous side margins: the direct labels sit outside the arc and clip
            against the card edge on narrow screens without them. */}
        <PieChart margin={{ top: 12, right: 56, left: 56, bottom: 12 }}>
          <Pie
            data={data}
            dataKey="total"
            nameKey="label"
            innerRadius="50%"
            outerRadius="72%"
            // 2px surface gap between adjacent fills.
            paddingAngle={1.5}
            stroke="var(--surface-card)"
            strokeWidth={2}
            // Direct labels are the relief for the light-mode contrast warning
            // on three of these hues — identity never rests on colour alone.
            label={({ name, percent }: { name?: string; percent?: number }) =>
              percent && percent >= 0.06 ? `${name} ${Math.round(percent * 100)}%` : ""
            }
            labelLine={false}
          >
            {data.map((row, index) => (
              <Cell key={row.label} fill={SERIES[index % SERIES.length]} />
            ))}
          </Pie>
          <Tooltip content={<ChartTooltip />} />
        </PieChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function ReleasesByYearChart({ data }: { data: Slice[] }) {
  return (
    <ChartCard
      title="Releases by year"
      caption="When the music you saved came out"
      rows={data}
      empty={data.length === 0}
    >
      <ResponsiveContainer width="100%" height={HEIGHT}>
        <LineChart data={data} margin={{ top: 8, right: 12, left: -18, bottom: 4 }}>
          <CartesianGrid stroke={GRID_STROKE} vertical={false} />
          <XAxis
            dataKey="label"
            tick={AXIS_TICK}
            stroke={AXIS_STROKE}
            tickLine={false}
            minTickGap={24}
          />
          <YAxis tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} allowDecimals={false} width={40} />
          <Tooltip content={<ChartTooltip />} cursor={{ stroke: AXIS_STROKE, strokeWidth: 1 }} />
          <Line
            type="monotone"
            dataKey="total"
            stroke={SINGLE}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4, strokeWidth: 2, stroke: "var(--surface-card)" }}
          />
        </LineChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function TrackCountHistogram({ data }: { data: Slice[] }) {
  return (
    <ChartCard
      title="Album length"
      caption="How many tracks your albums have"
      rows={data}
      empty={data.length === 0}
    >
      <ResponsiveContainer width="100%" height={HEIGHT}>
        <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 4 }}>
          <CartesianGrid stroke={GRID_STROKE} vertical={false} />
          <XAxis dataKey="label" tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} />
          <YAxis tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} allowDecimals={false} width={40} />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: "color-mix(in srgb, var(--text-muted) 12%, transparent)" }} />
          {/* A histogram's bars are contiguous buckets, so they sit tighter than a category bar chart. */}
          <Bar dataKey="total" fill={SINGLE} radius={[4, 4, 0, 0]} maxBarSize={56} />
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function TopArtistsChart({ data }: { data: Slice[] }) {
  return (
    <ChartCard
      title="Most-saved artists"
      caption="Top 10 by album count"
      rows={data}
      empty={data.length === 0}
    >
      <ResponsiveContainer width="100%" height={Math.max(HEIGHT, data.length * 30)}>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 20, left: 4, bottom: 4 }}>
          <CartesianGrid stroke={GRID_STROKE} horizontal={false} />
          <XAxis type="number" tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} allowDecimals={false} />
          <YAxis
            type="category"
            dataKey="label"
            tick={AXIS_TICK}
            stroke={AXIS_STROKE}
            tickLine={false}
            width={110}
            interval={0}
          />
          <Tooltip content={<ChartTooltip />} cursor={{ fill: "color-mix(in srgb, var(--text-muted) 12%, transparent)" }} />
          <Bar dataKey="total" fill={SINGLE} radius={[0, 4, 4, 0]} maxBarSize={18} />
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

export function LibraryGrowthChart({ data }: { data: CumulativePoint[] }) {
  const rows = data.map((point) => ({ label: point.label, total: point.cumulative }));

  return (
    <ChartCard
      title="Library growth"
      caption="Running total of albums saved"
      rows={rows}
      valueLabel="Total albums"
      empty={data.length === 0}
    >
      <ResponsiveContainer width="100%" height={HEIGHT}>
        <LineChart data={data} margin={{ top: 8, right: 12, left: -18, bottom: 4 }}>
          <CartesianGrid stroke={GRID_STROKE} vertical={false} />
          <XAxis dataKey="label" tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} minTickGap={32} />
          <YAxis tick={AXIS_TICK} stroke={AXIS_STROKE} tickLine={false} allowDecimals={false} width={40} />
          <Tooltip content={<ChartTooltip unit="album" />} cursor={{ stroke: AXIS_STROKE, strokeWidth: 1 }} />
          <Line
            type="monotone"
            dataKey="cumulative"
            stroke={SINGLE}
            strokeWidth={2}
            // A single-day library shows one point; without a visible dot it renders blank.
            dot={data.length === 1 ? { r: 4 } : false}
            activeDot={{ r: 4, strokeWidth: 2, stroke: "var(--surface-card)" }}
          />
        </LineChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}
