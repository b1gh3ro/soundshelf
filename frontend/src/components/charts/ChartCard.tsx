"use client";

import { useId, useState } from "react";

export const AXIS_TICK = { fill: "var(--text-muted)", fontSize: 12 } as const;
export const GRID_STROKE = "var(--gridline)";
export const AXIS_STROKE = "var(--axis)";

/** Fixed categorical order. Never cycled — a 9th slice folds into "Other". */
export const SERIES = [
  "var(--series-1)",
  "var(--series-2)",
  "var(--series-3)",
  "var(--series-4)",
  "var(--series-5)",
  "var(--series-6)",
  "var(--series-7)",
  "var(--series-8)",
] as const;

/**
 * Every chart ships with a table view. It's the fallback when colour alone
 * wouldn't carry the meaning — colourblind readers, screen readers, print.
 */
export function ChartCard({
  title,
  caption,
  rows,
  valueLabel = "Albums",
  children,
  empty,
}: {
  title: string;
  caption?: string;
  rows: { label: string; total: number }[];
  valueLabel?: string;
  children: React.ReactNode;
  empty?: boolean;
}) {
  const [showTable, setShowTable] = useState(false);
  const tableId = useId();

  return (
    <section
      className="rounded-xl border p-4 sm:p-5"
      style={{ background: "var(--surface-card)", borderColor: "var(--border)" }}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold tracking-tight">{title}</h2>
          {caption ? (
            <p className="mt-0.5 text-xs" style={{ color: "var(--text-muted)" }}>
              {caption}
            </p>
          ) : null}
        </div>
        {!empty ? (
          <button
            type="button"
            onClick={() => setShowTable((open) => !open)}
            className="shrink-0 rounded px-2 py-1 text-xs transition-opacity hover:opacity-75"
            style={{ color: "var(--text-secondary)" }}
            aria-expanded={showTable}
            aria-controls={tableId}
          >
            {showTable ? "Show chart" : "Show data"}
          </button>
        ) : null}
      </div>

      <div className="mt-4">
        {empty ? (
          <p className="py-10 text-center text-sm" style={{ color: "var(--text-muted)" }}>
            Save a few albums and this fills in.
          </p>
        ) : showTable ? (
          <div id={tableId} className="max-h-72 overflow-auto">
            <table className="w-full text-sm">
              <thead>
                <tr style={{ color: "var(--text-secondary)" }}>
                  <th scope="col" className="border-b py-1.5 pr-3 text-left font-medium" style={{ borderColor: "var(--border)" }}>
                    {title}
                  </th>
                  <th scope="col" className="border-b py-1.5 text-right font-medium" style={{ borderColor: "var(--border)" }}>
                    {valueLabel}
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.label}>
                    <td className="py-1.5 pr-3">{row.label}</td>
                    <td className="py-1.5 text-right tabular-nums">{row.total}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          children
        )}
      </div>
    </section>
  );
}

export function ChartTooltip({
  active,
  payload,
  label,
  unit = "album",
}: {
  active?: boolean;
  payload?: { value?: number; name?: string; payload?: { label?: string } }[];
  label?: string;
  unit?: string;
}) {
  if (!active || !payload?.length) return null;

  const value = payload[0]?.value ?? 0;
  const heading = label ?? payload[0]?.payload?.label ?? "";

  return (
    <div
      className="rounded-lg border px-3 py-2 text-xs shadow-lg"
      style={{ background: "var(--surface-card)", borderColor: "var(--border)", color: "var(--text-primary)" }}
    >
      <p className="font-medium">{heading}</p>
      <p className="mt-0.5 tabular-nums" style={{ color: "var(--text-secondary)" }}>
        {value} {unit}
        {value === 1 ? "" : "s"}
      </p>
    </div>
  );
}
