import React, { useState, useEffect } from "react";
import { Dialog } from "./Dialog";

export function MobileWarning() {
  const [isMobile, setIsMobile] = useState(false);
  const [ignored, setIgnored] = useState(false);
  const titleId = React.useId();
  const descriptionId = React.useId();

  useEffect(() => {
    const check = () => {
      if (window.innerWidth < 640) {
        setIsMobile(true);
      } else {
        setIsMobile(false);
      }
    };
    check();
    window.addEventListener("resize", check);
    return () => window.removeEventListener("resize", check);
  }, []);

  const open = isMobile && !ignored;
  if (!open) return null;

  return (
    <Dialog
      open={true}
      onClose={() => setIgnored(true)}
      labelledBy={titleId}
      describedBy={descriptionId}
      overlayClassName="bg-brand-bg/92 p-4 text-center"
      className="bg-brand-surface border border-brand-border p-6 rounded-lg shadow-lg max-w-md w-full"
    >
        <div className="text-3xl mb-3">📱</div>
        <h2 id={titleId} className="text-xl font-semibold text-white mb-2">Compact View Enabled</h2>
        <p id={descriptionId} className="text-brand-muted mb-6 text-sm leading-relaxed">
          This screen size uses a simplified layout with tabbed navigation.
          Rotate your device or expand the window for a wider workspace.
        </p>
        <button 
          onClick={() => setIgnored(true)}
          className="ui-btn ui-btn-primary w-full py-3 mb-3"
          type="button"
        >
          Continue
        </button>
        <div className="text-xs text-brand-muted opacity-50">
          Full multi-panel editing appears automatically on larger widths.
        </div>
    </Dialog>
  );
}
