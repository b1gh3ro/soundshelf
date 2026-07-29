"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { ApiError } from "@/lib/api";
import { Button, Field, Input } from "./ui/Controls";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const isRegister = mode === "register";
  const { user, loading: authLoading, login, register } = useAuth();
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!authLoading && user) router.replace("/library");
  }, [authLoading, user, router]);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});

    try {
      if (isRegister) {
        await register(email, password, displayName);
      } else {
        await login(email, password);
      }
      router.push("/library");
    } catch (cause) {
      if (cause instanceof ApiError) {
        setError(cause.message);
        setFieldErrors(cause.fieldErrors ?? {});
      } else {
        setError("Something went wrong. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  function fillDemo() {
    setEmail("demo@soundshelf.app");
    setPassword("demo1234");
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <h1 className="text-2xl font-semibold tracking-tight">Soundshelf</h1>
        <p className="mt-1.5 text-sm" style={{ color: "var(--text-secondary)" }}>
          {isRegister
            ? "Create an account to start building your library."
            : "Sign in to your library."}
        </p>

        <form onSubmit={onSubmit} className="mt-7 space-y-4" noValidate>
          {isRegister ? (
            <Field label="Display name (optional)" error={fieldErrors.displayName}>
              <Input
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                autoComplete="name"
                placeholder="Alex"
                invalid={Boolean(fieldErrors.displayName)}
              />
            </Field>
          ) : null}

          <Field label="Email" error={fieldErrors.email}>
            <Input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
              placeholder="you@example.com"
              invalid={Boolean(fieldErrors.email)}
            />
          </Field>

          <Field label="Password" error={fieldErrors.password}>
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete={isRegister ? "new-password" : "current-password"}
              required
              placeholder={isRegister ? "At least 8 characters" : "••••••••"}
              invalid={Boolean(fieldErrors.password)}
            />
          </Field>

          {error ? (
            <p className="text-sm" style={{ color: "var(--danger)" }} role="alert">
              {error}
            </p>
          ) : null}

          <Button type="submit" disabled={submitting} className="w-full">
            {submitting ? "Just a moment…" : isRegister ? "Create account" : "Sign in"}
          </Button>
        </form>

        {!isRegister ? (
          <button
            type="button"
            onClick={fillDemo}
            className="mt-3 w-full rounded-md border border-dashed px-3 py-2 text-xs transition-opacity hover:opacity-75"
            style={{ borderColor: "var(--axis)", color: "var(--text-secondary)" }}
          >
            Use the demo account (80 albums already saved)
          </button>
        ) : null}

        <p className="mt-6 text-center text-sm" style={{ color: "var(--text-secondary)" }}>
          {isRegister ? "Already have an account? " : "No account yet? "}
          <Link
            href={isRegister ? "/login" : "/register"}
            className="font-medium underline underline-offset-4"
            style={{ color: "var(--accent)" }}
          >
            {isRegister ? "Sign in" : "Create one"}
          </Link>
        </p>
      </div>
    </div>
  );
}
