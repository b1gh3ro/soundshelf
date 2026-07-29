"use client";

export function Skeleton({ className = "" }: { className?: string }) {
  return <div className={`skeleton rounded-md ${className}`} aria-hidden="true" />;
}

export function AlbumGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div
      className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4"
      role="status"
      aria-label="Loading albums"
    >
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="space-y-3">
          <Skeleton className="aspect-square w-full" />
          <Skeleton className="h-4 w-4/5" />
          <Skeleton className="h-3 w-3/5" />
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: React.ReactNode;
}) {
  return (
    <div
      className="flex flex-col items-center rounded-xl border border-dashed px-6 py-14 text-center"
      style={{ borderColor: "var(--axis)" }}
    >
      <p className="text-base font-medium" style={{ color: "var(--text-primary)" }}>
        {title}
      </p>
      <p className="mt-2 max-w-sm text-sm" style={{ color: "var(--text-secondary)" }}>
        {body}
      </p>
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div
      className="rounded-xl border px-5 py-4"
      style={{ borderColor: "var(--danger)", background: "color-mix(in srgb, var(--danger) 8%, transparent)" }}
      role="alert"
    >
      <p className="text-sm font-medium" style={{ color: "var(--text-primary)" }}>
        Something went wrong
      </p>
      <p className="mt-1 text-sm" style={{ color: "var(--text-secondary)" }}>
        {message}
      </p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 rounded-md px-3 py-1.5 text-sm font-medium transition-opacity hover:opacity-85"
          style={{ background: "var(--accent)", color: "#fff" }}
        >
          Try again
        </button>
      ) : null}
    </div>
  );
}

export function Spinner({ label }: { label: string }) {
  return (
    <span className="inline-flex items-center gap-2 text-sm" style={{ color: "var(--text-secondary)" }}>
      <span
        className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
        aria-hidden="true"
      />
      {label}
    </span>
  );
}
