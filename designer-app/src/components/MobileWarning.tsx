import React, { useState, useEffect } from "react";
import { Dialog } from "./Dialog";

export function MobileWarning() {
  const [isMobile, setIsMobile] = useState(false);
  const [ignored, setIgnored] = useState(false);
  const titleId = React.useId();
  const descriptionId = React.useId();

  useEffect(() => {
    const check = () => {
      // Check for mobile viewport width (standard breakdown: < 768px is usually mobile/tablet portrait)
      if (window.innerWidth < 1024) {
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
      overlayClassName="bg-brand-bg/95 backdrop-blur-sm p-6 text-center"
      className="bg-brand-surface border border-brand-border p-8 rounded-lg shadow-2xl max-w-md w-full"
    >
        <div className="text-4xl mb-4">🖥️</div>
        <h2 id={titleId} className="text-xl font-bold text-white mb-3">Desktop Recommended</h2>
        <p id={descriptionId} className="text-brand-muted mb-6 text-sm leading-relaxed">
          The BedrockGUI Designer is optimized for large screens. 
          For the best experience, please use a desktop or laptop computer.
        </p>
        <button 
          onClick={() => setIgnored(true)}
          className="ui-btn ui-btn-primary w-full py-3 mb-3"
          type="button"
        >
          Continue Anyway
        </button>
        <div className="text-xs text-brand-muted opacity-50">
          Some features may not work correctly on touch devices.
        </div>
    </Dialog>
  );
}
