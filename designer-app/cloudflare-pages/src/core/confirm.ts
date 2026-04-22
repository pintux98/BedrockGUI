import { create } from "zustand";

type ConfirmState = {
  open: boolean;
  title: string;
  message: string;
  confirmText: string;
  cancelText: string;
};

let resolveRef: ((ok: boolean) => void) | null = null;

export const useConfirmStore = create<ConfirmState & { show: (s: Omit<ConfirmState, "open">) => void; hide: () => void }>(() => ({
  open: false,
  title: "Confirm",
  message: "",
  confirmText: "Confirm",
  cancelText: "Cancel",
  show: (s) => useConfirmStore.setState({ ...s, open: true }),
  hide: () => useConfirmStore.setState({ open: false })
}));

export function confirmDialog(options: {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
}) {
  const title = options.title ?? "Confirm";
  const confirmText = options.confirmText ?? "Confirm";
  const cancelText = options.cancelText ?? "Cancel";
  const message = options.message;
  useConfirmStore.getState().show({ title, message, confirmText, cancelText });
  return new Promise<boolean>((resolve) => {
    resolveRef = resolve;
  });
}

export function resolveConfirm(ok: boolean) {
  useConfirmStore.getState().hide();
  const r = resolveRef;
  resolveRef = null;
  r?.(ok);
}

