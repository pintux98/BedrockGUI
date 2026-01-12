import React from "react";
import { useDesignerStore } from "../core/store";

interface HistoryPanelProps {
  onCollapseChange?: (collapsed: boolean) => void;
}

export function HistoryPanel({ onCollapseChange }: HistoryPanelProps) {
  const { undoStack, redoStack, jumpToHistory } = useDesignerStore();
  const [collapsed, setCollapsed] = React.useState(false);
  
  // Notify parent of collapse state
  React.useEffect(() => {
    onCollapseChange?.(collapsed);
  }, [collapsed, onCollapseChange]);
  
  // Combine stacks for display
  // ... (rest of logic)
  
  const history = [
    ...undoStack.map((x, i) => ({ ...x, index: i, active: false })),
    { description: "Current State", timestamp: Date.now(), index: undoStack.length, active: true },
    ...redoStack.slice().reverse().map((x, i) => ({ ...x, index: undoStack.length + 1 + i, active: false }))
  ].reverse();
  
  const scrollRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (scrollRef.current) {
        scrollRef.current.scrollTop = 0;
    }
  }, [history.length]);

  return (
    <div className={`flex flex-col h-full border-t border-brand-border overflow-hidden transition-all duration-300 ${collapsed ? "bg-brand-surface" : "bg-brand-bg"}`}>
      <div 
        className="h-8 flex items-center px-4 bg-brand-surface border-b border-brand-border cursor-pointer hover:bg-brand-surface2 transition-colors justify-between shrink-0"
        onClick={() => setCollapsed(!collapsed)}
      >
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-brand-text">History</span>
          {collapsed && <span className="text-[10px] text-brand-muted px-2 py-0.5 rounded bg-brand-bg border border-brand-border">Collapsed</span>}
        </div>
        <span className="text-xs text-brand-muted transition-transform duration-300" style={{ transform: collapsed ? 'rotate(180deg)' : 'rotate(0deg)' }}>▼</span>
      </div>
      {!collapsed && (
        <div ref={scrollRef} className="flex-1 overflow-y-auto custom-scrollbar p-1 space-y-1 animate-in fade-in slide-in-from-bottom-2 duration-300">
          {history.map((entry, i) => (
             <div 
               key={i} 
               className={`p-2 text-xs border cursor-pointer flex items-center justify-between group ${
                 entry.active 
                   ? "bg-brand-accent/20 border-brand-accent text-white" 
                   : "bg-brand-surface border-brand-border text-brand-muted hover:bg-brand-surface2"
               }`}
               onClick={() => !entry.active && entry.index < undoStack.length && jumpToHistory(entry.index)}
             >
               <div className="flex flex-col overflow-hidden">
                 <div className="font-bold truncate">{entry.description}</div>
                 <div className="opacity-50 text-[10px]">
                   {entry.timestamp ? new Date(entry.timestamp).toLocaleTimeString() : "--:--"}
                 </div>
               </div>
               {entry.active && <div className="w-2 h-2 rounded-full bg-brand-accent shadow-[0_0_5px_rgba(0,255,0,0.5)]" />}
               {!entry.active && entry.index < undoStack.length && (
                 <div className="hidden group-hover:block text-[10px] bg-brand-surface2 px-1 border border-brand-border">
                   Revert
                 </div>
               )}
             </div>
          ))}
          {history.length === 1 && (
              <div className="text-xs text-brand-muted text-center py-4">No history yet</div>
          )}
        </div>
      )}
    </div>
  );
}
