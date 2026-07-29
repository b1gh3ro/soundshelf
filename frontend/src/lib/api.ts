import type { ApiErrorBody } from "./types";

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";

const TOKEN_KEY = "soundshelf.token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY);
}

/** Carries the parsed error body so callers can show field errors inline. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  signal?: AbortSignal;
  /** Auth calls opt out so a failed login does not wipe an existing session. */
  skipAuthRedirect?: boolean;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, signal, skipAuthRedirect } = options;
  const token = getToken();

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    if (signal?.aborted) throw cause;
    // A free-tier backend that has gone to sleep looks exactly like this.
    throw new ApiError(0, "Could not reach the server. It may be waking up — try again in a moment.");
  }

  if (response.status === 401 && !skipAuthRedirect) {
    clearToken();
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
    throw new ApiError(401, "Your session expired. Please sign in again.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    const error = payload as ApiErrorBody | null;
    throw new ApiError(
      response.status,
      error?.message || error?.error || `Request failed (${response.status})`,
      error?.fieldErrors,
    );
  }

  return payload as T;
}

export const api = {
  get: <T,>(path: string, signal?: AbortSignal) => request<T>(path, { signal }),
  post: <T,>(path: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(path, { ...opts, method: "POST", body }),
  put: <T,>(path: string, body?: unknown) => request<T>(path, { method: "PUT", body }),
  delete: <T,>(path: string) => request<T>(path, { method: "DELETE" }),
};
