import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/lib/auth";

export const metadata: Metadata = {
  title: "Soundshelf",
  description: "Search the iTunes catalog, build an album library, and see what it says about you.",
};

/**
 * Applies the stored theme before first paint. Without this the page renders in
 * the OS theme and then flips, which reads as a bug.
 */
const THEME_SCRIPT = `
try {
  var stored = localStorage.getItem('soundshelf.theme');
  if (stored === 'light' || stored === 'dark') {
    document.documentElement.setAttribute('data-theme', stored);
  }
} catch (e) {}
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_SCRIPT }} />
      </head>
      <body className="min-h-screen antialiased">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
