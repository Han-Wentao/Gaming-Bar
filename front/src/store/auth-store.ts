import { useSyncExternalStore } from "react";
import { getMe } from "../api/modules/auth";
import type { User } from "../types/models";

export type AuthStatus = "unknown" | "authenticated" | "guest";

type AuthState = {
  status: AuthStatus;
  token: string | null;
  user: User | null;
};

const STORAGE_KEY = "gamingbar_auth";
let state: AuthState = loadInitialState();
const listeners = new Set<() => void>();
let bootstrapPromise: Promise<void> | null = null;

function loadInitialState(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return { status: "guest", token: null, user: null };
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AuthState>;
    if (typeof parsed.token !== "string" || !parsed.token.trim()) {
      return { status: "guest", token: null, user: null };
    }
    return {
      status: "unknown",
      token: parsed.token,
      user: isUserLike(parsed.user) ? parsed.user : null
    };
  } catch {
    return { status: "guest", token: null, user: null };
  }
}

function isUserLike(value: unknown): value is User {
  if (!value || typeof value !== "object") {
    return false;
  }

  const candidate = value as Partial<User>;
  return typeof candidate.id === "number" && typeof candidate.phone === "string";
}

function emit() {
  listeners.forEach((listener) => listener());
}

function persist(nextState: AuthState) {
  state = nextState;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
  emit();
}

export function getAuthState() {
  return state;
}

export function setAuthState(nextState: Omit<AuthState, "status">) {
  persist({ ...nextState, status: "authenticated" });
}

export function setAuthPending(token: string, user: User | null = null) {
  persist({ status: "unknown", token, user });
}

export function setGuestState() {
  localStorage.removeItem(STORAGE_KEY);
  state = { status: "guest", token: null, user: null };
  emit();
}

export function setAuthenticatedState(token: string, user: User) {
  persist({ status: "authenticated", token, user });
}

export function bootstrapAuth() {
  if (state.status !== "unknown" || !state.token) {
    return Promise.resolve();
  }

  if (!bootstrapPromise) {
    bootstrapPromise = getMe()
      .then((user) => {
        if (!state.token) {
          setGuestState();
          return;
        }
        setAuthenticatedState(state.token, user);
      })
      .catch(() => {
        setGuestState();
      })
      .finally(() => {
        bootstrapPromise = null;
      });
  }

  return bootstrapPromise;
}

export function clearAuthState() {
  setGuestState();
}

export function useAuthState() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => state,
    () => state
  );
}
