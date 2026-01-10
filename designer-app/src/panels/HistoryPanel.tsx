import React from "react";
import { useDesignerStore } from "../core/store";

export function HistoryPanel() {
  const { undoStack, redoStack, jumpToHistory } = useDesignerStore();
  
  // Combine stacks for display
  // undoStack is [oldest, ..., newest]
  // redoStack is [newest_undone, ..., oldest_undone] (stack behavior)
  // So chronological history is: ...undoStack, current, ...redoStack.reverse()
  
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
    <div className="ui-panel h-48 flex flex-col shrink-0">
      <div className="ui-panel-title">History</div>
      <div ref={scrollRef} className="flex-1 overflow-y-auto custom-scrollbar p-1 space-y-1">
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
    </div>
  );
}
