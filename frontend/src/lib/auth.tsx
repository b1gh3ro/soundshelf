"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api, clearToken, getToken, setToken } from "./api";
import type { AuthResponse, UserSummary } from "./types";

interface AuthState {
  user: UserSummary | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  // On boot we have a token but no user; ask the API who it belongs to.
  useEffect(() => {
    if (!getToken()) {
      setLoading(false);
      return;
    }
    api
      .get<UserSummary>("/api/auth/me")
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setLoading(false));
  }, []);

  const adopt = useCallback((auth: AuthResponse) => {
    setToken(auth.token);
    setUser(auth.user);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const auth = await api.post<AuthResponse>(
        "/api/auth/login",
        { email, password },
        { skipAuthRedirect: true },
      );
      adopt(auth);
    },
    [adopt],
  );

  const register = useCallback(
    async (email: string, password: string, displayName: string) => {
      const auth = await api.post<AuthResponse>(
        "/api/auth/register",
        { email, password, displayName: displayName || null },
        { skipAuthRedirect: true },
      );
      adopt(auth);
    },
    [adopt],
  );

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    router.push("/login");
  }, [router]);

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
