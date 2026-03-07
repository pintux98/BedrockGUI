import React from "react";
import { useToastStore } from "../core/toast";

function variantClasses(v: "success" | "error" | "info") {
  if (v === "success") return "border-brand-success/70 bg-brand-success/10 text-brand-text";
  if (v === "error") return "border-brand-danger/70 bg-brand-danger/10 text-brand-text";
  return "border-brand-border bg-brand-surface text-brand-text";
}

export function ToastHost() {
  const { toasts, dismiss } = useToastStore();
  if (toasts.length === 0) return null;
  return (
    <div className="fixed bottom-4 right-4 z-[9999] flex flex-col gap-2 items-end pointer-events-none">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`pointer-events-auto ui-panel px-3 py-2 text-xs w-[320px] max-w-[calc(100vw-2rem)] ${variantClasses(t.variant)}`}
          role="status"
          aria-live={t.variant === "error" ? "assertive" : "polite"}
        >
          <div className="flex items-start justify-between gap-3">
            <div className="leading-snug break-words">{t.message}</div>
            <button
              type="button"
              className="ui-btn ui-btn-ghost px-2 py-1 text-xs"
              onClick={() => dismiss(t.id)}
              aria-label="Dismiss notification"
            >
              ✕
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

