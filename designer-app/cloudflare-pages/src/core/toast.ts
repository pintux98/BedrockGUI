import { create } from "zustand";

export type ToastVariant = "success" | "error" | "info";

export type ToastItem = {
  id: string;
  message: string;
  variant: ToastVariant;
};

function uid() {
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}

export const useToastStore = create<{
  toasts: ToastItem[];
  push: (t: Omit<ToastItem, "id">, ttlMs?: number) => void;
  dismiss: (id: string) => void;
}>(() => ({
  toasts: [],
  push: (t, ttlMs = 2500) => {
    const id = uid();
    useToastStore.setState((s) => ({ toasts: [...s.toasts, { ...t, id }] }));
    window.setTimeout(() => {
      useToastStore.getState().dismiss(id);
    }, ttlMs);
  },
  dismiss: (id) => useToastStore.setState((s) => ({ toasts: s.toasts.filter((x) => x.id !== id) }))
}));

export const toast = {
  success(message: string, ttlMs?: number) {
    useToastStore.getState().push({ message, variant: "success" }, ttlMs);
  },
  error(message: string, ttlMs?: number) {
    useToastStore.getState().push({ message, variant: "error" }, ttlMs);
  },
  info(message: string, ttlMs?: number) {
    useToastStore.getState().push({ message, variant: "info" }, ttlMs);
  }
};

