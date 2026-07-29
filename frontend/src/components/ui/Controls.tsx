"use client";

import { forwardRef } from "react";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

const VARIANTS: Record<ButtonVariant, React.CSSProperties> = {
  primary: { background: "var(--accent)", color: "#fff", borderColor: "transparent" },
  secondary: { background: "var(--surface-card)", color: "var(--text-primary)", borderColor: "var(--axis)" },
  ghost: { background: "transparent", color: "var(--text-secondary)", borderColor: "transparent" },
  danger: { background: "transparent", color: "var(--danger)", borderColor: "var(--danger)" },
};

export const Button = forwardRef<
  HTMLButtonElement,
  React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }
>(function Button({ variant = "primary", className = "", style, ...props }, ref) {
  return (
    <button
      ref={ref}
      className={`inline-flex items-center justify-center gap-2 rounded-md border px-3.5 py-2 text-sm font-medium transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
      style={{ ...VARIANTS[variant], ...style }}
      {...props}
    />
  );
});

export const Input = forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }
>(function Input({ className = "", invalid, ...props }, ref) {
  return (
    <input
      ref={ref}
      aria-invalid={invalid || undefined}
      className={`w-full rounded-md border px-3 py-2 text-sm outline-none transition-colors focus:border-[var(--accent)] ${className}`}
      style={{
        background: "var(--surface-card)",
        color: "var(--text-primary)",
        borderColor: invalid ? "var(--danger)" : "var(--axis)",
      }}
      {...props}
    />
  );
});

export function Select({
  className = "",
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={`rounded-md border px-3 py-2 text-sm outline-none focus:border-[var(--accent)] ${className}`}
      style={{
        background: "var(--surface-card)",
        color: "var(--text-primary)",
        borderColor: "var(--axis)",
      }}
      {...props}
    />
  );
}

export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium" style={{ color: "var(--text-secondary)" }}>
        {label}
      </span>
      {children}
      {error ? (
        <span className="mt-1 block text-xs" style={{ color: "var(--danger)" }}>
          {error}
        </span>
      ) : null}
    </label>
  );
}

/** Interactive when onChange is supplied, static display otherwise. */
export function StarRating({
  value,
  onChange,
  size = 18,
}: {
  value: number | null;
  onChange?: (rating: number | null) => void;
  size?: number;
}) {
  const stars = [1, 2, 3, 4, 5];

  if (!onChange) {
    return (
      <span className="inline-flex items-center gap-0.5" aria-label={value ? `Rated ${value} of 5` : "Not rated"}>
        {stars.map((star) => (
          <Star key={star} filled={value !== null && star <= value} size={size} />
        ))}
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-0.5">
      {stars.map((star) => (
        <button
          key={star}
          type="button"
          // Clicking the current rating clears it, so a mis-tap is recoverable.
          onClick={() => onChange(value === star ? null : star)}
          className="rounded p-0.5 transition-transform hover:scale-110"
          aria-label={value === star ? `Clear rating` : `Rate ${star} of 5`}
          aria-pressed={value !== null && star <= value}
        >
          <Star filled={value !== null && star <= value} size={size} />
        </button>
      ))}
    </span>
  );
}

function Star({ filled, size }: { filled: boolean; size: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? "var(--series-4)" : "none"}
      stroke={filled ? "var(--series-4)" : "var(--text-muted)"}
      strokeWidth="1.8"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M12 3.5l2.6 5.8 6.1.6-4.6 4.1 1.3 6.1L12 16.9 6.6 20.1l1.3-6.1L3.3 9.9l6.1-.6z" />
    </svg>
  );
}
