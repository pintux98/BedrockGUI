import React, { useState, useEffect } from "react";

export function MobileWarning() {
  const [isMobile, setIsMobile] = useState(false);
  const [ignored, setIgnored] = useState(false);

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

  if (!isMobile || ignored) return null;

  return (
    <div className="fixed inset-0 z-[9999] bg-brand-bg/95 backdrop-blur-sm flex flex-col items-center justify-center p-6 text-center">
      <div className="bg-brand-surface border border-brand-border p-8 rounded-lg shadow-2xl max-w-md w-full">
        <div className="text-4xl mb-4">🖥️</div>
        <h2 className="text-xl font-bold text-white mb-3">Desktop Recommended</h2>
        <p className="text-brand-muted mb-6 text-sm leading-relaxed">
          The BedrockGUI Designer is optimized for large screens. 
          For the best experience, please use a desktop or laptop computer.
        </p>
        <button 
          onClick={() => setIgnored(true)}
          className="ui-btn ui-btn-primary w-full py-3 mb-3"
        >
          Continue Anyway
        </button>
        <div className="text-xs text-brand-muted opacity-50">
          Some features may not work correctly on touch devices.
        </div>
      </div>
    </div>
  );
}
