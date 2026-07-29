"use client";

import { useState } from "react";

/**
 * iTunes artwork URLs occasionally 404. A plain <img> with a fallback beats
 * next/image here: the hosts are not known at build time and these are already
 * correctly sized by the API.
 */
export function AlbumArt({ src, alt }: { src: string | null; alt: string }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div
        className="flex aspect-square w-full items-center justify-center rounded-lg"
        style={{ background: "color-mix(in srgb, var(--text-muted) 15%, transparent)" }}
        aria-hidden="true"
      >
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" strokeWidth="1.5">
          <circle cx="12" cy="12" r="9" />
          <circle cx="12" cy="12" r="2.5" />
        </svg>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className="aspect-square w-full rounded-lg object-cover"
      style={{ background: "color-mix(in srgb, var(--text-muted) 12%, transparent)" }}
    />
  );
}
