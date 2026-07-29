"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";

export default function HomePage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    router.replace(user ? "/library" : "/login");
  }, [user, loading, router]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-sm" style={{ color: "var(--text-secondary)" }}>
        Loading…
      </p>
    </div>
  );
}
