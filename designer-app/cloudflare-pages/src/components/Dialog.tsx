import React from "react";
import { createPortal } from "react-dom";

function getFocusable(root: HTMLElement) {
  const selectors = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    "[tabindex]:not([tabindex='-1'])"
  ];
  return Array.from(root.querySelectorAll<HTMLElement>(selectors.join(","))).filter(
    (el) => !el.hasAttribute("disabled") && el.tabIndex !== -1 && !el.getAttribute("aria-hidden")
  );
}

export function Dialog({
  open,
  onClose,
  children,
  closeOnBackdrop = false,
  labelledBy,
  describedBy,
  className = "",
  overlayClassName = ""
}: {
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
  closeOnBackdrop?: boolean;
  labelledBy?: string;
  describedBy?: string;
  className?: string;
  overlayClassName?: string;
}) {
  const dialogRef = React.useRef<HTMLDivElement>(null);
  const lastFocusRef = React.useRef<HTMLElement | null>(null);

  React.useEffect(() => {
    if (!open) return;
    lastFocusRef.current = (document.activeElement as HTMLElement | null) ?? null;
    const t = window.setTimeout(() => {
      const el = dialogRef.current;
      if (!el) return;
      const focusables = getFocusable(el);
      (focusables[0] ?? el).focus();
    }, 0);
    return () => window.clearTimeout(t);
  }, [open]);

  React.useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }
      if (e.key !== "Tab") return;
      const el = dialogRef.current;
      if (!el) return;
      const focusables = getFocusable(el);
      if (focusables.length === 0) {
        e.preventDefault();
        el.focus();
        return;
      }
      const active = document.activeElement as HTMLElement | null;
      const i = focusables.indexOf(active ?? focusables[0]);
      const next = e.shiftKey ? (i <= 0 ? focusables[focusables.length - 1] : focusables[i - 1]) : (i === focusables.length - 1 ? focusables[0] : focusables[i + 1]);
      e.preventDefault();
      next.focus();
    };
    document.addEventListener("keydown", onKeyDown, true);
    return () => document.removeEventListener("keydown", onKeyDown, true);
  }, [open, onClose]);

  React.useEffect(() => {
    if (open) return;
    const prev = lastFocusRef.current;
    if (prev && document.contains(prev)) prev.focus();
  }, [open]);

  if (!open) return null;

  return createPortal(
    <div
      className={`fixed inset-0 z-[9999] flex items-center justify-center ${overlayClassName}`}
      onMouseDown={(e) => {
        if (!closeOnBackdrop) return;
        if (e.target !== e.currentTarget) return;
        onClose();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        aria-describedby={describedBy}
        tabIndex={-1}
        className={className}
      >
        {children}
      </div>
    </div>,
    document.body
  );
}

