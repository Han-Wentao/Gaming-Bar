import { useSyncExternalStore } from "react";
import type { User } from "../types/models";

type AuthState = {
  token: string | null;
  user: User | null;
};

const STORAGE_KEY = "gamingbar_auth";
let state: AuthState = loadInitialState();
const listeners = new Set<() => void>();

function loadInitialState(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return { token: null, user: null };
  }

  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    return { token: null, user: null };
  }
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

export function setAuthState(nextState: AuthState) {
  persist(nextState);
}

export function clearAuthState() {
  localStorage.removeItem(STORAGE_KEY);
  state = { token: null, user: null };
  emit();
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
