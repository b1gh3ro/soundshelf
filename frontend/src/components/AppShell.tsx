"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { Button } from "./ui/Controls";

const NAV = [
  { href: "/search", label: "Search" },
  { href: "/library", label: "Library" },
  { href: "/analytics", label: "Analytics" },
  { href: "/insights", label: "Ask" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
          Loading your shelf…
        </p>
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 border-b backdrop-blur" style={{ borderColor: "var(--border)", background: "color-mix(in srgb, var(--surface-page) 85%, transparent)" }}>
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-4 py-3">
          <Link href="/library" className="mr-1 shrink-0 text-base font-semibold tracking-tight">
            Soundshelf
          </Link>

          {/* min-w-0 lets the flex child shrink below its content width, which is
              what makes the horizontal scroll actually engage on narrow screens. */}
          <nav
            className="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            aria-label="Main"
          >
            {NAV.map((item) => {
              const active = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className="rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap transition-colors"
                  style={{
                    background: active ? "var(--accent-wash)" : "transparent",
                    color: active ? "var(--accent-strong)" : "var(--text-secondary)",
                  }}
                >
                  {item.label}
                </Link>
              );
            })}

            {/* Below sm the header button is hidden, so sign-out rides along at the
                end of the scrollable nav rather than being unreachable. */}
            <button
              type="button"
              onClick={logout}
              className="rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap sm:hidden"
              style={{ color: "var(--text-secondary)" }}
            >
              Sign out
            </button>
          </nav>

          <ThemeToggle />
          {/* Wrapped rather than putting `hidden` on the Button: the button's own
              `inline-flex` base class wins the display cascade otherwise. */}
          <span className="hidden shrink-0 sm:block">
            <Button variant="ghost" onClick={logout}>
              Sign out
            </Button>
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6 sm:py-8">{children}</main>
    </div>
  );
}

function ThemeToggle() {
  const [theme, setTheme] = useState<"light" | "dark" | null>(null);

  useEffect(() => {
    const stored = window.localStorage.getItem("soundshelf.theme");
    if (stored === "light" || stored === "dark") {
      setTheme(stored);
    } else {
      setTheme(window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    }
  }, []);

  function toggle() {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.setAttribute("data-theme", next);
    window.localStorage.setItem("soundshelf.theme", next);
  }

  if (!theme) return <span className="h-8 w-8" />;

  return (
    <button
      type="button"
      onClick={toggle}
      className="rounded-md p-2 transition-colors"
      style={{ color: "var(--text-secondary)" }}
      aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
    >
      {theme === "dark" ? (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" strokeLinecap="round" />
        </svg>
      ) : (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <path d="M21 12.8A9 9 0 1111.2 3a7 7 0 009.8 9.8z" strokeLinejoin="round" />
        </svg>
      )}
    </button>
  );
}
