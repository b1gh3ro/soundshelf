export function releaseYear(date: string | null): string {
  if (!date) return "—";
  return date.slice(0, 4);
}

export function formatDate(date: string | null): string {
  if (!date) return "Unknown";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return "Unknown";
  return parsed.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

export function formatNumber(value: number): string {
  return value.toLocaleString();
}

export function formatPrice(value: number | null): string {
  if (value === null || value === undefined) return "—";
  return `$${value.toFixed(2)}`;
}

export function pluralise(count: number, singular: string, plural = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`;
}

/** Waits for a value to settle before letting callers act on it. */
export function debounce<T extends (...args: never[]) => void>(fn: T, ms: number) {
  let timer: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}
