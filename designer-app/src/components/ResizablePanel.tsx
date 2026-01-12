import React, { useState, useEffect, useCallback } from "react";

interface ResizablePanelProps {
  initialSize: number;
  minSize?: number;
  maxSize?: number;
  children: React.ReactNode;
  side: "left" | "right" | "top" | "bottom";
  persistenceKey?: string;
  className?: string;
  forceCollapse?: boolean;
}

export function ResizablePanel({
  initialSize,
  minSize = 200,
  maxSize = 600,
  children,
  side,
  persistenceKey,
  className = "",
  forceCollapse = false
}: ResizablePanelProps) {
  const [size, setSize] = useState(() => {
    if (persistenceKey) {
      const saved = localStorage.getItem(persistenceKey);
      if (saved) return Number(saved);
    }
    return initialSize;
  });
  const [isResizing, setIsResizing] = useState(false);

  const startResizing = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    setIsResizing(true);
    e.preventDefault();
  }, []);

  const stopResizing = useCallback(() => {
    setIsResizing(false);
    if (persistenceKey) {
      localStorage.setItem(persistenceKey, String(size));
    }
  }, [persistenceKey, size]);

  const resize = useCallback(
    (e: MouseEvent | TouchEvent) => {
      if (isResizing) {
        let clientX = 0;
        let clientY = 0;
        if (e instanceof MouseEvent) {
          clientX = e.clientX;
          clientY = e.clientY;
        } else if (e instanceof TouchEvent) {
          clientX = e.touches[0].clientX;
          clientY = e.touches[0].clientY;
        }

        let newSize = size;
        if (side === "left") {
          newSize = clientX;
        } else if (side === "right") {
          newSize = window.innerWidth - clientX;
        } else if (side === "top") {
          newSize = clientY;
        } else if (side === "bottom") {
          // For bottom resizing (like YAML panel), we usually calculate from bottom up
          // But usually this component wraps the bottom element, so size is height
          // We need the bounding rect of the parent to be accurate, but simplified:
          // If we are dragging the top edge of a bottom panel:
          const parentHeight = window.innerHeight; // Approximate for full screen app
          newSize = parentHeight - clientY;
        }

        if (newSize >= minSize && newSize <= maxSize) {
          setSize(newSize);
        }
      }
    },
    [isResizing, minSize, maxSize, side, size]
  );

  useEffect(() => {
    if (isResizing) {
      window.addEventListener("mousemove", resize);
      window.addEventListener("touchmove", resize);
      window.addEventListener("mouseup", stopResizing);
      window.addEventListener("touchend", stopResizing);
    }
    return () => {
      window.removeEventListener("mousemove", resize);
      window.removeEventListener("touchmove", resize);
      window.removeEventListener("mouseup", stopResizing);
      window.removeEventListener("touchend", stopResizing);
    };
  }, [isResizing, resize, stopResizing]);

  const isVertical = side === "top" || side === "bottom";
  
  // Update state when initialSize changes, unless resizing or persisting
  useEffect(() => {
    if (!persistenceKey) {
      setSize(initialSize);
    }
  }, [initialSize, persistenceKey]);

  const handleClass = isVertical
    ? `absolute left-0 right-0 h-1 cursor-row-resize hover:bg-brand-accent active:bg-brand-accent z-50 transition-colors ${
        side === "bottom" ? "top-0 -translate-y-1/2" : "bottom-0 translate-y-1/2"
      } ${isResizing ? "bg-brand-accent" : "bg-transparent"}`
    : `absolute top-0 bottom-0 w-1 cursor-col-resize hover:bg-brand-accent active:bg-brand-accent z-50 transition-colors ${
        side === "left" ? "right-0 translate-x-1/2" : "left-0 -translate-x-1/2"
      } ${isResizing ? "bg-brand-accent" : "bg-transparent"}`;

  return (
    <div 
      className={`relative flex flex-col shrink-0 ${className}`} 
      style={{ 
        width: isVertical ? "100%" : (forceCollapse ? "32px" : `${size}px`),
        height: isVertical ? (forceCollapse ? "32px" : `${size}px`) : "100%"
      }}
    >
      {children}
      {!forceCollapse && (
        <div 
          className={handleClass} 
          onMouseDown={startResizing} 
          onTouchStart={startResizing} 
        />
      )}
    </div>
  );
}
