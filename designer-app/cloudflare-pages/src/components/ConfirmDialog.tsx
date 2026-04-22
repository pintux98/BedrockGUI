import React from "react";
import { useConfirmStore, resolveConfirm } from "../core/confirm";
import { Dialog } from "./Dialog";

export function ConfirmDialog() {
  const { open, title, message, confirmText, cancelText } = useConfirmStore();
  const titleId = React.useId();
  const descriptionId = React.useId();
  return (
    <Dialog
      open={open}
      onClose={() => resolveConfirm(false)}
      labelledBy={titleId}
      describedBy={descriptionId}
      overlayClassName="bg-black/80 backdrop-blur-sm p-6"
      className="ui-panel w-[420px] max-w-[calc(100vw-2rem)]"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div id={titleId} className="ui-panel-title mb-2">
            {title}
          </div>
          <div id={descriptionId} className="text-sm text-brand-muted break-words">
            {message}
          </div>
        </div>
        <button type="button" className="ui-btn ui-btn-ghost px-2 py-1" onClick={() => resolveConfirm(false)} aria-label="Close dialog">
          ✕
        </button>
      </div>
      <div className="mt-4 flex justify-end gap-2">
        <button type="button" className="ui-btn ui-btn-secondary" onClick={() => resolveConfirm(false)}>
          {cancelText}
        </button>
        <button type="button" className="ui-btn ui-btn-primary" onClick={() => resolveConfirm(true)}>
          {confirmText}
        </button>
      </div>
    </Dialog>
  );
}

